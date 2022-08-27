/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import java.lang.ref.WeakReference;

public class GlobalCacheKey {

	private final WeakReference<Object> source;

	private final int timepoint;

	private final int level;

	public final WeakReference<Object> key;

	public GlobalCacheKey(final Object source, final int timepoint,
		final int level, final Object key)
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

	public boolean partialEquals(final Object source, final int timepoint,
		final int level)
	{
		if (this.source.get() == null) return false;
		if (key.get() == null) return false;

		return (this.source.get() == source) && (this.timepoint == timepoint) &&
			(this.level == level);
	}

	@Override
	public boolean equals(final Object other) {
		if (source.get() == null) return false;
		if (key.get() == null) return false;

		if (this == other) return true;
		if (!(other instanceof GlobalCacheKey)) return false;
		final GlobalCacheKey that = (GlobalCacheKey) other;

		return (this.source.get() == that.source.get()) &&
			(this.timepoint == that.timepoint) && (this.level == that.level) &&
			(this.key.get().equals(that.key.get()));
	}

	final int hashcode;

	@Override
	public int hashCode() {
		return hashcode;
	}
}
