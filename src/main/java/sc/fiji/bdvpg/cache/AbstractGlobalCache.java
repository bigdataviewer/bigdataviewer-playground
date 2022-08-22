package sc.fiji.bdvpg.cache;

import net.imglib2.cache.Cache;
import net.imglib2.img.basictypeaccess.ByteAccess;
import net.imglib2.img.basictypeaccess.FloatAccess;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.ShortAccess;
import net.imglib2.img.cell.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

abstract public class AbstractGlobalCache implements Cache<GlobalCacheKey, Object > {

    final static private Logger logger = LoggerFactory.getLogger(AbstractGlobalCache.class);

    public static <K> Predicate<GlobalCacheKey> getCondition(Object source, int timepoint, int level, Predicate<K> condition) {
        return (key) -> key.partialEquals(source, timepoint, level) && condition.test((K)key.key.get());
    }

    abstract public void setMaxSize(long maxCacheSize);

    static public GlobalCacheKey getKey(Object source, int timepoint, int level, Object key){
        return new GlobalCacheKey(source, timepoint, level, key);
    }

    abstract public void put(GlobalCacheKey key, Object value);

    @Override
    abstract public Object get(GlobalCacheKey key) throws ExecutionException;

    @Override
    abstract public Object getIfPresent(GlobalCacheKey key);

    @Override
    public void persist(GlobalCacheKey key) {
    }

    @Override
    public void persistIf(Predicate<GlobalCacheKey> condition) {
    }

    @Override
    public void persistAll() {
    }

    @Override
    abstract public void invalidate(GlobalCacheKey key);

    @Override
    abstract public void invalidateIf(long parallelismThreshold, Predicate<GlobalCacheKey> condition);

    @Override
    abstract public void invalidateAll(long parallelismThreshold);

    abstract public long getMaxSize();

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

    abstract public long getEstimatedSize();

    public static AbstractGlobalCache.GlobalCacheBuilder builder() {
        return new AbstractGlobalCache.GlobalCacheBuilder();
    }

    public abstract <V> void touch(GlobalCacheKey key, V value);

    public static class GlobalCacheBuilder{
        private boolean log = false;
        private int msBetweenLog = 2000;
        private long maxCacheSize = Runtime.getRuntime().maxMemory() ==  Long.MAX_VALUE ? 2*1024*1024*1024 : (long) (Runtime.getRuntime().maxMemory() * 0.5);

        public GlobalCacheBuilder log() {
            log = true;
            return this;
        }

        public GlobalCacheBuilder maxSize(long size) {
            maxCacheSize = size;
            return this;
        }

        public BoundedLinkedHashMapGlobalCache createLinkedHashMap() {
            return new BoundedLinkedHashMapGlobalCache(100, maxCacheSize, log, msBetweenLog);
        }

        public CaffeineGlobalCache createCaffeineCache() {
            return new CaffeineGlobalCache(maxCacheSize, log, msBetweenLog);
        }

    }
}
