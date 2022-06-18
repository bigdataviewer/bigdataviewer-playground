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
package sc.fiji.bdvpg.bvv.navigate;

import bdv.viewer.TimePointListener;
import bdv.viewer.TransformListener;
import bvv.util.BvvHandle;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.navigate.ViewerOrthoSyncStarter;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper;

import java.util.HashMap;
import java.util.Map;

import static sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper.MatrixApproxEquals;

/**
 * BigDataViewer Playground Action --
 * Action which synchronizes the display location of a {@link BvvHandle}
 *
 * Works in combination with the action {@link sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper}
 *
 * See also ViewTransformSynchronizationDemo
 *
 * Principle : for every changed view transform of a specific BdvHandle,
 * the view transform change is passed to the following BdvHandle in a closed loop manner
 *
 * Note : the center of the window in global coordinate is kept identical between BdvHandles
 *
 * Note : closing one window in the chain breaks the synchronization TODO make more robust
 *
 * To avoid infinite loop, the stop condition is : if the view transform is unnecessary
 * (i.e. the view target is approximately equal to the source {@link sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper#MatrixApproxEquals}),
 * then there's no need to trigger a view transform change to the next BdvHandle
 *
 * see also {@link ViewerOrthoSyncStarter}
 *
 * See ViewTransformSynchronizationDemo for a usage example
 *
 * @author Nicolas Chiaruttini, BIOP EPFL, nicolas.chiaruttini@epfl.ch
 */

public class ViewerTransformSyncStarter implements Runnable {

    /**
     * Array of BdvHandles to synchronize
     */
    BvvHandle[] bvvHandles;

    /**
     * Reference to the BdvHandle which will serve as a reference for the
     * first synchronization. Most of the time this has to be the BdvHandle
     * currently used by the user. If not set, the first synchronization
     * will look like it's a random BdvHandle which is used (one not in focus)
     */
    BvvHandle bvvHandleInitialReference = null;

    /**
     * Map which links each BdvHandle to the TransformListener which has been added
     * for synchronization purpose. This object contains all what's needed to stop
     * the synchronization
     */
    Map<BvvHandle, TransformListener<AffineTransform3D>> bvvHandleToTransformListener = new HashMap<>();


    /** Optional time synchronization
     *
     */
    boolean synchronizeTime;

    /**
     * Map which links each BdvHandle to the TimePointListener which has been added
     * for synchronization purpose. This object contains all what's neede to stop
     * the synchronization
     */
    Map<BvvHandle, TimePointListener> bdvHandleToTimeListener = new HashMap<>();


    public ViewerTransformSyncStarter(BvvHandle[] bdvHandles, boolean synchronizeTime) {
       this.bvvHandles = bdvHandles;
       this.synchronizeTime = synchronizeTime;
    }

    public void setBvvHandleInitialReference(BvvHandle bdvHandle) {
        bvvHandleInitialReference = bdvHandle;
    }

    @Override
    public void run() {

        // Getting transform for initial sync
        AffineTransform3D at3Dorigin = getViewTransformForInitialSynchronization();

        // Building circularly linked listeners with stop condition when all transforms are equal,
        // cf documentation

        for (int i = 0; i< bvvHandles.length; i++) {

            // The idea is that bdvHandles[i], when it has a view transform,
            // triggers an identical ViewTransform to the next bdvHandle in the array
            // (called nextBdvHandle). nextBdvHandle is bdvHandles[i+1] in most cases,
            // unless it's the end of the array,
            // where in this case nextBdvHandle is bdvHandles[0]
            BvvHandle currentBvvHandle = bvvHandles[i];
            BvvHandle nextBvvHandle;

            // Identifying nextBdvHandle
            if (i == bvvHandles.length-1) {
                nextBvvHandle = bvvHandles[0];
            } else {
                nextBvvHandle = bvvHandles[i+1];
            }

            // Building the TransformListener of currentBvvHandle
            TransformListener<AffineTransform3D> listener = (at3D) -> propagateTransformIfNecessary(at3D, currentBvvHandle, nextBvvHandle);

            // Adding this transform listener to the currenBdvHandle
            currentBvvHandle.getViewerPanel().addTransformListener(listener);

            // Storing the transform listener -> needed to remove them in order to stop synchronization when needed
            bvvHandleToTransformListener.put(bvvHandles[i], listener);

            if (synchronizeTime) {
                TimePointListener timeListener = (timepoint) -> {
                    if (nextBvvHandle.getViewerPanel().state().getCurrentTimepoint()!=timepoint)
                        nextBvvHandle.getViewerPanel().setTimepoint(timepoint);
                };

                currentBvvHandle.getViewerPanel().addTimePointListener(timeListener);
                bdvHandleToTimeListener.put(bvvHandles[i], timeListener);
            }
        }

        // Setting first transform for initial synchronization,
        // but only if the two necessary objects are present (the origin BdvHandle and the transform
         if ((bvvHandleInitialReference !=null)&&(at3Dorigin!=null)) {
             for (BvvHandle bvvh: bvvHandles) {
                 bvvh.getViewerPanel().state().setViewerTransform(at3Dorigin.copy());
                 bvvh.getViewerPanel().requestRepaint();
                 if (synchronizeTime) {
                     bvvh.getViewerPanel().state().setCurrentTimepoint(
                             bvvHandleInitialReference.getViewerPanel().state().getCurrentTimepoint()
                     );
                 }
             }
         }
    }

