package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ViewerTransformSyncStopper implements Runnable {

    Map<BdvHandle, TransformListener<AffineTransform3D>> bdvToTransformListener;

    public ViewerTransformSyncStopper(Map<BdvHandle, TransformListener<AffineTransform3D>> bdvToTransformListener) {
       this.bdvToTransformListener = bdvToTransformListener;
    }

    @Override
    public void run() {
        bdvToTransformListener.forEach((bdvh, listener) -> {
            bdvh.getViewerPanel().removeTransformListener(listener);
        });
    }


}
