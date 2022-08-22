package sc.fiji.bdvpg.cache;

import net.imglib2.cache.Cache;
import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.cell.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class GlobalCache implements Cache<GlobalCache.Key, Object > {

    final static Logger logger = LoggerFactory.getLogger(GlobalCache.class);

    final SoftRefs cache;

    private GlobalCache(int iniSize, long maxCacheSize, boolean log, int msBetweenLogs) {

        cache = new SoftRefs(iniSize, maxCacheSize);

        if (log) {
            TimerTask periodicLogger = new TimerTask() {
                @Override
                public void run() {
                    logger.info("Cache size : " + (cache.getCost() / (1024 * 1024)) + " Mb (" + (int) (100.0 * (double) cache.getCost() / (double) cache.getMaxCost()) + " %)");
                }
            };

            Timer time = new Timer(); // Instantiate Timer Object
            time.schedule(periodicLogger, 0, msBetweenLogs);
        }

    }

    public static <K> Predicate<Key> getCondition(Object source, int timepoint, int level, Predicate<K> condition) {
        return (key) -> key.partialEquals(source, timepoint, level) && condition.test((K)key.key);
    }

    public void setMaxSize(long maxCacheSize) {
        cache.setMaxCost(maxCacheSize);
    }

    void touch( final Key key, Object value ) {
        cache.touch(key, value);
    }

    static public Key getKey(Object source, int timepoint, int level, Object key){
        return new Key(source, timepoint, level, key);
    }

    public void put(Key key, Object value) {
        cache.touch(key, value);
    }

    @Override
    public Object get(Key key) throws ExecutionException {
        return cache.get(key);
    }

    @Override
    public Object getIfPresent(Key key) {
        return cache.get(key);
    }

    @Override
    public void persist(Key key) {
    }

    @Override
    public void persistIf(Predicate<Key> condition) {
    }

    @Override
    public void persistAll() {

    }

    @Override
    public void invalidate(Key key) {
        cache.remove(key);
    }

    @Override
    public void invalidateIf(long parallelismThreshold, Predicate<Key> condition) {
        cache.keySet().removeIf( condition );
    }

    @Override
    public void invalidateAll(long parallelismThreshold) {
        cache.clear();
    }

    public long getMaxSize() {
        return cache.getMaxCost();
    }

    static class Key
    {
        private final WeakReference<Object> source;

        private final int timepoint;

        private final int level;

        private final WeakReference<Object> key;

        public Key( final Object source, final int timepoint, final int level, final Object key )
        {
            this.source = new WeakReference<>(source);
            this.timepoint = timepoint;
            this.level = level;
            this.key = new WeakReference<>(key);

            int value = source.hashCode();
            value = 31 * value + level;
            value = 31 * value + key.hashCode();
            value = 31 * value + timepoint;
            hashcode = value;
        }

        public boolean partialEquals(final Object source, final int timepoint, final int level) {
            if (this.source.get()==null) return false;
            if (key.get()==null) return false;

            return ( this.source.get() == source )
                    && ( this.timepoint == timepoint )
                    && ( this.level == level );
        }

        @Override
        public boolean equals( final Object other )
        {
            if (source.get()==null) return false;
            if (key.get()==null) return false;

            if ( this == other )
                return true;
            if ( !( other instanceof GlobalCache.Key ) )
                return false;
            final GlobalCache.Key that = (GlobalCache.Key) other;

            return ( this.source.get() == that.source.get() )
                    && ( this.timepoint == that.timepoint )
                    && ( this.level == that.level )
                    && ( this.key.get().equals(that.key.get()) );
        }

        final int hashcode;

        @Override
        public int hashCode()
        {
            return hashcode;
        }
    }

    static long getWeight(Object object) {
        if (object instanceof Cell) {
            Cell cell = ((Cell) object);
            Object data = cell.getData();

            if (ShortAccess.class.isInstance(data)) {
                return 2*cell.size();
            } else if (ByteAccess.class.isInstance(data)) {
                return cell.size();
            } else if (FloatAccess.class.isInstance(data)) {
                return 4*cell.size();
            } else if (IntAccess.class.isInstance(data)) {
                return 4*cell.size();
            } else {
                logger.info("Unknown data class of cell object "+data.getClass());
                return cell.size();
            }

        } else {
            logger.info("Unknown class of cached object "+object.getClass());
            return 1;
        }
    }

    public long getEstimatedSize() {
        return cache.getCost();
    }

    static class SoftRefs extends LinkedHashMap< Key, SoftReference< Object >>
    {
        private static final long serialVersionUID = 1L;

        private long maxCost;

        AtomicLong totalWeight = new AtomicLong();

        HashMap<Key, Long> cost = new HashMap<>();

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
        protected boolean removeEldestEntry( final Map.Entry< Key, SoftReference< Object > > eldest )
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

        synchronized public void touch( final Key key, final Object value )
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

    public static GlobalCacheBuilder builder() {
        return new GlobalCacheBuilder();
    }

    public static class GlobalCacheBuilder{
        private boolean log = false;
        private int msBetweenLog = 2000;
        private long maxCacheSize = Runtime.getRuntime().maxMemory() ==  Long.MAX_VALUE ? 2*1024*1024*1024 : Runtime.getRuntime().maxMemory() /2;

        public GlobalCacheBuilder log() {
            log = true;
            return this;
        }

        public GlobalCacheBuilder maxSize(long size) {
            maxCacheSize = size;
            return this;
        }

        public GlobalCache create() {
            return new GlobalCache(100, maxCacheSize, log, msBetweenLog);
        }

    }

}
