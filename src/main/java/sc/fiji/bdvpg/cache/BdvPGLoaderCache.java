/*-
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2017 - 2020 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
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
import net.imglib2.cache.ref.BoundedSoftRefLoaderCache;
import net.imglib2.cache.ref.GuardedStrongRefLoaderCache;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.cache.ref.WeakRefLoaderCache;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.lang.ref.SoftReference;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

/**
 * TODO
 */
public class BdvPGLoaderCache< K, V > implements LoaderCache< K, V >
{
	private final LoaderCache< K, V > cache = new WeakRefLoaderCache<>();//new BoundedSoftRefLoaderCache<>(1000);

	private final GlobalCache globalCache;

	private final Object source;

	private final int timepoint, level;

	public BdvPGLoaderCache(Object source, int timepoint, int level) {
		this.source = source;
		this.timepoint = timepoint;
		this.level = level;
		globalCache = SourceAndConverterServices.getSourceAndConverterService().getCache();
	}

	@Override
	public V getIfPresent( final K key )
	{
		final V value = cache.getIfPresent( key );
		if (value!=null)
			globalCache.touch(GlobalCache.getKey(source, timepoint, level, key ), value);
		return value;
	}

	@Override
	public V get( final K key, final CacheLoader< ? super K, ? extends V > loader ) throws ExecutionException
	{
		final V value = cache.get( key, loader );
		globalCache.put(GlobalCache.getKey(source, timepoint, level, key ), value);
		return value;
	}

	@Override
	public void persist( final K key )
	{}

	@Override
	public void persistIf( final Predicate< K > condition )
	{}

	@Override
	public void persistAll()
	{}

	@Override
	public void invalidate( final K key )
	{
		cache.invalidate( key );
	}

	@Override
	public void invalidateIf( final long parallelismThreshold, final Predicate< K > condition )
	{
		cache.invalidateIf( parallelismThreshold, condition );
	}

	@Override
	public void invalidateAll( final long parallelismThreshold )
	{
		cache.invalidateAll( parallelismThreshold );
	}

}
