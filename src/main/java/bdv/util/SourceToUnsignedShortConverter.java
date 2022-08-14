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

package bdv.util;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper function to transform RealTyped {@link bdv.viewer.Source} to
 * {@link UnsignedShortType} source TODO : improved conversion or retire... the
 * conversion is not modular it's a direct casting to int
 */

public class SourceToUnsignedShortConverter {

	protected static final Logger logger = LoggerFactory.getLogger(
		SourceToUnsignedShortConverter.class);

	static public Source<UnsignedShortType> convertRealSource(
		Source<RealType> iniSrc)
	{
		Converter<RealType, UnsignedShortType> cvt = (i, o) -> o.set((int) i
			.getRealDouble());
		Source<UnsignedShortType> cvtSrc = new Source<UnsignedShortType>() {

			@Override
			public boolean isPresent(int t) {
				return iniSrc.isPresent(t);
			}

			@Override
			public RandomAccessibleInterval<UnsignedShortType> getSource(int t,
				int level)
			{
				return Converters.convert(iniSrc.getSource(t, level), cvt,
					new UnsignedShortType());
			}

			@Override
			public RealRandomAccessible<UnsignedShortType> getInterpolatedSource(
				int t, int level, Interpolation method)
			{
				return Converters.convert(iniSrc.getInterpolatedSource(t, level,
					method), cvt, new UnsignedShortType());
			}

			@Override
			public void getSourceTransform(int t, int level,
				AffineTransform3D transform)
			{
				iniSrc.getSourceTransform(t, level, transform);
			}

			@Override
			public UnsignedShortType getType() {
				return new UnsignedShortType();
			}

			@Override
			public String getName() {
				return iniSrc.getName();
			}

			@Override
			public VoxelDimensions getVoxelDimensions() {
				return iniSrc.getVoxelDimensions();
			}

			@Override
			public int getNumMipmapLevels() {
				return iniSrc.getNumMipmapLevels();
			}
		};

		return cvtSrc;
	}

	static public <T> Source<UnsignedShortType> convertSource(Source<T> iniSrc) {
		if (iniSrc.getType() instanceof UnsignedShortType)
			return (Source<UnsignedShortType>) iniSrc;
		if (iniSrc.getType() instanceof RealType) return convertRealSource(
			(Source<RealType>) iniSrc);
		logger.error("Cannot convert source to Unsigned Short Type, " + iniSrc
			.getType().getClass().getSimpleName() +
			" cannot be converted to RealType");
		return null;
	}
}
