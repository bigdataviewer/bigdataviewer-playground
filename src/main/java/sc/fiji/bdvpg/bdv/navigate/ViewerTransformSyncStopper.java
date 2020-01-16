package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
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

    public ViewerTransformSyncStopper(Map<BdvHandle, TransformListener<AffineTransform3D>> bdvHandleToTransformListener) {
       this.bdvHandleToTransformListener = bdvHandleToTransformListener;
    }

    @Override
    public void run() {
        bdvHandleToTransformListener.forEach((bdvHandle, listener) -> {
            bdvHandle.getViewerPanel().removeTransformListener(listener);
        });
    }


}
