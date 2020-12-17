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
import bdv.viewer.TimePointListener;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.TransformListener;
import java.util.Map;

/**
 * BigDataViewer Playground Action --
 * Action which stops the synchronization of the display location of a {@link BdvHandle}
 * Works in combination with the action {@link ViewerTransformSyncStarter}
 * and {@link ViewerOrthoSyncStarter}
 *
 * See ViewTransformSynchronizationDemo for a usage example
 *
 * @author Nicolas Chiaruttini, BIOP EPFL, nicolas.chiaruttini@epfl.ch
 */

public class ViewerTransformSyncStopper implements Runnable {

    Map<BdvHandle, TransformListener<AffineTransform3D>> bdvHandleToTransformListener;

    Map<BdvHandle, TimePointListener> bdvHandleToTimePointListener;

    public ViewerTransformSyncStopper(
            Map<BdvHandle,TransformListener<AffineTransform3D>> bdvHandleToTransformListener,
            Map<BdvHandle, TimePointListener> bdvHandleToTimePointListener) {
       this.bdvHandleToTransformListener = bdvHandleToTransformListener;
       this.bdvHandleToTimePointListener = bdvHandleToTimePointListener;
    }

    @Override
    public void run() {
        bdvHandleToTransformListener.forEach((bdvHandle, listener) ->
            bdvHandle.getViewerPanel().removeTransformListener(listener)
        );
        if (bdvHandleToTimePointListener!=null) {
            bdvHandleToTimePointListener.forEach((bdvHandle, listener) ->
                bdvHandle.getViewerPanel().removeTimePointListener(listener)
            );
        }
    }

    /**
     * Tests whether two arrays of double are approximately equal
     * Used internally with {@link AffineTransform3D#getRowPackedCopy()}
     * To test if two matrices are approximately equal
     *
     * @param m1 first matrix of double
     * @param m2 second matrix of double
     * @return if these matrix are approx equal
     */
    public static boolean MatrixApproxEquals(double[] m1, double[] m2) {
        assert m1.length == m2.length;
        boolean ans = true;
        for (int i=0;i<m1.length;i++) {
            if (Math.abs(m1[i]-m2[i])>1e6*Math.ulp(Math.min(Math.abs(m1[i]), Math.abs(m2[i])))) {
                ans = false;
                break;
            }
        }
        return ans;
    }

}
