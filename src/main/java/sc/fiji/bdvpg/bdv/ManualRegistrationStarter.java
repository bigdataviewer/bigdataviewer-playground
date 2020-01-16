package sc.fiji.bdvpg.bdv;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransform;

public class ManualRegistrationStarter implements Runnable {

    SourceAndConverter[] sacs;
    SourceAndConverter[] wrappedSacs;

    BdvHandle bdvHandle;

    AffineTransform3D diff;

    TransformListener<AffineTransform3D> manualRegistrationListener; // Listener used to maintain the non moving source in place

    public ManualRegistrationStarter(BdvHandle bdvHandle, SourceAndConverter... sacs) {
            this.sacs = sacs;
            this.bdvHandle = bdvHandle;
    }

    @Override
    public void run() {
        wrappedSacs = new SourceAndConverter[sacs.length];
        for (int i=0;i<sacs.length;i++) {
            // Wraps into a Transformed Source
            wrappedSacs[i] = new SourceAffineTransform(sacs[i], new AffineTransform3D()).getSourceOut();
            // Remove initial source from current display
            BdvService.getSourceAndConverterDisplayService().remove(bdvHandle, sacs[i]);
            // Put the wrapped Source
            BdvService.getSourceAndConverterDisplayService().show(bdvHandle, wrappedSacs[i]);
        }

        // View of the BdvHandle before starting the registration
        AffineTransform3D originalViewTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().getState().getViewerTransform(originalViewTransform);

        manualRegistrationListener = (newView) -> {
                // Fetch ViewTransformDifference

                // Keeps the final AffineTransform orthonormal

                // Global difference of transform is
                diff = newView.copy();
                diff = diff.inverse();
                diff = diff.concatenate(originalViewTransform);

                for (int i=0;i<sacs.length;i++) {
                    ((TransformedSource) wrappedSacs[i].getSpimSource())
                            .setFixedTransform(diff);

                }
        };
        bdvHandle.getViewerPanel().addTransformListener(manualRegistrationListener);
    }

    public BdvHandle getBdvHandle() {
        return bdvHandle;
    }

    public TransformListener<AffineTransform3D> getListener() {
        return manualRegistrationListener;
    }

    public SourceAndConverter[] getTransformedSourceAndConverter() {
        return wrappedSacs;
    }

    public SourceAndConverter[] getOriginalSourceAndConverter() {
        return sacs;
    }

    public AffineTransform3D getCurrentTransform() {
        return diff;
    }

}
