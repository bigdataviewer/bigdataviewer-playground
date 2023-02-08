/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Weigher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

public class CaffeineGlobalCache extends AbstractGlobalCache {

	final static Logger logger = LoggerFactory.getLogger(
		CaffeineGlobalCache.class);

	final Cache<GlobalCacheKey, Object> cache;

	final long maxCacheSize;

	CaffeineGlobalCache(long maxCacheSize, boolean log, int msBetweenLogs) {
		this.maxCacheSize = maxCacheSize;
		cache = Caffeine.newBuilder().maximumWeight(maxCacheSize).softValues()
			.weigher((Weigher<GlobalCacheKey, Object>) (key,
				value) -> (int) AbstractGlobalCache.getWeight(value)).build();

		if (log) {
			TimerTask periodicLogger = new TimerTask() {

				@Override
				public void run() {
					logger.info(CaffeineGlobalCache.this.toString());
				}
			};

			Timer time = new Timer(); // Instantiate Timer Object
			time.schedule(periodicLogger, 0, msBetweenLogs);
		}

	}

	public void setMaxSize(long maxCacheSize) {
		throw new UnsupportedOperationException(
			"Can't changed caffeine backed max cache size");
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
	public void invalidateIf(long parallelismThreshold,
		Predicate<GlobalCacheKey> condition)
	{
		throw new UnsupportedOperationException(
			"Can't invalidate based on predicate");
	}

	@Override
	public void invalidateAll(long parallelismThreshold) {
		cache.invalidateAll();
	}

	public long getMaxSize() {
		return maxCacheSize;
	}

	public long getEstimatedSize() {
		return cache.estimatedSize() * 1_000_000;
	}

	@Override
	public <V> void touch(GlobalCacheKey key, V value) {
		cache.getIfPresent(key); // for frequency use
	}

	@Override
	public String toString() {
		long totalBytes = cache.policy().eviction().get().weightedSize()
			.getAsLong();
		return "Cache size : " + (totalBytes / (1024 * 1024)) + " Mb (" +
			(int) (100.0 * (double) totalBytes / (double) maxCacheSize) + " %)";
	}

}
