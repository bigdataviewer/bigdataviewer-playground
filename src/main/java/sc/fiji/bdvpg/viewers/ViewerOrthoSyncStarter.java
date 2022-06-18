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

import bdv.viewer.TimePointListener;
import bdv.viewer.TransformListener;
import bvv.util.BvvHandle;
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
    final ViewerAdapter[] handles = new ViewerAdapter[3]; // Front Right Bottom

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
     * the synchronization, required in {@link ViewerTransformSyncStopper}
     */
    final Map<ViewerAdapter, TransformListener<AffineTransform3D>> handleToTransformListener = new HashMap<>();

    /**
     * Optional time synchronization between BdvHandles
     */
    final boolean synchronizeTime;

    /**
     * Map which links each BdvHandle to the TimePointListener which has been added
     * for synchronization purpose. This object contains all what's neede to stop
     * the synchronization, required in {@link ViewerTransformSyncStopper}
     */
    Map<ViewerAdapter, TimePointListener> handleToTimeListener = new HashMap<>();

    public ViewerOrthoSyncStarter(ViewerAdapter handleX, ViewerAdapter handleY, ViewerAdapter handleZ, boolean syncTime) {
        this.handles[0] = handleX;
        this.handles[1] = handleY;
        this.handles[2] = handleZ;
        this.synchronizeTime = syncTime;
    }

    public void setHandleInitialReference(ViewerAdapter handle) {
        handleInitialReference = handle;
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
                handles[0],
                handles[1],
                this::getRotatedView0);

        handles[0].addTransformListener(listener);
        handleToTransformListener.put(handles[0], listener);

        listener = (at3D) -> propagateTransformIfNecessary(at3D,
                handles[1],
                handles[2],
                this::getRotatedView1);

        handles[1].addTransformListener(listener);
        handleToTransformListener.put(handles[1], listener);

        listener = (at3D) -> propagateTransformIfNecessary(at3D,
                handles[2],
                handles[0],
                this::getRotatedView2);
        handles[2].addTransformListener(listener);
        handleToTransformListener.put(handles[2], listener);

        if (synchronizeTime) {
            TimePointListener timeListener;
            timeListener = (timepoint) -> {
                if (handles[1].state().getCurrentTimepoint() != timepoint)
                    handles[1].setTimepoint(timepoint);
            };

            handles[0].addTimePointListener(timeListener);
            handleToTimeListener.put(handles[0], timeListener);

            timeListener = (timepoint) -> {
                if (handles[2].state().getCurrentTimepoint() != timepoint)
                    handles[2].setTimepoint(timepoint);
            };

            handles[1].addTimePointListener(timeListener);
            handleToTimeListener.put(handles[1], timeListener);


            timeListener = (timepoint) -> {
                if (handles[0].state().getCurrentTimepoint() != timepoint)
                    handles[0].setTimepoint(timepoint);
            };

            handles[2].addTimePointListener(timeListener);
            handleToTimeListener.put(handles[2], timeListener);
        }


        // Setting first transform for initial synchronization,
        // but only if the two necessary objects are present (the origin BvvHandle and the transform
        if ((handleInitialReference != null) && (at3Dorigin != null)) {
            for (ViewerAdapter handle : handles) {
                handle.state().setViewerTransform(at3Dorigin.copy());
                handle.requestRepaint();
                if (synchronizeTime) {
                    handle.state().setCurrentTimepoint(
                            handle.state().getCurrentTimepoint()
                    );
                }
            }
        }
    }

    void propagateTransformIfNecessary(AffineTransform3D at3D, ViewerAdapter currentHandle, ViewerAdapter nextHandle, Function<double[], double[]> rotator) {

        // Is the transform necessary ? That's the stop condition

        double cur_wcx = currentHandle.getWidth() / 2.0; // Current Window Center X
        double cur_wcy = currentHandle.getHeight() / 2.0; // Current Window Center Y

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
        double next_wcx = nextHandle.getWidth() / 2.0; // Next Window Center X
        double next_wcy = nextHandle.getHeight() / 2.0; // Next Window Center Y

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
        nextHandle.state().getViewerTransform(ati);

        if (!MatrixApproxEquals(nextAffineTransform.getRowPackedCopy(), ati.getRowPackedCopy())) {
            // Yes -> triggers a transform change to the nextBvvHandle
            // For ortho view : switches axis:
            // X --> Y
            // Y --> Z
            // Z --> X
            // Calling it three times leads to an identical transform, hence the stopping condition is triggered
            AffineTransform3D nextAt3D = nextAffineTransform.copy();
            nextAt3D.set(nextAffineTransform.getRowPackedCopy());
            nextHandle.state().setViewerTransform(nextAt3D);
            nextHandle.requestRepaint();
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
        for (ViewerAdapter handle : handles) {
            // if the BvvHandle is the one that should be used for initial synchronization
            if (handle.equals(handleInitialReference)) {
                // Storing the transform that will be used for first synchronization
                at3Dorigin = new AffineTransform3D();
                handle.state().getViewerTransform(at3Dorigin);
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
    public Map<ViewerAdapter, TransformListener<AffineTransform3D>> getSynchronizers() {
        return handleToTransformListener;
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
    public Map<ViewerAdapter, TimePointListener> getTimeSynchronizers() {
        return handleToTimeListener;
    }
}
