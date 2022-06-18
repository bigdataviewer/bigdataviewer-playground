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

import bvv.util.BvvHandle;
import bdv.viewer.TimePointListener;
import bdv.viewer.TransformListener;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper.MatrixApproxEquals;

/**
 * BigDataViewer Playground Action --
 * Action which synchronizes the display location of 3 {@link BvvHandle}
 *
 * Works in combination with the action {@link ViewerTransformSyncStopper}
 *
 *
 * Principle : for every changed view transform of a specific BdvHandle,
 * the view transform change is triggered to the following BdvHandle in a closed loop manner
 *
 * Each transform is passed to next one by rolling the axes so that 3 swaps lead to an identical transform:
 *
 * // For ortho view : switches axis:
 * //  X, Y, Z (bdvHandle[0])
 * //  X,-Z, Y (bdvHandle[1]) Right View
 * // -Z, Y, X (bdvHandle[2]) Bottom View
 * <p>
 * This transformation chains maintains the center of the window in global coordinates constant
 * <p>
 * To avoid inifinite loop, the stop condition is : if the view transform is unnecessary (i.e.
 * the target viewTransform is approximately equal {@link ViewerTransformSyncStopper#MatrixApproxEquals}
 * to the source viewTransform), then there's no need to trigger a view transform change to the next BdvHandle
 *
 * See OrthoViewDemo for a usage example
 *
 * @author Nicolas Chiaruttini, BIOP EPFL, nicolas.chiaruttini@epfl.ch
 */

public class ViewerOrthoSyncStarter implements Runnable {

    /**
     * Array of BdvHandles to synchronize
     */
    final BvvHandle[] bvvHandles = new BvvHandle[3]; // Front Right Bottom

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
     * the synchronization, required in {@link ViewerTransformSyncStopper}
     */
    final Map<BvvHandle, TransformListener<AffineTransform3D>> bvvHandleToTransformListener = new HashMap<>();

    /**
     * Optional time synchronization between BdvHandles
     */
    final boolean synchronizeTime;

    /**
     * Map which links each BdvHandle to the TimePointListener which has been added
     * for synchronization purpose. This object contains all what's neede to stop
     * the synchronization, required in {@link ViewerTransformSyncStopper}
     */
    Map<BvvHandle, TimePointListener> bvvHandleToTimeListener = new HashMap<>();

    public ViewerOrthoSyncStarter(BvvHandle bvvHandleX, BvvHandle bvvHandleY, BvvHandle bvvHandleZ, boolean syncTime) {
        this.bvvHandles[0] = bvvHandleX;
        this.bvvHandles[1] = bvvHandleY;
        this.bvvHandles[2] = bvvHandleZ;
        this.synchronizeTime = syncTime;
    }

    public void setBvvHandleInitialReference(BvvHandle bvvHandle) {
        bvvHandleInitialReference = bvvHandle;
    }

