package sc.fiji.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.viewer.animate.SimilarityTransformAnimator;
import bdv.viewer.state.ViewerState;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdv.BDVUtils;

/**
 * ViewTransformator
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class ViewTransformator implements Runnable {

    private BdvHandle bdvHandle;
    private AffineTransform3D transform;

    public ViewTransformator(BdvHandle bdvHandle, AffineTransform3D transform) {
        this.bdvHandle = bdvHandle;
        this.transform = transform;
    }

    @Override
    public void run() {
        // get current transform
        AffineTransform3D view = new AffineTransform3D();
        bdvHandle.getViewerPanel().getState().getViewerTransform(view);

        // change the transform
        view = view.concatenate(transform);

        // submit to BDV
        bdvHandle.getViewerPanel().setCurrentViewerTransform(view);

    }
}
