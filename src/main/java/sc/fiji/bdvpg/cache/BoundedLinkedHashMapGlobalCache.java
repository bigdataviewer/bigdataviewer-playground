package sc.fiji.bdvpg.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class BoundedLinkedHashMapGlobalCache extends AbstractGlobalCache {

    final static Logger logger = LoggerFactory.getLogger(BoundedLinkedHashMapGlobalCache.class);

    final SoftRefs cache;

    BoundedLinkedHashMapGlobalCache(int iniSize, long maxCacheSize, boolean log, int msBetweenLogs) {

        cache = new SoftRefs(iniSize, maxCacheSize);

        if (log) {
            TimerTask periodicLogger = new TimerTask() {
                @Override
                public void run() {
                    logger.info(this.toString());
                }
            };

            Timer time = new Timer(); // Instantiate Timer Object
            time.schedule(periodicLogger, 0, msBetweenLogs);
        }

    }

    public void setMaxSize(long maxCacheSize) {
        cache.setMaxCost(maxCacheSize);
    }

    public void put(GlobalCacheKey key, Object value) {
        cache.touch(key, value);
    }

    @Override
    public Object get(GlobalCacheKey key) throws ExecutionException {
        return cache.get(key);
    }

    @Override
    public Object getIfPresent(GlobalCacheKey key) {
        return cache.get(key);
    }

    @Override
    public void invalidate(GlobalCacheKey key) {
        cache.remove(key);
    }

    @Override
    public void invalidateIf(long parallelismThreshold, Predicate<GlobalCacheKey> condition) {
        cache.keySet().removeIf( condition );
    }

    @Override
    public void invalidateAll(long parallelismThreshold) {
        cache.clear();
    }

    public long getMaxSize() {
        return cache.getMaxCost();
    }

    public long getEstimatedSize() {
        return cache.getCost();
    }

    @Override
    public <V> void touch(GlobalCacheKey key, V value) {
        cache.touch(key, value);
    }

    static class SoftRefs extends LinkedHashMap< GlobalCacheKey, SoftReference< Object >>
    {
        private static final long serialVersionUID = 1L;

        private long maxCost;

        AtomicLong totalWeight = new AtomicLong();

        HashMap<GlobalCacheKey, Long> cost = new HashMap<>();

        public SoftRefs( final int iniSize, final long maxCost  )
        {
            super( iniSize, 0.75f, true );
            this.maxCost = maxCost;
        }

        public void setMaxCost(long maxCost) {
            this.maxCost = maxCost;
        }

        public long getCost() {
            return totalWeight.get();
        }

        public long getMaxCost() {
            return maxCost;
        }

        @Override
        protected boolean removeEldestEntry( final Map.Entry< GlobalCacheKey, SoftReference< Object > > eldest )
        {
            if ( totalWeight.get() > maxCost )
            {
                totalWeight.addAndGet(-cost.get(eldest.getKey()));
                cost.remove(eldest.getKey());
                eldest.getValue().clear();
                return true;
            }
            else
                return false;
        }

        synchronized public void touch( final GlobalCacheKey key, final Object value )
        {
            final SoftReference< Object > ref = get( key );
            if ( ref == null ) {
                long costValue = getWeight(value);
                totalWeight.addAndGet(costValue);
                cost.put(key, costValue);
                put(key, new SoftReference<>(value));
            } else if (ref.get() == null ) {
                put(key, new SoftReference<>(value));
            }
        }

        @Override
        public synchronized void clear()
        {
            for ( final SoftReference< Object > ref : values() ) {
                ref.clear();
            }
            totalWeight.set(0);
            cost.clear();
            super.clear();
        }
    }


    @Override
    public String toString() {
        return "Cache size : " + (cache.getCost() / (1024 * 1024)) + " Mb (" + (int) (100.0 * (double) cache.getCost() / (double) cache.getMaxCost()) + " %)";
    }

}
