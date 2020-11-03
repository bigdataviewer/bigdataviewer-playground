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
import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * BigDataViewer Playground Action --
 * Action which concatenates the current viewTransform
 * of a {@link BdvHandle} with the input {@link AffineTransform3D}
 *
 * See ViewTransformSetAndLogDemo for a usage example
 *
 * @author haesleinhuepf, tischi
 * 12 2019
 */

public class ViewerTransformChanger implements Runnable {

    private final BdvHandle bdvHandle;
    private AffineTransform3D transform;
    private final int animationDurationMillis;
    private final boolean concatenateToCurrentTransform;

    public ViewerTransformChanger( BdvHandle bdvHandle, AffineTransform3D transform, boolean concatenateToCurrentTransform, int animationDurationMillis ) {
        this.bdvHandle = bdvHandle;
        this.transform = transform;
        this.concatenateToCurrentTransform = concatenateToCurrentTransform;
        this.animationDurationMillis = animationDurationMillis;
    }

    @Override
    public void run() {

        if ( concatenateToCurrentTransform )
        {
            AffineTransform3D view = new AffineTransform3D();
            bdvHandle.getViewerPanel().state().getViewerTransform( view );
            transform = view.concatenate( transform );
        }

        if ( animationDurationMillis <= 0 )
        {
            bdvHandle.getViewerPanel().state().setViewerTransform( transform );
            return;
        }
        else
        {
            final AffineTransform3D currentViewerTransform = new AffineTransform3D();
            bdvHandle.getViewerPanel().state().getViewerTransform( currentViewerTransform );

            final SimilarityTransformAnimator similarityTransformAnimator =
                    new SimilarityTransformAnimator(
                            currentViewerTransform,
                            transform,
                            0, 0,  // TODO: understand what this does
                            animationDurationMillis  );

            bdvHandle.getViewerPanel().setTransformAnimator( similarityTransformAnimator );
            //bdvHandle.getViewerPanel().( currentViewerTransform );
        }
    }
}
