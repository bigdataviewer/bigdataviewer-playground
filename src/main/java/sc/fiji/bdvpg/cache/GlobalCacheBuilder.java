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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalCacheBuilder {

	final static Logger logger = LoggerFactory.getLogger(
		GlobalCacheBuilder.class);

	final public static String LINKED_HASH_MAP = "LinkedHashMap";
	final public static String CAFFEINE = "Caffeine";

	final static double defaultPolicyRatio = 0.5;

	private boolean log = false;
	private int msBetweenLog = 2000;

	// Policy one : set a ratio of the total available memory ( default policy
	private double memoryRatioForCache = defaultPolicyRatio; // if set, the cache
																														// will occupy this
																														// ratio of the max
																														// memory

	// Policy two : set a cache size explicitely
	private long memoryInBytesForCache = -1;

	// Policy three : specify the amount of ram not used for caching
	private long memoryInBytesForEverythingElse = -1;

	transient long maxAvailableMemoryInBytes;

	String cacheType = CAFFEINE;

	protected GlobalCacheBuilder() {
		if (Runtime.getRuntime().maxMemory() == Long.MAX_VALUE) {
			logger.error(
				"Cannot fetch maximum runtime memory! Maximal memory available set completely arbitrarily to 20 Gb");
			maxAvailableMemoryInBytes = 21_474_836_480L;
		}
		else {
			maxAvailableMemoryInBytes = (long) (Runtime.getRuntime().maxMemory() *
				0.5);
		}
	}

	protected GlobalCacheBuilder(long maxAvailableMemoryInBytes) {
		this.maxAvailableMemoryInBytes = maxAvailableMemoryInBytes;
	}

	// Accessors for the scijava cache option command
	public String getCacheType() {
		return cacheType;
	}

	public boolean getLog() {
		return log;
	}

	public int getMsBetweenLog() {
		return msBetweenLog;
	}

	public long getMemoryInBytesForCache() {
		return memoryInBytesForCache;
	}

	public long getMemoryInBytesForEverythingElse() {
		return memoryInBytesForEverythingElse;
	}

	public double getMemoryRatioForCache() {
		return memoryRatioForCache;
	}

	// builder methods
	public static GlobalCacheBuilder builder() {
		return new GlobalCacheBuilder();
	}

	public static GlobalCacheBuilder builder(long explicitMaxMemoryUsable) {
		return new GlobalCacheBuilder(explicitMaxMemoryUsable);
	}

	public GlobalCacheBuilder log(int msBetweenLog) {
		log = true;
		this.msBetweenLog = msBetweenLog;
		return this;
	}

	public GlobalCacheBuilder maxSize(long size) {
		maxAvailableMemoryInBytes = size;
		return this;
	}

	public GlobalCacheBuilder caffeine() {
		cacheType = CAFFEINE;
		return this;
	}

	public GlobalCacheBuilder linkedHashMap() {
		cacheType = LINKED_HASH_MAP;
		return this;
	}

	public GlobalCacheBuilder memoryRatioForCache(double ratio) {
		memoryRatioForCache = ratio;
		memoryInBytesForCache = -1;
		memoryInBytesForEverythingElse = -1;
		return this;
	}

	public GlobalCacheBuilder memoryForCache(long nBytes) {
		memoryRatioForCache = -1;
		memoryInBytesForCache = nBytes;
		memoryInBytesForEverythingElse = -1;
		return this;
	}

	public GlobalCacheBuilder memoryForEverythingElse(long nBytes) {
		memoryRatioForCache = -1;
		memoryInBytesForCache = -1;
		memoryInBytesForEverythingElse = nBytes;
		return this;
	}

	public AbstractGlobalCache create() {
		long cacheSize;

		if (memoryRatioForCache > 0) {
			cacheSize = (long) (memoryRatioForCache * maxAvailableMemoryInBytes);
		}
		else if (memoryInBytesForCache > 0) {
			if (memoryInBytesForCache > maxAvailableMemoryInBytes) {
				logger.warn("You are setting a cache size (" + memoryInBytesForCache +
					")which is higher than the total available memory from the JVM (" +
					maxAvailableMemoryInBytes + ")!");
			}
			cacheSize = memoryInBytesForCache;
		}
		else if (memoryInBytesForEverythingElse > 0) {
			cacheSize = maxAvailableMemoryInBytes - memoryInBytesForEverythingElse;
			if (cacheSize < 0) {
				logger.warn(
					"You are setting a cache size of 0 bytes. Setting it back to default policy.");
				memoryRatioForCache = defaultPolicyRatio;
				cacheSize = (long) (memoryRatioForCache * maxAvailableMemoryInBytes);
			}
		}
		else {
			logger.error("Unknown cache policy! Setting it back to default policy.");
			memoryRatioForCache = defaultPolicyRatio;
			cacheSize = (long) (memoryRatioForCache * maxAvailableMemoryInBytes);
		}

		logger.info("Global cache set to " + (int) (cacheSize / (1024.0 * 1024.0)) +
			" Mb, out of " + (int) (maxAvailableMemoryInBytes / (1024.0 * 1024.0)) +
			" Mb available (" + (int) (100.0 * (cacheSize / (1024.0 * 1024.0)) /
				(maxAvailableMemoryInBytes / (1024.0 * 1024.0))) + "%)");

		switch (cacheType) {
			case CAFFEINE:
				return new CaffeineGlobalCache(cacheSize, log, msBetweenLog);
			case LINKED_HASH_MAP:
				return new BoundedLinkedHashMapGlobalCache(100, cacheSize, log,
					msBetweenLog);
			default:
				throw new UnsupportedOperationException("Cannot create cache of type " +
					cacheType);
		}
	}

}
