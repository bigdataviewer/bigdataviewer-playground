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

package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.util.OutOfBoundsColorChangedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.function.Function;

public class SourceOutOfBoundsColorChanger<T extends NumericType<T> & NativeType<T>>
	implements Runnable, Function<SourceAndConverter<T>, SourceAndConverter<T>>
{

	final SourceAndConverter<T> sac_in;

	final T outOfBoundsColor;

	public SourceOutOfBoundsColorChanger(final SourceAndConverter<T> sac_in,
                                         final T outOfBoundsColor)
	{
		this.outOfBoundsColor = outOfBoundsColor;
		this.sac_in = sac_in;
	}

	@Override
	public void run() {

	}

	public SourceAndConverter<T> get() {
		return apply(sac_in);
	}

	@Override
	public SourceAndConverter<T> apply(final SourceAndConverter<T> src) {
		final Source<T> srcNewBg = new OutOfBoundsColorChangedSource<>(src.getSpimSource(), outOfBoundsColor);

		SourceAndConverter<T> sac;
		if (src.asVolatile() != null) {
			SourceAndConverter<? extends Volatile<T>> vsac;
			Source<? extends Volatile<T>> vsrcNewBg;
			// Rah...
			Volatile<T> vOutOfBoundsColor;
			if (outOfBoundsColor instanceof UnsignedShortType) {
				vOutOfBoundsColor = (Volatile<T>) new VolatileUnsignedShortType(((UnsignedShortType) outOfBoundsColor).get());
			} else if (outOfBoundsColor instanceof UnsignedByteType) {
				vOutOfBoundsColor = (Volatile<T>) new VolatileUnsignedByteType(((UnsignedByteType) outOfBoundsColor).get());
			} else if (outOfBoundsColor instanceof ARGBType) {
				vOutOfBoundsColor = (Volatile<T>) new VolatileARGBType(((ARGBType) outOfBoundsColor).get());
			} else if (outOfBoundsColor instanceof FloatType) {
				vOutOfBoundsColor = (Volatile<T>) new VolatileFloatType(((FloatType) outOfBoundsColor).get());
			} else {
				throw new RuntimeException("Sorry, can't find matching volatile type of pixel class "+outOfBoundsColor.getClass().getSimpleName()+". Please contribute to bdv-playground SourceOutOfBoundsColorChanger class");
			}

			vsrcNewBg = new OutOfBoundsColorChangedSource(src.asVolatile().getSpimSource(), (NumericType) vOutOfBoundsColor);
			vsac = new SourceAndConverter(vsrcNewBg, SourceAndConverterHelper
				.cloneConverter(src.asVolatile().getConverter(), src.asVolatile()));
			sac = new SourceAndConverter<>(srcNewBg, SourceAndConverterHelper
				.cloneConverter(src.getConverter(), src), vsac);
		}
		else {
			sac = new SourceAndConverter<>(srcNewBg, SourceAndConverterHelper
				.cloneConverter(src.getConverter(), src));
		}
		return sac;
	}
}
