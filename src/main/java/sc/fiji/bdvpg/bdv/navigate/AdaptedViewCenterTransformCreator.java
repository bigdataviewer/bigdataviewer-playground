/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import net.imglib2.realtransform.AffineTransform3D;

public class AdaptedViewCenterTransformCreator
{
	private final BdvHandle bdvHandle;
	private final double[] xyz;
	private final int t;

	/**
	 * Based on the current ViewerTransform, creates a new viewer transform with a new center position.
	 *
	 * @param bdvHandle
	 * @param xyz
	 * 			target coordinates in physical units
	 * @param t
	 * 			time point
	 */
	public AdaptedViewCenterTransformCreator( BdvHandle bdvHandle, double[] xyz, int t )
	{
		this.bdvHandle = bdvHandle;
		this.xyz = xyz;
		this.t = t;
	}

	private AffineTransform3D createAdaptedTransform()
	{
		bdvHandle.getViewerPanel().setTimepoint( t );

		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform( currentViewerTransform );

		AffineTransform3D adaptedViewerTransform = currentViewerTransform.copy();

		// ViewerTransform notes:
		// - applyInverse: coordinates in viewer => coordinates in image
		// - apply: coordinates in image => coordinates in viewer

		final double[] targetPositionInViewerInPixels = new double[ 3 ];
		currentViewerTransform.apply( xyz, targetPositionInViewerInPixels );

		for ( int d = 0; d < 3; d++ )
		{
			targetPositionInViewerInPixels[ d ] *= -1;
		}

		adaptedViewerTransform.translate( targetPositionInViewerInPixels );

		final double[] windowCentreInViewerInPixels = new double[ 3 ];
		windowCentreInViewerInPixels[ 0 ] = bdvHandle.getViewerPanel().getDisplay().getWidth() / 2.0;
		windowCentreInViewerInPixels[ 1 ] = bdvHandle.getViewerPanel().getDisplay().getHeight() / 2.0;

		adaptedViewerTransform.translate( windowCentreInViewerInPixels );

		return adaptedViewerTransform;
	}

	public AffineTransform3D getAdaptedCenterTransform()
	{
		AffineTransform3D adaptedTransform = createAdaptedTransform();
		return adaptedTransform;
	}
}
