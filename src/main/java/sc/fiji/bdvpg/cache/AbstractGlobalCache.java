
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

/**
 * A memory bounded global cache at the JVM level that can be used by many
 * bigdataviewer sources coming from different origins (spimdata, resampled
 * sources). To use it, instantiate a {@link GlobalLoaderCache}, and the loaded
 * objects will be forwarded to the global cache of BigDataViewer-Playground.
 * BigDataViewer-Playground also attempts to override the cache of any
 * {@link bdv.ViewerImgLoader} in order to use the bigdataviewer playground
 * global cache. Two implementations are provided: - a Caffeine backed cache
 * {@link CaffeineGlobalCache} - and a LinkedHashMap cache
 * {@link BoundedLinkedHashMapGlobalCache} The
 * {@link AbstractGlobalCache.Builder} object can be serialized to store the
 * cache configuration The global caching allows to bound the memory used when
 * many sources are potentially accessed in a random manner by the program. The
 * reason to use many sources is if the user wants to work on different spim
 * data objects at the same time, work with resampled sources, etc Global
 * caching allow to bound the memory used at the JVM level, instead of a per
 * source level or per spimdata level. Each value of the value is weighted by
 * its memory footprint, assuming it is a {@link Cell} object. If the value is
 * not a Cell object, an error is logged. This allows to bound memory correctly
 * even when sources have very different block size (setting a fixed number of
 * items maintained in cache would not be precise enough).
 *
 * @author Nicolas Chiaruttini
 */
abstract public class AbstractGlobalCache implements
	Cache<GlobalCacheKey, Object>
{

	final static private Logger logger = LoggerFactory.getLogger(
		AbstractGlobalCache.class);

	public static <K> Predicate<GlobalCacheKey> getCondition(Object source,
		int timepoint, int level, Predicate<K> condition)
	{
		return (key) -> key.partialEquals(source, timepoint, level) && condition
			.test((K) key.key.get());
	}

	abstract public void setMaxSize(long maxCacheSize);

	static public GlobalCacheKey getKey(Object source, int timepoint, int level,
		Object key)
	{
		return new GlobalCacheKey(source, timepoint, level, key);
	}

	abstract public void put(GlobalCacheKey key, Object value);

	@Override
	abstract public Object get(GlobalCacheKey key) throws ExecutionException;

	@Override
	abstract public Object getIfPresent(GlobalCacheKey key);

	@Override
	public void persist(GlobalCacheKey key) {}

	@Override
	public void persistIf(Predicate<GlobalCacheKey> condition) {}

	@Override
	public void persistAll() {}

	@Override
	abstract public void invalidate(GlobalCacheKey key);

	@Override
	abstract public void invalidateIf(long parallelismThreshold,
		Predicate<GlobalCacheKey> condition);

	@Override
	abstract public void invalidateAll(long parallelismThreshold);

	abstract public long getMaxSize();

	static long getWeight(Object object) {
		if (object instanceof Cell) {
			Cell cell = ((Cell) object);
			Object data = cell.getData();

			if (ShortAccess.class.isInstance(data)) {
				return 2 * cell.size();
			}
			else if (ByteAccess.class.isInstance(data)) {
				return cell.size();
			}
			else if (FloatAccess.class.isInstance(data)) {
				return 4 * cell.size();
			}
			else if (IntAccess.class.isInstance(data)) {
				return 4 * cell.size();
			}
			else {
				logger.info("Unknown data class of cell object " + data.getClass());
				return cell.size();
			}

		}
		else {
			logger.info("Unknown class of cached object " + object.getClass());
			return 1;
		}
	}

	abstract public long getEstimatedSize();

	public abstract <V> void touch(GlobalCacheKey key, V value);

}
