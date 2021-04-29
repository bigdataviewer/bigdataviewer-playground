/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.bdv.projector;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 * Default sum projector
 *
 * TODO : is this projector necessary ?
 *
 */

public class AccumulateSumProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{

	public AccumulateSumProjectorARGB(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< SourceAndConverter< ? > > sources,
			final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		super( sourceProjectors, sourceScreenImages, target, numThreads, executorService );
	}

	@Override
	protected void accumulate(
			final Cursor< ? extends ARGBType >[] accesses,
			final ARGBType target )
	{
		final int argbIndex = getArgbIndex( accesses );

		target.set( argbIndex );
	}

	public static int getArgbIndex( Cursor< ? extends ARGBType >[] accesses )
	{
		int aAccu = 0, rAccu = 0, gAccu = 0, bAccu = 0;

		for ( final Cursor< ? extends ARGBType > access : accesses )
		{
			final int value = access.get().get();
			final int a = ARGBType.alpha( value );
			final int r = ARGBType.red( value );
			final int g = ARGBType.green( value );
			final int b = ARGBType.blue( value );

			if ( a == 0 )
			{
				continue;
			}

			aAccu += a;
			rAccu += r;
			gAccu += g;
			bAccu += b;
		}

		if ( aAccu > 255 )
			aAccu = 255;
		if ( rAccu > 255 )
			rAccu = 255;
		if ( gAccu > 255 )
			gAccu = 255;
		if ( bAccu > 255 )
			bAccu = 255;

		return ARGBType.rgba( rAccu, gAccu, bAccu, aAccu );
	}

}
