package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.viewer.TimePointListener;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.TransformListener;//net.imglib2.ui.TransformListener;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper.MatrixApproxEquals;

/**
 * BigDataViewer Playground Action -->
 * Action which synchronizes the display location of 3 {@link BdvHandle}
 * <p>
 * Works in combination with the action {@link ViewerTransformSyncStopper}
 * <p>
 * <p>
 * Principle : for every changed view transform of a specific BdvHandle,
 * the view transform change is triggered to the following BdvHandle in a closed loop manner
 * <p>
 * Each transform is passed to next one by rolling the axes so that 3 swaps lead to an identical transform:
 * <p>
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
 * See {@link TODO ViewOrthoSynchronizationDemo} for a usage example
 *
 * @author Nicolas Chiaruttini, BIOP EPFL, nicolas.chiaruttini@epfl.ch
 */

public class ViewerOrthoSyncStarter implements Runnable {

    /**
     * Array of BdvHandles to synchronize
     */
    final BdvHandle[] bdvHandles = new BdvHandle[3]; // Front Right Bottom

    /**
     * Reference to the BdvHandle which will serve as a reference for the
     * first synchronization. Most of the time this has to be the BdvHandle
     * currently used by the user. If not set, the first synchronization
     * will look like it's a random BdvHandle which is used (one not in focus)
     */
    BdvHandle bdvHandleInitialReference = null;

    /**
     * Map which links each BdvHandle to the TransformListener which has been added
     * for synchronization purpose. This object contains all what's needed to stop
     * the synchronization, required in {@link ViewerTransformSyncStopper}
     */
    final Map<BdvHandle, TransformListener<AffineTransform3D>> bdvHandleToTransformListener = new HashMap<>();

    /**
     * Optional time synchronization between BdvHandles
     */
    final boolean synchronizeTime;

    /**
     * Map which links each BdvHandle to the TimePointListener which has been added
     * for synchronization purpose. This object contains all what's neede to stop
     * the synchronization, required in {@link ViewerTransformSyncStopper}
     */
    Map<BdvHandle, TimePointListener> bdvHandleToTimeListener = new HashMap<>();

    public ViewerOrthoSyncStarter(BdvHandle bdvHandleX, BdvHandle bdvHandleY, BdvHandle bdvHandleZ, boolean syncTime) {
        this.bdvHandles[0] = bdvHandleX;
        this.bdvHandles[1] = bdvHandleY;
        this.bdvHandles[2] = bdvHandleZ;
        this.synchronizeTime = syncTime;
    }

    public void setBdvHandleInitialReference(BdvHandle bdvHandle) {
        bdvHandleInitialReference = bdvHandle;
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
                bdvHandles[0],
                bdvHandles[1],
                this::getRotatedView0);

        bdvHandles[0].getViewerPanel()
                .addTransformListener(listener);
        bdvHandleToTransformListener.put(bdvHandles[0], listener);

        listener = (at3D) -> propagateTransformIfNecessary(at3D,
                bdvHandles[1],
                bdvHandles[2],
                this::getRotatedView1);

        bdvHandles[1].getViewerPanel()
                .addTransformListener(listener);
        bdvHandleToTransformListener.put(bdvHandles[1], listener);

        listener = (at3D) -> propagateTransformIfNecessary(at3D,
                bdvHandles[2],
                bdvHandles[0],
                this::getRotatedView2);
        bdvHandles[2].getViewerPanel()
                .addTransformListener(listener);
        bdvHandleToTransformListener.put(bdvHandles[2], listener);


        if (synchronizeTime) {
            TimePointListener timeListener;
            timeListener = (timepoint) -> {
                if (bdvHandles[1].getViewerPanel().state().getCurrentTimepoint() != timepoint)
                    bdvHandles[1].getViewerPanel().setTimepoint(timepoint);
            };

            bdvHandles[0].getViewerPanel().addTimePointListener(timeListener);
            bdvHandleToTimeListener.put(bdvHandles[0], timeListener);

            timeListener = (timepoint) -> {
                if (bdvHandles[2].getViewerPanel().state().getCurrentTimepoint() != timepoint)
                    bdvHandles[2].getViewerPanel().setTimepoint(timepoint);
            };

            bdvHandles[1].getViewerPanel().addTimePointListener(timeListener);
            bdvHandleToTimeListener.put(bdvHandles[1], timeListener);


            timeListener = (timepoint) -> {
                if (bdvHandles[0].getViewerPanel().state().getCurrentTimepoint() != timepoint)
                    bdvHandles[0].getViewerPanel().setTimepoint(timepoint);
            };

            bdvHandles[2].getViewerPanel().addTimePointListener(timeListener);
            bdvHandleToTimeListener.put(bdvHandles[2], timeListener);
        }


