package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * BigDataViewer Playground Action -->
 * Action which concatenates the current viewTransform
 * of a {@link BdvHandle} with the input {@link AffineTransform3D}
 *
 * See ViewTransformSetAndLogDemo for a usage example
 *
 * @author @haesleinhuepf
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
        bdvHandle.getViewerPanel().state().getViewerTransform(view);

        // change the transform
        view = view.concatenate(transform);

        // submit to BDV
        bdvHandle.getViewerPanel().state().setViewerTransform(view);

    }
}
