package sc.fiji.bdvpg.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import org.jetbrains.annotations.NotNull;
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

public class CaffeineGlobalCache extends AbstractGlobalCache {

    final static Logger logger = LoggerFactory.getLogger(CaffeineGlobalCache.class);

    final Cache<GlobalCacheKey, Object> cache;

    final long maxCacheSize;

    CaffeineGlobalCache(long maxCacheSize, boolean log, int msBetweenLogs) {
        this.maxCacheSize = maxCacheSize;
        cache = Caffeine.newBuilder()
                .maximumWeight(maxCacheSize)
                .softValues()
                .weigher((Weigher<GlobalCacheKey, Object>) (key, value) -> (int) AbstractGlobalCache.getWeight(value))
                .build();

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
        throw new UnsupportedOperationException("Can't changed caffeine backed max cache size");
    }

    public void put(GlobalCacheKey key, Object value) {
        cache.put(key, value);
    }

    @Override
    public Object get(GlobalCacheKey key) throws ExecutionException {
        logger.error("Cannot use get");
        return null;
    }

    @Override
    public Object getIfPresent(GlobalCacheKey key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void invalidate(GlobalCacheKey key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateIf(long parallelismThreshold, Predicate<GlobalCacheKey> condition) {
        throw new UnsupportedOperationException("Can't invalidate based on predicate");
    }

    @Override
    public void invalidateAll(long parallelismThreshold) {
        cache.invalidateAll();
    }

    public long getMaxSize() {
        return maxCacheSize;
    }

    public long getEstimatedSize() {
        return cache.estimatedSize()*1_000_000;
    }

    @Override
    public <V> void touch(GlobalCacheKey key, V value) {
        cache.getIfPresent(key); // for frequency use
    }

    @Override
    public String toString() {
        return "Cache size : " + (cache.estimatedSize());
    }

}