        // Setting first transform for initial synchronization,
        // but only if the two necessary objects are present (the origin BdvHandle and the transform
        if ((bdvHandleInitialReference != null) && (at3Dorigin != null)) {
            for (BdvHandle bdvh : bdvHandles) {
                bdvh.getViewerPanel().setCurrentViewerTransform(at3Dorigin.copy());
                bdvh.getViewerPanel().requestRepaint();
                if (synchronizeTime) {
                    bdvh.getViewerPanel().state().setCurrentTimepoint(
                            bdvHandleInitialReference.getViewerPanel().state().getCurrentTimepoint()
                    );
                }
            }
        }
    }

    void propagateTransformIfNecessary(AffineTransform3D at3D, BdvHandle currentBdvHandle, BdvHandle nextBdvHandle, Function<double[], double[]> rotator) {

        // Is the transform necessary ? That's the stop condition

        double cur_wcx = currentBdvHandle.getViewerPanel().getWidth() / 2.0; // Current Window Center X
        double cur_wcy = currentBdvHandle.getViewerPanel().getHeight() / 2.0; // Current Window Center Y

        RealPoint centerScreenCurrentBdv = new RealPoint(new double[]{cur_wcx, cur_wcy, 0});
        RealPoint centerScreenGlobalCoord = new RealPoint(3);

        at3D.inverse().apply(centerScreenCurrentBdv, centerScreenGlobalCoord);

        // Now compute what should be the matrix in the next BDV frame:
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
        double next_wcx = nextBdvHandle.getViewerPanel().getWidth() / 2.0; // Next Window Center X
        double next_wcy = nextBdvHandle.getViewerPanel().getHeight() / 2.0; // Next Window Center Y

        RealPoint centerScreenNextBdv = new RealPoint(new double[]{next_wcx, next_wcy, 0});
        RealPoint shiftNextBdv = new RealPoint(3);

        nextAffineTransform.inverse().apply(centerScreenNextBdv, shiftNextBdv);

        double sx = -centerScreenGlobalCoord.getDoublePosition(0) + shiftNextBdv.getDoublePosition(0);
        double sy = -centerScreenGlobalCoord.getDoublePosition(1) + shiftNextBdv.getDoublePosition(1);
        double sz = -centerScreenGlobalCoord.getDoublePosition(2) + shiftNextBdv.getDoublePosition(2);

        RealPoint shiftWindow = new RealPoint(new double[]{sx, sy, sz});
        RealPoint shiftMatrix = new RealPoint(3);
        nextAffineTransform.apply(shiftWindow, shiftMatrix);

        nextAffineTransform.set(shiftMatrix.getDoublePosition(0), 0, 3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(1), 1, 3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(2), 2, 3);

        // Is the transform necessary ? That's the stop condition
        AffineTransform3D ati = new AffineTransform3D();
        nextBdvHandle.getViewerPanel().state().getViewerTransform(ati);

        if (!MatrixApproxEquals(nextAffineTransform.getRowPackedCopy(), ati.getRowPackedCopy())) {
            // Yes -> triggers a transform change to the nextBdvHandle
            // For ortho view : switches axis:
            // X --> Y
            // Y --> Z
            // Z --> X
            // Calling it three times leads to an identical transform, hence the stopping condition is triggered
            AffineTransform3D nextAt3D = nextAffineTransform.copy();
            nextAt3D.set(nextAffineTransform.getRowPackedCopy());
            nextBdvHandle.getViewerPanel().setCurrentViewerTransform(nextAt3D);
            nextBdvHandle.getViewerPanel().requestRepaint();
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
     * A simple search to identify the view transform of the BdvHandle that will be used
     * for the initial synchronization (first reference)
     *
     * @return
     */
    private AffineTransform3D getViewTransformForInitialSynchronization() {
        AffineTransform3D at3Dorigin = null;
        for (int i = 0; i < bdvHandles.length; i++) {
            BdvHandle bdvHandle = bdvHandles[i];
            // if the BdvHandle is the one that should be used for initial synchronization
            if (bdvHandle.equals(bdvHandleInitialReference)) {
                // Storing the transform that will be used for first synchronization
                at3Dorigin = new AffineTransform3D();
                bdvHandle.getViewerPanel().state().getViewerTransform(at3Dorigin);
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
    public Map<BdvHandle, TransformListener<AffineTransform3D>> getSynchronizers() {
        return bdvHandleToTransformListener;
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
    public Map<BdvHandle, TimePointListener> getTimeSynchronizers() {
        return bdvHandleToTimeListener;
    }
}
