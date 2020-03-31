package sc.fiji.bdvpg.bdv;


import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;


// TODO : Ensure volatile is working with source which are not AbstractSpimSource

/**
 * Action which stops the manual registration of n SourceAndConverters
 * Works in coordination with ManualRegistrationStarter
 *
 * Works with a single BdvHandle (TODO : synchronizes with multiple BdvHandle)
 *
 * Working principle ( read ManualRegistrationStarter first ) :
 * - Stops listener of manual registration
 * - Removes transiently wrapped sources from display, and from BdvSourceAndCOnverterService
 *
 * - Transform all the sources that needed to be transformed, according to the registrationPolicy (see details below)
 *
 * - Restores the initially displays sources, but trasnformed according to the choosen registrationpolicy
 *
 * a registrationPolicy is a function that performes outputs a registered source, with inputs being the initial source
 * and an affine transform, thus it's BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>
 *
 * This modularity allows for different ways to store the registration depending on the source.
 *
 * A few policies are implemented in this action:
 * * createNewTransformedSourceAndConverter: Wraps into transformed sources the registered sources
 * * mutateTransformedSourceAndConverter: provided a source was already a trasnformed source, updates the inner affineTransform3D
 * * appendNewSpimdataTransformation: if a source has a linked spimdata, appends a new transformation in the registration model
 * * mutateLastSpimdataTransformation: if a source has a linked spimdata, mutates the last registration to account for changes
 * * cancel : ignore registration - returns original source
 *
 * * mutate : branch between mutateTransformedSourceAndConverter and mutateLastSpimdataTransformation depending  on the source class
 * * append : branch between createNewTransformedSourceAndConverter and appendNewSpimdataTransformation depending on the source class
 *
 * Any other policy can be used since it is a parameter of this action
 *
 *
 */

public class ManualRegistrationStopper implements Runnable {

    ManualRegistrationStarter starter;

    BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter> registrationPolicy;// = ManualRegistrationStopper::createNewTransformedSourceAndConverter;

    SourceAndConverter[] transformedSources;

    public ManualRegistrationStopper(ManualRegistrationStarter starter, BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter> registrationPolicy) {
        this.starter = starter;
        this.registrationPolicy = registrationPolicy;
    }

    @Override
    public void run() {

        // Gets the final transformation
        AffineTransform3D transform3D = this.starter.getCurrentTransform().copy();

        // Stops BdvHandle listener
        this.starter.getBdvHandle().getViewerPanel().removeTransformListener(starter.getListener());

        // Removes temporary TransformedSourceAndConverter - in two times for performance issue
        List<SourceAndConverter> tempSacs = starter.getTransformedSourceAndConverterDisplayed();
        SourceAndConverterServices.getSourceAndConverterDisplayService().remove(starter.bdvHandle,tempSacs.toArray(new SourceAndConverter[tempSacs.size()]));

        for (SourceAndConverter sac: tempSacs) {
            SourceAndConverterServices.getSourceAndConverterService().remove(sac);
        }

        int nSources = starter.getOriginalSourceAndConverter().length;
        transformedSources = new SourceAndConverter[nSources];

        List<SourceAndConverter> transformedSacsToDisplay = new ArrayList<>();
        // Applies the policy
        for (int i=0;i<nSources;i++) {
            SourceAndConverter sac  = this.starter.getOriginalSourceAndConverter()[i];
            transformedSources[i] = registrationPolicy.apply(transform3D, sac);
            if (starter.getOriginallyDisplayedSourceAndConverter().contains(sac)) {
                transformedSacsToDisplay.add(transformedSources[i]);
            }
        }

        // Calls display ( array for performance issue )
        SourceAndConverterServices.getSourceAndConverterDisplayService().show(starter.getBdvHandle(),
                transformedSacsToDisplay.toArray(new SourceAndConverter[transformedSacsToDisplay.size()]));

    }

    public SourceAndConverter[] getTransformedSources() {
        return transformedSources;
    }
}
