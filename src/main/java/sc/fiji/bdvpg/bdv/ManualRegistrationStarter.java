package sc.fiji.bdvpg.bdv;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Action which starts the manual registration of n SourceAndConverters
 * Works in coordination with ManualRegistrationStopper
 *
 * Works with a single BdvHandle (TODO : synchronizes with multiple BdvHandle)
 *
 * Working principle:
 * - Sources are transiently wrapped into TransformedSource (only the ones which are displayed) and displayed
 * - The original sources are removed from the display (those who were displayed)
 * - When a BdvHandle view is transformed because of a user action, all transformed sources
 * are transformed in such a way that there position relative to the viewer is kept identical.
 *
 * The ManualRegistrationStopper action does actually stores the registration once it is finished
 *
 * Note : all the selected sources will be registered ( parameter 'SourceAndConverter... sacs' in the constructor ),
 * however, only those who were displayed originally will be used for the intereactive manual registration,
 * this allows for a much stronger performance : you can actually register multiple
 * sources but only base your registration on a single displayed one.
 *
 */
public class ManualRegistrationStarter implements Runnable {

    /**
     * Sources that will be transformed
     */
    SourceAndConverter[] sacs;

    /**
     * From the sources that will be transformed, list of sources which were actually
     * displayed at the beginning of the action
     */
    List<SourceAndConverter> originallyDisplayedSacs = new ArrayList<>();

    /**
     * Transient transformed source displayed for the registration
     */
    List<SourceAndConverter> displayedSacsWrapped = new ArrayList<>();

    /**
     * bdvHandle used for the manual registration
     */
    BdvHandle bdvHandle;

    /**
     * Current registration state
     */
    AffineTransform3D currentRegistration;

    /**
     * Listener to BdvHandle view transform changes
     * - maintains the displayed registered source at the same location relative to the viewer
     */
    TransformListener<AffineTransform3D> manualRegistrationListener;

    public ManualRegistrationStarter(BdvHandle bdvHandle, SourceAndConverter... sacs) {
            this.sacs = sacs;
            this.bdvHandle = bdvHandle;
    }

    @Override
    public void run() {

        for (int i=0;i<sacs.length;i++) {

            // Wraps into a Transformed Source, if the source was displayed originally
            if (BdvService.getSourceAndConverterDisplayService().getDisplaysOf(sacs[i]).contains(bdvHandle)) {
                displayedSacsWrapped.add(new SourceAffineTransformer(sacs[i], new AffineTransform3D()).getSourceOut());
                originallyDisplayedSacs.add(sacs[i]);
            }
        }

        // Remove from display the originally dispalyed sources
        BdvService.getSourceAndConverterDisplayService().remove(bdvHandle, originallyDisplayedSacs.toArray(new SourceAndConverter[originallyDisplayedSacs.size()]));

        // Shows the displayed wrapped Source
        BdvService.getSourceAndConverterDisplayService().show(bdvHandle, displayedSacsWrapped.toArray(new SourceAndConverter[displayedSacsWrapped.size()]));

        // View of the BdvHandle before starting the registration
        AffineTransform3D originalViewTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().getState().getViewerTransform(originalViewTransform);

        manualRegistrationListener = (newView) -> {
                // Compute "difference" of ViewTransform beteween the original state and the current state

                // Global difference of transform is
                currentRegistration = newView.copy();
                currentRegistration = currentRegistration.inverse();
                currentRegistration = currentRegistration.concatenate(originalViewTransform);

                // TODO check orthonormality !
                // Sets view transform fo transiently wrapped soure to maintain relative position
                displayedSacsWrapped.forEach(sac -> ((TransformedSource) sac.getSpimSource()).setFixedTransform(currentRegistration));
        };

        // Sets the listener
        bdvHandle.getViewerPanel().addTransformListener(manualRegistrationListener);
    }

    public BdvHandle getBdvHandle() {
        return bdvHandle;
    }

    /**
     * Gets the listener -> useful to stop the registration
     * @return
     */
    public TransformListener<AffineTransform3D> getListener() {
        return manualRegistrationListener;
    }

    /**
     * Returns the transient wrapped transformed sources displayed (and then used by the user for the registration)
     * @return
     */
    public List<SourceAndConverter> getTransformedSourceAndConverterDisplayed() {
        return displayedSacsWrapped;
    }

    /**
     * Returns the sources that need to be registered
     * @return
     */
    public SourceAndConverter[] getOriginalSourceAndConverter() {
        return sacs;
    }

    /**
     * Returns the sources (within the sources that need to be transformed) that were originally displayed in the bdvHandle
     * @return
     */
    public List<SourceAndConverter> getOriginallyDisplayedSourceAndConverter() {
        return originallyDisplayedSacs;
    }

    /**
     * Gets the current registration state, based on the difference between the initial
     * bdvhandle view transform and its current view transform
     * @return
     */
    public AffineTransform3D getCurrentTransform() {
        return currentRegistration;
    }

}
