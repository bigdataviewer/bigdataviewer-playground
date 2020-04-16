package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.viewer.TimePointListener;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import java.util.Map;

/**
 * Action which stops the synchronization of the display location of n BdvHandle
 * Works in combination with the action ViewerTransformSyncStarter
 *
 * See also ViewTransformSynchronizationDemo
 *
 * author Nicolas Chiaruttini, BIOP EPFL, nicolas.chiaruttini@epfl.ch
 **/

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
        bdvHandleToTransformListener.forEach((bdvHandle, listener) -> {
            bdvHandle.getViewerPanel().removeTransformListener(listener);
        });
        if (bdvHandleToTimePointListener!=null) {
            bdvHandleToTimePointListener.forEach((bdvHandle, listener) -> {
                bdvHandle.getViewerPanel().removeTimePointListener(listener);
            });
        }
    }

    public static boolean MatrixApproxEquals(double[] m1, double[] m2) {
        assert m1.length == m2.length;
        boolean ans = true;
        for (int i=0;i<m1.length;i++) {
            //System.out.println("Math.abs(m1["+i+"]-m2["+i+"]) = "+(Math.abs(m1[i]-m2[i])));
            //System.out.println("Math.ulp(Math.min(Math.abs(m1[i]), Math.abs(m2[i]))) = "+(Math.ulp(Math.min(Math.abs(m1[i]), Math.abs(m2[i])))));

            if (Math.abs(m1[i]-m2[i])>1e6*Math.ulp(Math.min(Math.abs(m1[i]), Math.abs(m2[i])))) {
                ans = false;
                break;
            }
        }
        return ans;
    }

}
