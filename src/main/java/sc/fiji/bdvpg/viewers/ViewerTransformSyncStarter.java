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
package sc.fiji.bdvpg.viewers;

import bdv.util.BdvHandle;
import bdv.viewer.TimePointListener;
import bdv.viewer.TransformListener;
import bvv.util.BvvHandle;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.HashMap;
import java.util.Map;

import static sc.fiji.bdvpg.viewers.ViewerOrthoSyncStarter.MatrixApproxEquals;


/**
 * BigDataViewer Playground Action --
 * Action which synchronizes the display location of a {@link BvvHandle}
 *
 * Works in combination with the action {@link ViewerTransformSyncStopper}
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
 * (i.e. the view target is approximately equal to the source
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
    ViewerAdapter[] handles;

    /**
     * Reference to the BdvHandle which will serve as a reference for the
     * first synchronization. Most of the time this has to be the BdvHandle
     * currently used by the user. If not set, the first synchronization
     * will look like it's a random BdvHandle which is used (one not in focus)
     */
    ViewerAdapter handleInitialReference = null;

    /**
     * Map which links each BdvHandle to the TransformListener which has been added
     * for synchronization purpose. This object contains all what's needed to stop
     * the synchronization
     */
    Map<ViewerAdapter, TransformListener<AffineTransform3D>> handleToTransformListener = new HashMap<>();


    /** Optional time synchronization
     *
     */
    boolean synchronizeTime;

    /**
     * Map which links each BdvHandle to the TimePointListener which has been added
     * for synchronization purpose. This object contains all what's neede to stop
     * the synchronization
     */
    Map<ViewerAdapter, TimePointListener> handleToTimeListener = new HashMap<>();

    public ViewerTransformSyncStarter(BdvHandle[] bdvHandles, boolean synchronizeTime) {
        this.handles = new ViewerAdapter[bdvHandles.length];
        for (int i = 0;i< bdvHandles.length;i++) {
            handles[i] = new ViewerAdapter(bdvHandles[i]);
        }
        this.synchronizeTime = synchronizeTime;
    }

    public ViewerTransformSyncStarter(BvvHandle[] bvvHandles, boolean synchronizeTime) {
        this.handles = new ViewerAdapter[bvvHandles.length];
        for (int i = 0;i< bvvHandles.length;i++) {
            handles[i] = new ViewerAdapter(bvvHandles[i]);
        }
        this.synchronizeTime = synchronizeTime;
    }

    public ViewerTransformSyncStarter(ViewerAdapter[] handles, boolean synchronizeTime) {
       this.handles = handles;
       this.synchronizeTime = synchronizeTime;
    }

    public void setHandleInitialReference(ViewerAdapter handle) {
        handleInitialReference = handle;
    }

    @Override
    public void run() {

        // Getting transform for initial sync
        AffineTransform3D at3Dorigin = getViewTransformForInitialSynchronization();

        // Building circularly linked listeners with stop condition when all transforms are equal,
        // cf documentation

        for (int i = 0; i< handles.length; i++) {

            // The idea is that bdvHandles[i], when it has a view transform,
            // triggers an identical ViewTransform to the next bdvHandle in the array
            // (called nextBdvHandle). nextBdvHandle is bdvHandles[i+1] in most cases,
            // unless it's the end of the array,
            // where in this case nextBdvHandle is bdvHandles[0]
            ViewerAdapter currentHandle = handles[i];
            ViewerAdapter nextHandle;

            // Identifying nextBdvHandle
            if (i == handles.length-1) {
                nextHandle = handles[0];
            } else {
                nextHandle = handles[i+1];
            }

            // Building the TransformListener of currentBvvHandle
            TransformListener<AffineTransform3D> listener = (at3D) -> propagateTransformIfNecessary(at3D, currentHandle, nextHandle);

            // Adding this transform listener to the currenBdvHandle
            currentHandle.addTransformListener(listener);

            // Storing the transform listener -> needed to remove them in order to stop synchronization when needed
            handleToTransformListener.put(handles[i], listener);

            if (synchronizeTime) {
                TimePointListener timeListener = (timepoint) -> {
                    if (nextHandle.state().getCurrentTimepoint()!=timepoint)
                        nextHandle.setTimepoint(timepoint);
                };

                currentHandle.addTimePointListener(timeListener);
                handleToTimeListener.put(handles[i], timeListener);
            }
        }

        // Setting first transform for initial synchronization,
        // but only if the two necessary objects are present (the origin BdvHandle and the transform
         if ((handleInitialReference !=null)&&(at3Dorigin!=null)) {
             for (ViewerAdapter handle: handles) {
                 handle.state().setViewerTransform(at3Dorigin.copy());
                 handle.requestRepaint();
                 if (synchronizeTime) {
                     handle.state().setCurrentTimepoint(
                             handleInitialReference.state().getCurrentTimepoint()
                     );
                 }
             }
         }
    }

    void propagateTransformIfNecessary(AffineTransform3D at3D, ViewerAdapter currentHandle, ViewerAdapter nextHandle) {
        // We need to transfer the transform - but while keeping the center of the window constant
        double cur_wcx = currentHandle.getWidth()/2.0; // Current Window Center X
        double cur_wcy = currentHandle.getHeight()/2.0; // Current Window Center Y

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
        double next_wcx = nextHandle.getWidth()/2.0; // Next Window Center X
        double next_wcy = nextHandle.getHeight()/2.0; // Next Window Center Y

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
        nextHandle.state().getViewerTransform(ati);

        if (!MatrixApproxEquals(nextAffineTransform.getRowPackedCopy(), ati.getRowPackedCopy())) {
            // Yes -> triggers a transform change to the nextBdvHandle
            // Calling it three times leads to an identical transform, hence the stopping condition is triggered
            AffineTransform3D nextAt3D = nextAffineTransform.copy();
            nextAt3D.set(nextAffineTransform.getRowPackedCopy());
            nextHandle.state().setViewerTransform(nextAt3D);
            nextHandle.requestRepaint();
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
        for (ViewerAdapter handle : handles) {
            // if the BdvHandle is the one that should be used for initial synchronization
            if (handle.equals(handleInitialReference)) {
                // Storing the transform that will be used for first synchronization
                at3Dorigin = new AffineTransform3D();
                handle.state().getViewerTransform(at3Dorigin);
            }
        }
        return at3Dorigin;
    }

    /**
     * @return map which can be used to stop the spatial synchronization see {@link ViewerTransformSyncStopper}
     */
    public Map<ViewerAdapter, TransformListener<AffineTransform3D>> getSynchronizers() {
        return handleToTransformListener;
    }

    public boolean isSynchronizingTime() {
        return synchronizeTime;
    }

    /**
     * @return map which can be used to stop the time synchronization see {@link ViewerTransformSyncStopper}
     */
    public Map<ViewerAdapter, TimePointListener> getTimeSynchronizers() {
        return handleToTimeListener;
    }
}
