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
package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class BrightnessAutoAdjuster< T extends RealType< T >> implements Runnable
{
	private final SourceAndConverter sac;
	private final double cumulativeMinCutoff;
	private final double cumulativeMaxCutoff;
	private final int timePoint;

	public BrightnessAutoAdjuster( final SourceAndConverter sac, int timePoint )
	{
		this(sac, timePoint, 0.01, 0.99 );
	}

	public BrightnessAutoAdjuster( final SourceAndConverter sac, final int timePoint, final double cumulativeMinCutoff, final double cumulativeMaxCutoff )
	{
		this.sac = sac;
		this.cumulativeMinCutoff = cumulativeMinCutoff;
		this.cumulativeMaxCutoff = cumulativeMaxCutoff;
		this.timePoint = timePoint;
	}

	@Override
	public void run()
	{
		if ( !sac.getSpimSource().isPresent( timePoint ) )
			return;
		if ( !(sac.getSpimSource().getType() instanceof RealType)){
			System.out.println("Can't auto adjust brightness of pixel type "+sac.getSpimSource().getType().getClass().getSimpleName());
			return;
		}

		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< T > img = ( RandomAccessibleInterval< T > ) sac.getSpimSource().getSource( timePoint, sac.getSpimSource().getNumMipmapLevels() - 1 );
		final long zMiddle = ( img.min( 2 ) + img.max( 2 ) + 1 ) / 2;

		final Iterable< T > sampledPixels = Views.iterable( Views.hyperSlice( img, 2, zMiddle ) );
		double minValue = Double.MAX_VALUE;
		double maxValue = -Double.MAX_VALUE;

		for ( T pixel : sampledPixels) {
			double val = pixel.getRealDouble();
			if (val<minValue) minValue = val;
			if (val>maxValue) maxValue = val;
		}

		final int numBins = 6535;
		final Histogram1d< T > histogram = new Histogram1d<>( sampledPixels, new Real1dBinMapper<T>(minValue, maxValue, numBins, false) );
		final DiscreteFrequencyDistribution dfd = histogram.dfd();
		final long[] bin = new long[] { 0 };
		double cumulative = 0;
		int i = 0;
		for ( ; (i < numBins) && (cumulative < cumulativeMinCutoff); ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}

		final double min = ((double)(i)/(double)numBins) * (maxValue - minValue) + minValue;

		for ( ; (i < numBins) && (cumulative < cumulativeMaxCutoff); ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		i+=1;
		final double max = ((double)i/(double)numBins) * (maxValue - minValue) + minValue;

		ConverterSetup converterSetup = SourceAndConverterServices.getSourceAndConverterService().getConverterSetup( sac );
		converterSetup.setDisplayRange(min, max);
	}

}
