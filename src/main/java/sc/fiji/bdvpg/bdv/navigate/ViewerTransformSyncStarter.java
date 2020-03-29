package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.viewer.TimePointListener;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Action which synchronizes the display location of n BdvHandle
 *
 * Works in combination with the action ViewerTransformSyncStopper
 *
 * See also ViewTransformSynchronizationDemo
 *
 * Principle : for every changed view transform of a specific BdvHandle,
 * the view transform change is triggered to the following BdvHandle in a closed loop manner
 *
 * To avoid inifinite loop, the stop condition is : if the view transform is unnecessary (between
 * the view target is equal to the source), then there's no need to trigger a view transform change
 * to the next BdvHandle
 *
 * author Nicolas Chiaruttini, BIOP EPFL, nicolas.chiaruttini@epfl.ch
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
     * for synchronization purpose. This object contains all what's neede to stop
     * the synchronization
     */
    Map<BdvHandle, TransformListener<AffineTransform3D>> bdvHandleToTransformListener = new HashMap<>();


    /** Optional time synchronization
     *
     */
    boolean synchronizeTime;

    /**
     * Map which links each BdvHandle to the TransformListener which has been added
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
            TransformListener<AffineTransform3D> listener =
                    (at3D) -> {
                        // Is the transform necessary ? That's the stop condition
                        AffineTransform3D ati = new AffineTransform3D();
                        nextBdvHandle.getViewerPanel().getState().getViewerTransform(ati);
                        if (!Arrays.equals(at3D.getRowPackedCopy(), ati.getRowPackedCopy())) {
                            // Yes -> triggers a transform change to the nextBdvHandle
                            nextBdvHandle.getViewerPanel().setCurrentViewerTransform(at3D.copy());
                            nextBdvHandle.getViewerPanel().requestRepaint();
                        }
                    };

            // Adding this transform listener to the currenBdvHandle
            currentBdvHandle.getViewerPanel().addTransformListener(listener);

            // Storing the transform listener -> needed to remove them in order to stop synchronization when needed
            bdvHandleToTransformListener.put(bdvHandles[i], listener);

            if (synchronizeTime) {
                TimePointListener timeListener = (timepoint) -> {
                    if (nextBdvHandle.getViewerPanel().getState().getCurrentTimepoint()!=timepoint)
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
             }
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
                bdvHandle.getViewerPanel().getState().getViewerTransform(at3Dorigin);
            }
        }
        return at3Dorigin;
    }

    /**
     * output of this action : this map can be used to stop the synchronization
     * see ViewerTransformSyncStopper
     * @return
     */
    public Map<BdvHandle, TransformListener<AffineTransform3D>> getSynchronizers() {
        return bdvHandleToTransformListener;
    }

    public boolean isSynchronizingTime() {
        return synchronizeTime;
    }

    /**
     * output of this action : this map can be used to stop the synchronization
     * see ViewerTransformSyncStopper
     * @return
     */
    public Map<BdvHandle, TimePointListener> getTimeSynchronizers() {
        return bdvHandleToTimeListener;
    }
}