    @Override
    public void run() {

        // Getting transform for initial sync
        AffineTransform3D at3Dorigin = getViewTransformForInitialSynchronization();

        // Building circularly linked listeners with stop condition when all transforms are equal,
        // cf javadoc
        // Building the TransformListener of currentBdvHandle
        TransformListener<AffineTransform3D> listener;


        // Adding this transform listener to the current BdvHandle
        listener = (at3D) -> propagateTransformIfNecessary(at3D,
                bvvHandles[0],
                bvvHandles[1],
                this::getRotatedView0);

        bvvHandles[0].getViewerPanel()
                .addTransformListener(listener);
        bvvHandleToTransformListener.put(bvvHandles[0], listener);

        listener = (at3D) -> propagateTransformIfNecessary(at3D,
                bvvHandles[1],
                bvvHandles[2],
                this::getRotatedView1);

        bvvHandles[1].getViewerPanel()
                .addTransformListener(listener);
        bvvHandleToTransformListener.put(bvvHandles[1], listener);

        listener = (at3D) -> propagateTransformIfNecessary(at3D,
                bvvHandles[2],
                bvvHandles[0],
                this::getRotatedView2);
        bvvHandles[2].getViewerPanel()
                .addTransformListener(listener);
        bvvHandleToTransformListener.put(bvvHandles[2], listener);


        if (synchronizeTime) {
            TimePointListener timeListener;
            timeListener = (timepoint) -> {
                if (bvvHandles[1].getViewerPanel().state().getCurrentTimepoint() != timepoint)
                    bvvHandles[1].getViewerPanel().setTimepoint(timepoint);
            };

            bvvHandles[0].getViewerPanel().addTimePointListener(timeListener);
            bvvHandleToTimeListener.put(bvvHandles[0], timeListener);

            timeListener = (timepoint) -> {
                if (bvvHandles[2].getViewerPanel().state().getCurrentTimepoint() != timepoint)
                    bvvHandles[2].getViewerPanel().setTimepoint(timepoint);
            };

            bvvHandles[1].getViewerPanel().addTimePointListener(timeListener);
            bvvHandleToTimeListener.put(bvvHandles[1], timeListener);


            timeListener = (timepoint) -> {
                if (bvvHandles[0].getViewerPanel().state().getCurrentTimepoint() != timepoint)
                    bvvHandles[0].getViewerPanel().setTimepoint(timepoint);
            };

            bvvHandles[2].getViewerPanel().addTimePointListener(timeListener);
            bvvHandleToTimeListener.put(bvvHandles[2], timeListener);
        }


        // Setting first transform for initial synchronization,
        // but only if the two necessary objects are present (the origin BvvHandle and the transform
        if ((bvvHandleInitialReference != null) && (at3Dorigin != null)) {
            for (BvvHandle bvvh : bvvHandles) {
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

    void propagateTransformIfNecessary(AffineTransform3D at3D, BvvHandle currentBvvHandle, BvvHandle nextBvvHandle, Function<double[], double[]> rotator) {

        // Is the transform necessary ? That's the stop condition

        double cur_wcx = currentBvvHandle.getViewerPanel().getWidth() / 2.0; // Current Window Center X
        double cur_wcy = currentBvvHandle.getViewerPanel().getHeight() / 2.0; // Current Window Center Y

        RealPoint centerScreenCurrentBvv = new RealPoint(cur_wcx, cur_wcy, 0);
        RealPoint centerScreenGlobalCoord = new RealPoint(3);

        at3D.inverse().apply(centerScreenCurrentBvv, centerScreenGlobalCoord);

        // Now compute what should be the matrix in the next BVV frame:
        AffineTransform3D nextAffineTransform = new AffineTransform3D();

        // It should have the same scaling and rotation than the current view
        nextAffineTransform.set(at3D);

        // No Shift
        nextAffineTransform.set(0, 0, 3);
        nextAffineTransform.set(0, 1, 3);
        nextAffineTransform.set(0, 2, 3);

        // Rotates axis according to rotator
        nextAffineTransform.set(rotator.apply(nextAffineTransform.getRowPackedCopy()));

        // But the center of the window should be centerScreenGlobalCoord
        // Let's compute the shift
        double next_wcx = nextBvvHandle.getViewerPanel().getWidth() / 2.0; // Next Window Center X
        double next_wcy = nextBvvHandle.getViewerPanel().getHeight() / 2.0; // Next Window Center Y

        RealPoint centerScreenNextBvv = new RealPoint(next_wcx, next_wcy, 0);
        RealPoint shiftNextBvv = new RealPoint(3);

        nextAffineTransform.inverse().apply(centerScreenNextBvv, shiftNextBvv);

        double sx = -centerScreenGlobalCoord.getDoublePosition(0) + shiftNextBvv.getDoublePosition(0);
        double sy = -centerScreenGlobalCoord.getDoublePosition(1) + shiftNextBvv.getDoublePosition(1);
        double sz = -centerScreenGlobalCoord.getDoublePosition(2) + shiftNextBvv.getDoublePosition(2);

        RealPoint shiftWindow = new RealPoint(sx, sy, sz);
        RealPoint shiftMatrix = new RealPoint(3);
        nextAffineTransform.apply(shiftWindow, shiftMatrix);

        nextAffineTransform.set(shiftMatrix.getDoublePosition(0), 0, 3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(1), 1, 3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(2), 2, 3);

        // Is the transform necessary ? That's the stop condition
        AffineTransform3D ati = new AffineTransform3D();
        nextBvvHandle.getViewerPanel().state().getViewerTransform(ati);

        if (!MatrixApproxEquals(nextAffineTransform.getRowPackedCopy(), ati.getRowPackedCopy())) {
            // Yes -> triggers a transform change to the nextBvvHandle
            // For ortho view : switches axis:
            // X --> Y
            // Y --> Z
            // Z --> X
            // Calling it three times leads to an identical transform, hence the stopping condition is triggered
            AffineTransform3D nextAt3D = nextAffineTransform.copy();
            nextAt3D.set(nextAffineTransform.getRowPackedCopy());
            nextBvvHandle.getViewerPanel().state().setViewerTransform(nextAt3D);
            nextBvvHandle.getViewerPanel().requestRepaint();
        }

    }


    public double[] getRotatedView0(double[] m) {
        return new double[]{
                m[0], m[1], m[2],
                m[3],
                -m[8], -m[9], -m[10],
                m[7],
                m[4], m[5], m[6],
                m[11],

        };
    }

    public double[] getRotatedView1(double[] m) {
        return new double[]{

                m[4], m[5], m[6],
                m[3],
                m[8], m[9], m[10],
                m[7],
                m[0], m[1], m[2],
                m[11],

        };
    }

    public double[] getRotatedView2(double[] m) {
        return new double[]{
                m[8], m[9], m[10],
                m[3],
                m[4], m[5], m[6],
                m[7],
                -m[0], -m[1], -m[2],
                m[11],
        };
    }

    /**
     * A simple search to identify the view transform of the BvvHandle that will be used
     * for the initial synchronization (first reference)
     *
     * @return the view transform of the BvvHandle that will be used for the initial synchronization
     */
    private AffineTransform3D getViewTransformForInitialSynchronization() {
        AffineTransform3D at3Dorigin = null;
        for (BvvHandle bvvHandle : bvvHandles) {
            // if the BvvHandle is the one that should be used for initial synchronization
            if (bvvHandle.equals(bvvHandleInitialReference)) {
                // Storing the transform that will be used for first synchronization
                at3Dorigin = new AffineTransform3D();
                bvvHandle.getViewerPanel().state().getViewerTransform(at3Dorigin);
            }
        }
        return at3Dorigin;
    }

    /**
     * output of this action : this map can be used to stop the synchronization
     * see {@link ViewerTransformSyncStopper}
     *
     * @return map of {@link TransformListener} which can be used to stop the synchronization
     */
    public Map<BvvHandle, TransformListener<AffineTransform3D>> getSynchronizers() {
        return bvvHandleToTransformListener;
    }

    public boolean isSynchronizingTime() {
        return synchronizeTime;
    }

    /**
     * output of this action : this map can be used to stop the synchronization
     * see {@link ViewerTransformSyncStopper}
     *
     * @return map of {@link TimePointListener} which can be used to stop the synchronization
     */
    public Map<BvvHandle, TimePointListener> getTimeSynchronizers() {
        return bvvHandleToTimeListener;
    }
}