    void propagateTransformIfNecessary(AffineTransform3D at3D, BvvHandle currentBvvHandle, BvvHandle nextBvvHandle) {
        // We need to transfer the transform - but while keeping the center of the window constant
        double cur_wcx = currentBvvHandle.getViewerPanel().getWidth()/2.0; // Current Window Center X
        double cur_wcy = currentBvvHandle.getViewerPanel().getHeight()/2.0; // Current Window Center Y

        RealPoint centerScreenCurrentBdv = new RealPoint(cur_wcx, cur_wcy, 0);
        RealPoint centerScreenGlobalCoord = new RealPoint(3);

        at3D.inverse().apply(centerScreenCurrentBdv, centerScreenGlobalCoord);

        // Now compute what should be the matrix in the next BDV frame:
        AffineTransform3D nextAffineTransform = new AffineTransform3D();

        // It should have the same scaling and rotation than the current view
        nextAffineTransform.set(at3D);

        // No Shift
        nextAffineTransform.set(0,0,3);
        nextAffineTransform.set(0,1,3);
        nextAffineTransform.set(0,2,3);

        // But the center of the window should be centerScreenGlobalCoord
        // Let's compute the shift
        double next_wcx = nextBvvHandle.getViewerPanel().getWidth()/2.0; // Next Window Center X
        double next_wcy = nextBvvHandle.getViewerPanel().getHeight()/2.0; // Next Window Center Y

        RealPoint centerScreenNextBdv = new RealPoint(next_wcx, next_wcy, 0);
        RealPoint shiftNextBdv = new RealPoint(3);

        nextAffineTransform.inverse().apply(centerScreenNextBdv, shiftNextBdv);

        double sx = -centerScreenGlobalCoord.getDoublePosition(0)+shiftNextBdv.getDoublePosition(0);
        double sy = -centerScreenGlobalCoord.getDoublePosition(1)+shiftNextBdv.getDoublePosition(1);
        double sz = -centerScreenGlobalCoord.getDoublePosition(2)+shiftNextBdv.getDoublePosition(2);

        RealPoint shiftWindow = new RealPoint(sx, sy, sz);
        RealPoint shiftMatrix = new RealPoint(3);
        nextAffineTransform.apply(shiftWindow, shiftMatrix);

        nextAffineTransform.set(shiftMatrix.getDoublePosition(0),0,3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(1),1,3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(2),2,3);

        // Is the transform necessary ? That's the stop condition
        AffineTransform3D ati = new AffineTransform3D();
        nextBvvHandle.getViewerPanel().state().getViewerTransform(ati);

        if (!MatrixApproxEquals(nextAffineTransform.getRowPackedCopy(), ati.getRowPackedCopy())) {
            // Yes -> triggers a transform change to the nextBdvHandle
            // Calling it three times leads to an identical transform, hence the stopping condition is triggered
            AffineTransform3D nextAt3D = nextAffineTransform.copy();
            nextAt3D.set(nextAffineTransform.getRowPackedCopy());
            nextBvvHandle.getViewerPanel().state().setViewerTransform(nextAt3D);
            nextBvvHandle.getViewerPanel().requestRepaint();
        }
    }

    /**
     * A simple search to identify the view transform of the BdvHandle that will be used
     * for the initial synchronization (first reference)
     * @return the view transform of the BdvHandle that will be used
     *      for the initial synchronization (first reference)
     */
    private AffineTransform3D getViewTransformForInitialSynchronization() {
        AffineTransform3D at3Dorigin = null;
        for (BvvHandle bvvHandle : bvvHandles) {
            // if the BdvHandle is the one that should be used for initial synchronization
            if (bvvHandle.equals(bvvHandleInitialReference)) {
                // Storing the transform that will be used for first synchronization
                at3Dorigin = new AffineTransform3D();
                bvvHandle.getViewerPanel().state().getViewerTransform(at3Dorigin);
            }
        }
        return at3Dorigin;
    }

    /**
     * @return map which can be used to stop the spatial synchronization see {@link sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper}
     */
    public Map<BvvHandle, TransformListener<AffineTransform3D>> getSynchronizers() {
        return bvvHandleToTransformListener;
    }

    public boolean isSynchronizingTime() {
        return synchronizeTime;
    }

    /**
     * @return map which can be used to stop the time synchronization see {@link ViewerTransformSyncStopper}
     */
    public Map<BvvHandle, TimePointListener> getTimeSynchronizers() {
        return bdvHandleToTimeListener;
    }
}
