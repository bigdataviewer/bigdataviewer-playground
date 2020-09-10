package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.viewer.TimePointListener;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.TransformListener;//net.imglib2.ui.TransformListener;
import java.util.HashMap;
import java.util.Map;

import static sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper.MatrixApproxEquals;

/**
 * BigDataViewer Playground Action -->
 * Action which synchronizes the display location of a {@link BdvHandle}
 *
 * Works in combination with the action {@link ViewerTransformSyncStopper}
 *
 * See also ViewTransformSynchronizationDemo
 *
 * Principle : for every changed view transform of a specific BdvHandle,
 * the view transform change is triggered to the following BdvHandle in a closed loop manner
 *
 * Note : the center of the window in global coordinate is kept identical between BdvHandles
 *
 * Note : closing one window in the chain breaks the synchronization TODO make more robust
 *
 * To avoid infinite loop, the stop condition is : if the view transform is unnecessary
 * (i.e. the view target is approximately equal to the source {@link ViewerTransformSyncStopper#MatrixApproxEquals}),
 * then there's no need to trigger a view transform change to the next BdvHandle
 *
 * see also {@link ViewerOrthoSyncStarter}
 *
 * See {@link ViewTransformSynchronizationDemo} for a usage example
 *
 * @author Nicolas Chiaruttini, BIOP EPFL, nicolas.chiaruttini@epfl.ch
 */

public class ViewerTransformSyncStarter implements Runnable {

    /**
     * Array of BdvHandles to synchronize
     */
    BdvHandle[] bdvHandles;

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
     * the synchronization
     */
    Map<BdvHandle, TransformListener<AffineTransform3D>> bdvHandleToTransformListener = new HashMap<>();


    /** Optional time synchronization
     *
     */
    boolean synchronizeTime;

    /**
     * Map which links each BdvHandle to the TimePointListener which has been added
     * for synchronization purpose. This object contains all what's neede to stop
     * the synchronization
     */
    Map<BdvHandle, TimePointListener> bdvHandleToTimeListener = new HashMap<>();


    public ViewerTransformSyncStarter(BdvHandle[] bdvHandles, boolean synchronizeTime) {
       this.bdvHandles = bdvHandles;
       this.synchronizeTime = synchronizeTime;
    }

    public void setBdvHandleInitialReference(BdvHandle bdvHandle) {
        bdvHandleInitialReference = bdvHandle;
    }

