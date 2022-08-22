package sc.fiji.bdvpg.cache;

import ij.IJ;
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
import java.util.concurrent.atomic.AtomicLong;

public class GlobalCache {

    final static Logger logger = LoggerFactory.getLogger(GlobalCache.class);

    public GlobalCache() {

        new Thread(() -> {
            while(true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                IJ.log("Cache size : "+(cache.getCost()/(1024*1024))+" Mb ("+(int)(100.0*(double) cache.getCost()/ (double) cache.getMaxCost())+" %)");
            }
        }).start();

    }

    SoftRefs cache = new SoftRefs(100, 2_000_000_000);

    void touch( final Key key, Object value ) {
        //cache.getIfPresent(key); // Touch
        cache.touch(key, value);
    }

    static public Key getKey(Object source, int timepoint, int level, Object key){
        return new Key(source, timepoint, level, key);
    }

    public void put(Key key, Object value) {
        cache.touch(key, value);
        //cache.put(key, value);
    }

    public static class Key
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

    public static long getWeight(Object object) {
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

    static class SoftRefs extends LinkedHashMap< Key, SoftReference< Object >>
    {
        private static final long serialVersionUID = 1L;

        private final long maxCost;

        AtomicLong totalWeight = new AtomicLong();

        HashMap<Key, Long> cost = new HashMap<>();

        public SoftRefs( final int iniSize, final long maxCost  )
        {
            super( iniSize, 0.75f, true );
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
            cost.clear();
            super.clear();
        }
    }

}
