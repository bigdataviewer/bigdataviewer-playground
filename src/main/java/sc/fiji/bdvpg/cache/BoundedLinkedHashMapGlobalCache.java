/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.bdvpg.cache;

import bdv.img.cache.VolatileGlobalCellCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class BoundedLinkedHashMapGlobalCache extends AbstractGlobalCache {

	final static Logger logger = LoggerFactory.getLogger(
		BoundedLinkedHashMapGlobalCache.class);

	final SoftRefs cache;

	BoundedLinkedHashMapGlobalCache(int iniSize, long maxCacheSize, boolean log,
		int msBetweenLogs)
	{

		cache = new SoftRefs(iniSize, maxCacheSize);

		if (log) {
			TimerTask periodicLogger = new TimerTask() {

				@Override
				public void run() {
					logger.info(BoundedLinkedHashMapGlobalCache.this.toString());
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
		// Access-order get() reorders the map (a structural modification), so it
		// must be guarded against concurrent touch()/trim eviction.
		synchronized (cache) {
			return cache.get(key);
		}
	}

	@Override
	public Object getIfPresent(GlobalCacheKey key) {
		synchronized (cache) {
			return cache.get(key);
		}
	}

	@Override
	public void invalidate(GlobalCacheKey key) {
		cache.removeEntry(key);
	}

	@Override
	public void invalidateIf(long parallelismThreshold,
		Predicate<GlobalCacheKey> condition)
	{
		cache.removeIfKey(condition);
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

	static class SoftRefs extends
		LinkedHashMap<GlobalCacheKey, SoftReference<Object>>
	{

		private static final long serialVersionUID = 1L;

		private long maxCost;

		AtomicLong totalWeight = new AtomicLong();

		HashMap<GlobalCacheKey, Long> cost = new HashMap<>();

		public SoftRefs(final int iniSize, final long maxCost) {
			super(iniSize, 0.75f, true);
			this.maxCost = maxCost;
		}

		synchronized public void setMaxCost(long maxCost) {
			this.maxCost = maxCost;
			trimToCost(null);
		}

		public long getCost() {
			return totalWeight.get();
		}

		public long getMaxCost() {
			return maxCost;
		}

		/*
		 * Note: we deliberately do NOT override removeEldestEntry. That hook is
		 * invoked at most once per insertion and removes at most a single
		 * (possibly tiny) entry, with no notion of how much weight still needs
		 * to be reclaimed. When block sizes vary a lot - e.g. a 10 MB block is
		 * inserted while the LRU tail holds only 10 kB entries - evicting one
		 * entry frees far too little and the cache overshoots maxCost. Eviction
		 * is therefore performed by the explicit loop in trimToCost() below.
		 */

		synchronized public void touch(final GlobalCacheKey key,
			final Object value)
		{
			final SoftReference<Object> ref = get(key);
			if (ref == null) {
				long costValue = getWeight(value);
				totalWeight.addAndGet(costValue);
				cost.put(key, costValue);
				put(key, new SoftReference<>(value));
				trimToCost(key);
			}
			else if (ref.get() == null) {
				put(key, new SoftReference<>(value));
			}
		}

		/**
		 * Evicts least-recently-used entries until the total weight no longer
		 * exceeds {@link #maxCost}. Iterates in access order (eldest first); the
		 * just-inserted {@code protect} key, if any, is skipped so that a single
		 * block larger than the whole budget is still cached rather than
		 * discarded immediately after being loaded (in that degenerate case the
		 * cache holds exactly that one block and maxCost is exceeded by it
		 * alone). Must be called while holding this monitor.
		 *
		 * @param protect key that must not be evicted, or {@code null}
		 */
		private void trimToCost(final GlobalCacheKey protect) {
			if (totalWeight.get() <= maxCost) return;
			final Iterator<Map.Entry<GlobalCacheKey, SoftReference<Object>>> it =
				entrySet().iterator();
			while (totalWeight.get() > maxCost && it.hasNext()) {
				final Map.Entry<GlobalCacheKey, SoftReference<Object>> eldest = it
					.next();
				if (eldest.getKey() == protect) continue;
				final Long c = cost.remove(eldest.getKey());
				if (c != null) totalWeight.addAndGet(-c);
				eldest.getValue().clear();
				it.remove();
			}
		}

		/**
		 * Removes a single entry, keeping the weight accounting consistent:
		 * decrements {@link #totalWeight} and drops the entry from the
		 * {@link #cost} map. Plain {@code remove()} would leave the weight
		 * counted forever, inflating the perceived cache size.
		 *
		 * @param key key to remove
		 */
		synchronized public void removeEntry(final GlobalCacheKey key) {
			final SoftReference<Object> ref = remove(key);
			if (ref != null) {
				ref.clear();
				final Long c = cost.remove(key);
				if (c != null) totalWeight.addAndGet(-c);
			}
		}

		/**
		 * Removes every entry whose key matches {@code condition}, keeping the
		 * weight accounting consistent. Replaces {@code keySet().removeIf(...)},
		 * which would remove entries without updating {@link #totalWeight} or the
		 * {@link #cost} map.
		 *
		 * @param condition predicate selecting keys to remove
		 */
		synchronized public void removeIfKey(
			final Predicate<GlobalCacheKey> condition)
		{
			final Iterator<Map.Entry<GlobalCacheKey, SoftReference<Object>>> it =
				entrySet().iterator();
			while (it.hasNext()) {
				final Map.Entry<GlobalCacheKey, SoftReference<Object>> e = it.next();
				if (condition.test(e.getKey())) {
					final Long c = cost.remove(e.getKey());
					if (c != null) totalWeight.addAndGet(-c);
					e.getValue().clear();
					it.remove();
				}
			}
		}

		@Override
		public synchronized void clear() {
			for (final SoftReference<Object> ref : values()) {
				ref.clear();
			}
			totalWeight.set(0);
			cost.clear();
			super.clear();
		}
	}

	@Override
	public String toString() {
		return "Cache size : " + (cache.getCost() / (1024 * 1024)) + " Mb (" +
			(int) (100.0 * (double) cache.getCost() / (double) cache.getMaxCost()) +
			" %)";
	}

	@Override
	public CacheStats getCacheStats(Object source, int setupid, int timepoint) {
		long totalSize = 0;
		long cellCount = 0;
        try {

            Field tpField = VolatileGlobalCellCache.Key.class.getDeclaredField("timepoint");
            tpField.setAccessible(true);
            Field setupField = VolatileGlobalCellCache.Key.class.getDeclaredField("setup");
            setupField.setAccessible(true);

            synchronized (cache) {
                for (Map.Entry<GlobalCacheKey, SoftReference<Object>> entry : cache.entrySet()) {
                    GlobalCacheKey key = entry.getKey();
                    VolatileGlobalCellCache.Key innerKey = null;
                    if (key.key.get() instanceof VolatileGlobalCellCache.Key) {
                        innerKey = (VolatileGlobalCellCache.Key) key.key.get();
                    }
                    if (innerKey == null) continue;
                    int tp = (int) tpField.get(innerKey);
                    int setupId = (int) setupField.get(innerKey);

                    if (timepoint == -1) {
                        // Match source for any timepoint
                        if ((key.getSource() == source) && (setupid == setupId)) {
                            Long cost = cache.cost.get(key);
                            if (cost != null) {
                                totalSize += cost;
                                cellCount++;
                            }
                        }
                    } else {
                        // Match source and specific timepoint (any level)
                        if ((key.getSource() == source) && (tp == timepoint) && (setupid == setupId)) {
                            Long cost = cache.cost.get(key);
                            if (cost != null) {
                                totalSize += cost;
                                cellCount++;
                            }
                        }
                    }

                }
                return new CacheStats(cellCount, totalSize);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new CacheStats(0,0);
        }
	}

	@Override
	public CacheStats getCacheStats(Object source, int timepoint) {
		long totalSize = 0;
		long cellCount = 0;

		synchronized (cache) {
			for (Map.Entry<GlobalCacheKey, SoftReference<Object>> entry : cache
				.entrySet())
			{
				GlobalCacheKey key = entry.getKey();
				if (timepoint == -1) {
					// Match source for any timepoint
					if (key.getSource() == source) {
						Long cost = cache.cost.get(key);
						if (cost != null) {
							totalSize += cost;
							cellCount++;
						}
					}
				}
				else {
					// Match source and specific timepoint (any level)
					if (key.getSource() == source && key.getTimepoint() == timepoint) {
						Long cost = cache.cost.get(key);
						if (cost != null) {
							totalSize += cost;
							cellCount++;
						}
					}
				}
			}
		}

		return new CacheStats(cellCount, totalSize);
	}

}