    @Override
    public void run() {

        // Getting transform for initial sync
        AffineTransform3D at3Dorigin = getViewTransformForInitialSynchronization();

        // Building circularly linked listeners with stop condition when all transforms are equal,
        // cf documentation

        for (int i = 0; i< bdvHandles.length; i++) {

            // The idea is that bdvHandles[i], when it has a view transform,
            // triggers an identical ViewTransform to the next bdvHandle in the array
            // (called nextBdvHandle). nextBdvHandle is bdvHandles[i+1] in most cases,
            // unless it's the end of the array,
            // where in this case nextBdvHandle is bdvHandles[0]
            BdvHandle currentBdvHandle = bdvHandles[i];
            BdvHandle nextBdvHandle;

            // Identifying nextBdvHandle
            if (i == bdvHandles.length-1) {
                nextBdvHandle = bdvHandles[0];
            } else {
                nextBdvHandle = bdvHandles[i+1];
            }

            // Building the TransformListener of currentBdvHandle
            TransformListener<AffineTransform3D> listener = (at3D) -> propagateTransformIfNecessary(at3D, currentBdvHandle, nextBdvHandle);

            // Adding this transform listener to the currenBdvHandle
            currentBdvHandle.getViewerPanel().addTransformListener(listener);

            // Storing the transform listener -> needed to remove them in order to stop synchronization when needed
            bdvHandleToTransformListener.put(bdvHandles[i], listener);

            if (synchronizeTime) {
                TimePointListener timeListener = (timepoint) -> {
                    if (nextBdvHandle.getViewerPanel().state().getCurrentTimepoint()!=timepoint)
                        nextBdvHandle.getViewerPanel().setTimepoint(timepoint);
                };

                currentBdvHandle.getViewerPanel().addTimePointListener(timeListener);
                bdvHandleToTimeListener.put(bdvHandles[i], timeListener);
            }
        }

        // Setting first transform for initial synchronization,
        // but only if the two necessary objects are present (the origin BdvHandle and the transform
         if ((bdvHandleInitialReference !=null)&&(at3Dorigin!=null)) {
             for (BdvHandle bdvh: bdvHandles) {
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

    void propagateTransformIfNecessary(AffineTransform3D at3D, BdvHandle currentBdvHandle, BdvHandle nextBdvHandle) {
        // We need to transfer the transform - but while keeping the center of the window constant
        double cur_wcx = currentBdvHandle.getViewerPanel().getWidth()/2.0; // Current Window Center X
        double cur_wcy = currentBdvHandle.getViewerPanel().getHeight()/2.0; // Current Window Center Y

        RealPoint centerScreenCurrentBdv = new RealPoint(new double[]{cur_wcx, cur_wcy, 0});
        RealPoint centerScreenGlobalCoord = new RealPoint(3);

        at3D.inverse().apply(centerScreenCurrentBdv, centerScreenGlobalCoord);
        //System.out.println("centerScreenGlobalCoord"+centerScreenGlobalCoord);

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
        double next_wcx = nextBdvHandle.getViewerPanel().getWidth()/2.0; // Next Window Center X
        double next_wcy = nextBdvHandle.getViewerPanel().getHeight()/2.0; // Next Window Center Y

        RealPoint centerScreenNextBdv = new RealPoint(new double[]{next_wcx, next_wcy, 0});
        RealPoint shiftNextBdv = new RealPoint(3);

        nextAffineTransform.inverse().apply(centerScreenNextBdv, shiftNextBdv);

        double sx = -centerScreenGlobalCoord.getDoublePosition(0)+shiftNextBdv.getDoublePosition(0);
        double sy = -centerScreenGlobalCoord.getDoublePosition(1)+shiftNextBdv.getDoublePosition(1);
        double sz = -centerScreenGlobalCoord.getDoublePosition(2)+shiftNextBdv.getDoublePosition(2);

        RealPoint shiftWindow = new RealPoint(new double[]{sx, sy, sz});
        RealPoint shiftMatrix = new RealPoint(3);
        nextAffineTransform.apply(shiftWindow, shiftMatrix);

        nextAffineTransform.set(shiftMatrix.getDoublePosition(0),0,3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(1),1,3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(2),2,3);

        // Is the transform necessary ? That's the stop condition
        AffineTransform3D ati = new AffineTransform3D();
        nextBdvHandle.getViewerPanel().state().getViewerTransform(ati);

        if (!MatrixApproxEquals(nextAffineTransform.getRowPackedCopy(), ati.getRowPackedCopy())) {
            // Yes -> triggers a transform change to the nextBdvHandle
            // Calling it three times leads to an identical transform, hence the stopping condition is triggered
            AffineTransform3D nextAt3D = nextAffineTransform.copy();
            nextAt3D.set(nextAffineTransform.getRowPackedCopy());
            nextBdvHandle.getViewerPanel().setCurrentViewerTransform(nextAt3D);
            nextBdvHandle.getViewerPanel().requestRepaint();
        }
    }

    /**
     * A simple search to identify the view transform of the BdvHandle that will be used
     * for the initial synchronization (first reference)
     * @return
     */
    private AffineTransform3D getViewTransformForInitialSynchronization() {
        AffineTransform3D at3Dorigin = null;
        for (int i = 0; i< bdvHandles.length; i++) {
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
     * @return map which can be used to stop the spatial synchronization see {@link ViewerTransformSyncStopper}
     */
    public Map<BdvHandle, TransformListener<AffineTransform3D>> getSynchronizers() {
        return bdvHandleToTransformListener;
    }

    public boolean isSynchronizingTime() {
        return synchronizeTime;
    }

    /**
     * @return map which can be used to stop the time synchronization see {@link ViewerTransformSyncStopper}
     */
    public Map<BdvHandle, TimePointListener> getTimeSynchronizers() {
        return bdvHandleToTimeListener;
    }
}
