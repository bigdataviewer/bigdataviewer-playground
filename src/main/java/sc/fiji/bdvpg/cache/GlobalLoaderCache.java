/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.ref.WeakRefLoaderCache;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

/**
 * LoaderCache which uses BigDataViewer-Playground global cache. See
 * {@link AbstractGlobalCache} Can be used to cache multi-timepoint
 * multi-resolution bdv {@link bdv.viewer.Source}s
 *
 * @author Nicolas Chiaruttini
 */
public class GlobalLoaderCache<K, V> implements LoaderCache<K, V> {

	private final LoaderCache<K, V> cache = new WeakRefLoaderCache<>();

	private final AbstractGlobalCache globalCache;

	private final Object source;

	private final int timepoint, level;

	/**
	 * Creates a loader cache object for a 3D rai of a source
	 * 
	 * @param source used in the keys of the global cache to know which object it
	 *          belongs to
	 * @param timepoint timepoint of the rai cached
	 * @param level resolution level
	 */
	public GlobalLoaderCache(Object source, int timepoint, int level) {
		this.source = source;
		this.timepoint = timepoint;
		this.level = level;
		globalCache = SourceAndConverterServices.getSourceAndConverterService()
			.getCache();
	}

	/**
	 * Creates a loader cache object for a specific source object
	 * 
	 * @param source used in the keys of the global cache to know which object it
	 *          belongs to
	 */
	public GlobalLoaderCache(Object source) {
		this.source = source;
		this.timepoint = -1;
		this.level = -1;
		globalCache = SourceAndConverterServices.getSourceAndConverterService()
			.getCache();
	}

	@Override
	public V getIfPresent(final K key) {
		final V value = cache.getIfPresent(key);
		if (value != null) globalCache.touch(BoundedLinkedHashMapGlobalCache.getKey(
			source, timepoint, level, key), value);
		return value;
	}

	@Override
	public V get(final K key, final CacheLoader<? super K, ? extends V> loader)
		throws ExecutionException
	{
		final V value = cache.get(key, loader);
		globalCache.put(BoundedLinkedHashMapGlobalCache.getKey(source, timepoint,
			level, key), value);
		return value;
	}

	@Override
	public void persist(final K key) {}

	@Override
	public void persistIf(final Predicate<K> condition) {}

	@Override
	public void persistAll() {}

	@Override
	public void invalidate(final K key) {
		cache.invalidate(key);
		globalCache.invalidate(BoundedLinkedHashMapGlobalCache.getKey(source,
			timepoint, level, key));
	}

	@Override
	public void invalidateIf(final long parallelismThreshold,
		final Predicate<K> condition)
	{
		cache.invalidateIf(parallelismThreshold, condition);
		globalCache.invalidateIf(BoundedLinkedHashMapGlobalCache.getCondition(
			source, timepoint, level, condition));
	}

	@Override
	public void invalidateAll(final long parallelismThreshold) {
		cache.invalidateAll(parallelismThreshold);
	}

}
