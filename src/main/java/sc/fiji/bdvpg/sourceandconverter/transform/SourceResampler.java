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

package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.util.ResampledSource;
import bdv.util.VolatileSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.function.Function;

public class SourceResampler<T extends NumericType<T> & NativeType<T>>
	implements Runnable, Function<SourceAndConverter<T>, SourceAndConverter<T>>
{

	final SourceAndConverter<T> sac_in;

	final SourceAndConverter<?> model;

	final boolean reuseMipMaps;

	final boolean interpolate;

	final boolean cache;

	final int defaultMipMapLevel;

	private final String name;

	public SourceResampler(SourceAndConverter<T> sac_in,
		SourceAndConverter<?> model, String name, boolean reuseMipmaps,
		boolean cache, boolean interpolate, int defaultMipMapLevel)
	{
		this.name = name;
		this.reuseMipMaps = reuseMipmaps;
		this.model = model;
		this.sac_in = sac_in;
		this.interpolate = interpolate;
		this.cache = cache;
		this.defaultMipMapLevel = defaultMipMapLevel;
	}

	@Override
	public void run() {

	}

	public SourceAndConverter<T> get() {
		return apply(sac_in);
	}

	@Override
	public SourceAndConverter<T> apply(SourceAndConverter<T> src) {
		Source<T> srcRsampled = new ResampledSource<>(src.getSpimSource(), model
			.getSpimSource(), name, reuseMipMaps, cache, interpolate,
			defaultMipMapLevel);

		SourceAndConverter<T> sac;
		if (src.asVolatile() != null) {
			SourceAndConverter<? extends Volatile<T>> vsac;
			Source<? extends Volatile<T>> vsrcResampled;
			if (cache) {
				vsrcResampled = new VolatileSource<>(srcRsampled);
			}
			else {
				vsrcResampled = new ResampledSource(src.asVolatile().getSpimSource(),
					model.getSpimSource(), name, reuseMipMaps, false, interpolate,
					defaultMipMapLevel);
			}
			vsac = new SourceAndConverter(vsrcResampled, SourceAndConverterHelper
				.cloneConverter(src.asVolatile().getConverter(), src.asVolatile()));
			sac = new SourceAndConverter<>(srcRsampled, SourceAndConverterHelper
				.cloneConverter(src.getConverter(), src), vsac);
		}
		else {
			sac = new SourceAndConverter<>(srcRsampled, SourceAndConverterHelper
				.cloneConverter(src.getConverter(), src));
		}
		return sac;
	}
}
