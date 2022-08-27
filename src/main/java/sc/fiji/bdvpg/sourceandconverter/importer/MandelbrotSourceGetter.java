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

package sc.fiji.bdvpg.sourceandconverter.importer;

import bdv.util.Procedural3DImageShort;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.function.Supplier;

public class MandelbrotSourceGetter implements Runnable,
	Supplier<SourceAndConverter<UnsignedShortType>>
{

	final int maxIterations = 255;

	public void run() {
		// Useless
	}

	@Override
	public SourceAndConverter<UnsignedShortType> get() {
		Source<UnsignedShortType> s = new Procedural3DImageShort(p -> {
			double re = p[0];
			double im = p[1];
			int i = 0;
			for (; i < maxIterations; ++i) {
				final double squre = re * re;
				final double squim = im * im;
				if (squre + squim > 4) break;
				im = 2 * re * im + p[1];
				re = squre - squim + p[0];
			}
			return i;
		}).getSource("Mandelbrot Set");

		return SourceAndConverterHelper.createSourceAndConverter(s);
	}

}
