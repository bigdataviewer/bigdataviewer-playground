package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
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
 * author Nicolas Chiaruttini, BIOP EPFL, nicolas.chiaruttini@epfl.ch
 *
 */

public class ViewerTransformSyncStarter implements Runnable {

    BdvHandle[] bdvhs;

    BdvHandle originatingBdvHandle = null;

    Map<BdvHandle, TransformListener<AffineTransform3D>> bdvToTransformListener = new HashMap<>();

    public ViewerTransformSyncStarter(BdvHandle[] bdvhs) {
       this.bdvhs = bdvhs;
    }

    public void setOriginatingBdvHandle(BdvHandle bdvh) {
        originatingBdvHandle = bdvh;
    }

    @Override
    public void run() {

        // Building circularly linked listeners with stop condition when all transforms are equal

        AffineTransform3D at3Dorigin = null;

        for (int i=0;i<bdvhs.length;i++) {
            BdvHandle bdvhi = bdvhs[i];
            if (bdvhi.equals(originatingBdvHandle)) {
                //System.out.println("Catching origin transform");
                at3Dorigin = new AffineTransform3D();
                bdvhi.getViewerPanel().getState().getViewerTransform(at3Dorigin);
            }
        }

        for (int i=0;i<bdvhs.length;i++) {

            BdvHandle bdvhip1;
            if (i == bdvhs.length-1) {
                bdvhip1 = bdvhs[0];
            } else {
                bdvhip1 = bdvhs[i+1];
            }

            final int ifinal = i;
            TransformListener<AffineTransform3D> listener =
                    (at3D) -> {
                        //System.out.println("listener index "+ifinal+" called");
                        AffineTransform3D ati = new AffineTransform3D();
                        bdvhip1.getViewerPanel().getState().getViewerTransform(ati);
                        if (!Arrays.equals(at3D.getRowPackedCopy(), ati.getRowPackedCopy())) {
                            bdvhip1.getViewerPanel().setCurrentViewerTransform(at3D.copy());
                            bdvhip1.getViewerPanel().requestRepaint();
                        }
                    };
            bdvhs[i].getViewerPanel().addTransformListener(listener);
            bdvToTransformListener.put(bdvhs[i], listener);
        }

         if ((originatingBdvHandle!=null)&&(at3Dorigin!=null)) {
             //System.out.println("Fixing origin to proper location");
             for (BdvHandle bdvh:bdvhs) {
                 bdvh.getViewerPanel().setCurrentViewerTransform(at3Dorigin.copy());
                 bdvh.getViewerPanel().requestRepaint();
             }
         }
    }

    public Map<BdvHandle, TransformListener<AffineTransform3D>> getSynchronizers() {
        return bdvToTransformListener;
    }
}
