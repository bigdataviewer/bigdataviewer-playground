package sc.fiji.bdvpg.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Weigher;
import ij.IJ;
import net.imglib2.img.cell.Cell;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public class GlobalCache {

    public GlobalCache() {

        new Thread(() -> {
            while(true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //IJ.log("Cache estimated size : "+cache.stats().requestCount()+" / "+maxNumberOfPixels);
            }
        }).start();

    }

    final long maxNumberOfPixels = 100_000_000;// Runtime.getRuntime().maxMemory() / 2;

    /*final Cache< Key, Object > cache = Caffeine.newBuilder()
            .maximumWeight(maxNumberOfPixels)
            //.maximumSize(100)
            .softValues()
            .weigher((Weigher<Key, Object>) (key, value) -> {
                if (value instanceof Cell) {
                    return (int) ((Cell) value).size();
                } else return 1;
            })
            .removalListener((Key key, Object object, RemovalCause cause) ->
                    System.out.printf("Key %s was removed (%s)%n", key, cause))
            .build();*/

    SoftRefs cache = new SoftRefs(100);

    void touch( final Key key, Object value ) {
        //cache..getIfPresent(key); // Touch
        cache.touch(key, value);
    }

    static public Key getKey(Object source, int timepoint, int level, Object key){
        return new Key(source, timepoint, level, key);
    }

    public void put(Key key, Object value) {
        cache.touch(key, value);
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

    static class SoftRefs extends LinkedHashMap< Key, SoftReference< Object >>
    {
        private static final long serialVersionUID = 1L;

        private final int maxSoftRefs;

        public SoftRefs( final int maxSoftRefs )
        {
            super( maxSoftRefs, 0.75f, true );
            this.maxSoftRefs = maxSoftRefs;
        }

        @Override
        protected boolean removeEldestEntry( final Map.Entry< Key, SoftReference< Object > > eldest )
        {
            if ( size() > maxSoftRefs )
            {
                eldest.getValue().clear();
                return true;
            }
            else
                return false;
        }

        synchronized public void touch( final Key key, final Object value )
        {
            final SoftReference< Object > ref = get( key );
            if ( ref == null || ref.get() == null )
                put( key, new SoftReference<>( value ) );
        }

        @Override
        public synchronized void clear()
        {
            for ( final SoftReference< Object > ref : values() )
                ref.clear();
            super.clear();
        }
    }

}
