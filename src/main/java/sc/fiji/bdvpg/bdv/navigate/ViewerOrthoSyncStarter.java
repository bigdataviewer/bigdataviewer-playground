package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Action which synchronizes the display location of 3 BdvHandle
 *
 * TODO : Works in combination with the action ViewerOrthoSyncStopper
 *
 * TODO See also ViewOrthoSynchronizationDemo
 *
 * Principle : for every changed view transform of a specific BdvHandle,
 * the view transform change is triggered to the following BdvHandle in a closed loop manner
 *
 * Each transform is passed to next one by rolling the axes so that 3 swaps lead to an identical transform:
 *
 // For ortho view : switches axis:
 // X --> Y
 // Y --> Z
 // Z --> X
 *
 * TODO : Issue : the center is at the top left corner of the bdv window, instead of being at the center
 *
 * To avoid inifinite loop, the stop condition is : if the view transform is unnecessary (between
 * the view target is equal to the source), then there's no need to trigger a view transform change
 * to the next BdvHandle
 *
 * author Nicolas Chiaruttini, BIOP EPFL, nicolas.chiaruttini@epfl.ch
 */

public class ViewerOrthoSyncStarter implements Runnable {

    /**
     * Array of BdvHandles to synchronize
     */
    BdvHandle[] bdvHandles = new BdvHandle[3]; // X Y Z

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

    public ViewerOrthoSyncStarter(BdvHandle bdvHandleX, BdvHandle bdvHandleY, BdvHandle bdvHandleZ) {
        this.bdvHandles[0] = bdvHandleX;
        this.bdvHandles[1] = bdvHandleY;
        this.bdvHandles[2] = bdvHandleZ;
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
                        propagateTransformIfNecessary(at3D, currentBdvHandle, nextBdvHandle);
                    };

            // Adding this transform listener to the currenBdvHandle
            currentBdvHandle.getViewerPanel().addTransformListener(listener);

            // Storing the transform listener -> needed to remove them in order to stop synchronization when needed
            bdvHandleToTransformListener.put(bdvHandles[i], listener);
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

    void propagateTransformIfNecessary(AffineTransform3D at3D, BdvHandle currentBdvHandle, BdvHandle nextBdvHandle) {
        // Is the transform necessary ? That's the stop condition
        AffineTransform3D ati = new AffineTransform3D();
        nextBdvHandle.getViewerPanel().getState().getViewerTransform(ati);

        if (!Arrays.equals(getRotatedView(at3D.getRowPackedCopy()), ati.getRowPackedCopy())) {
            // Yes -> triggers a transform change to the nextBdvHandle
            // For ortho view : switches axis:
            // X --> Y
            // Y --> Z
            // Z --> X
            // Calling it three times leads to an identical transform, hence the stopping condition is triggered
            AffineTransform3D nextAt3D = at3D.copy();
            nextAt3D.set(getRotatedView(at3D.getRowPackedCopy()));
            nextBdvHandle.getViewerPanel().setCurrentViewerTransform(nextAt3D);
            nextBdvHandle.getViewerPanel().requestRepaint();
        }
    }


    public double[] getRotatedView(double[] m) {
        return new double[] {
                m[1], m[2], m[0], m[3],
                m[5], m[6], m[4], m[7],
                m[9], m[10], m[8], m[11],
        };
        /*
        return new double[] {
				a.m00, a.m01, a.m02, a.m03,
				a.m10, a.m11, a.m12, a.m13,
				a.m20, a.m21, a.m22, a.m23
		};
         */
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
}
