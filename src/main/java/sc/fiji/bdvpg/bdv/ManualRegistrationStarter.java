package sc.fiji.bdvpg.bdv;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManualRegistrationStarter implements Runnable {

    SourceAndConverter[] sacs;
    List<SourceAndConverter> originallyDisplayedSacs = new ArrayList<>();
    List<SourceAndConverter> displayedSacsWrapped = new ArrayList<>();

    BdvHandle bdvHandle;

    AffineTransform3D diff;

    TransformListener<AffineTransform3D> manualRegistrationListener; // Listener used to maintain the non moving source in place

    public ManualRegistrationStarter(BdvHandle bdvHandle, SourceAndConverter... sacs) {
            this.sacs = sacs;
            this.bdvHandle = bdvHandle;
    }

    @Override
    public void run() {
        //wrappedSacs = new SourceAndConverter[sacs.length];

        //int numberOfSourcesToDisplay = 0;
        BdvService.getSourceAndConverterDisplayService().logLocationsDisplayingSource();
        for (int i=0;i<sacs.length;i++) {
            System.out.println("i = "+i);
            // Wraps into a Transformed Source
            if (BdvService.getSourceAndConverterDisplayService().getDisplaysOf(sacs[i]).contains(bdvHandle)) {
                displayedSacsWrapped.add(new SourceAffineTransformer(sacs[i], new AffineTransform3D()).getSourceOut());
                originallyDisplayedSacs.add(sacs[i]);
                System.out.println("is displayed");
            }
        }

        BdvService.getSourceAndConverterDisplayService().remove(bdvHandle, originallyDisplayedSacs.toArray(new SourceAndConverter[originallyDisplayedSacs.size()]));
        // Put the displayed wrapped Source
        BdvService.getSourceAndConverterDisplayService().show(bdvHandle, displayedSacsWrapped.toArray(new SourceAndConverter[displayedSacsWrapped.size()]));

        // View of the BdvHandle before starting the registration
        AffineTransform3D originalViewTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().getState().getViewerTransform(originalViewTransform);

        manualRegistrationListener = (newView) -> {
                // Fetch ViewTransformDifference

                // Global difference of transform is
                diff = newView.copy();
                diff = diff.inverse();
                diff = diff.concatenate(originalViewTransform);

                // TODO check orthonormality !

                displayedSacsWrapped.forEach(sac -> ((TransformedSource) sac.getSpimSource()).setFixedTransform(diff));
        };

        bdvHandle.getViewerPanel().addTransformListener(manualRegistrationListener);
    }

    public BdvHandle getBdvHandle() {
        return bdvHandle;
    }

    public TransformListener<AffineTransform3D> getListener() {
        return manualRegistrationListener;
    }

    public List<SourceAndConverter> getTransformedSourceAndConverterDisplayed() {
        return displayedSacsWrapped;
    }

    public SourceAndConverter[] getOriginalSourceAndConverter() {
        return sacs;
    }

    public List<SourceAndConverter> getOriginallyDisplayedSourceAndConverter() {
        return originallyDisplayedSacs;
    }

    public AffineTransform3D getCurrentTransform() {
        return diff;
    }

}
