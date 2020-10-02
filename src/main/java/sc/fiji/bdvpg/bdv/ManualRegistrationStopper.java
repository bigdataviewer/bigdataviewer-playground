package sc.fiji.bdvpg.bdv;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

// TODO : Ensure volatile is working with source which are not AbstractSpimSource

/**
 * BigDataViewer Playground Action -->
 * Action which stops the manual registration of n {@link SourceAndConverter}s
 * Works in coordination with {@link ManualRegistrationStarter}
 *
 * Works with a single BdvHandle (TODO : synchronizes with multiple BdvHandle)
 *
 * Working principle ( read {@link ManualRegistrationStarter} first ) :
 * - Stops listener of manual registration
 * - Removes transiently wrapped sources from display, and from {@link SourceAndConverterServices}
 *
 * - Transform all the sources that needed to be transformed, according to the {@link ManualRegistrationStopper#registrationPolicy} (see details below)
 *
 * - Restores the initially displays sources, but transformed according to the chosen registrationPolicy
 *
 * a registrationPolicy is a function that outputs a registered source, with inputs being the initial source with a time range {@link SourceAndConverterAndTimeRange}
 * and an affine transform, thus it's {@code BiFunction<AffineTransform3D, SourceAndConverterAndTimeRange, SourceAndConverter>}
 *
 * This modularity allows for different ways to store the registration depending on the source.
 *
 * Standards policies are implemented in {@link sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper}
 *
 * * mutate : branch between mutateTransformedSourceAndConverter and mutateLastSpimdataTransformation depending  on the source class
 * * append : branch between createNewTransformedSourceAndConverter and appendNewSpimdataTransformation depending on the source class
 *
 * Any other policy can be used since it is a parameter of this action
 *
 * @author : Nicolas Chiaruttini, BIOP, EPFL 2019
 */

public class ManualRegistrationStopper implements Runnable {

    ManualRegistrationStarter starter;

    BiFunction<AffineTransform3D, SourceAndConverterAndTimeRange, SourceAndConverter> registrationPolicy;// = ManualRegistrationStopper::createNewTransformedSourceAndConverter;

    SourceAndConverter[] transformedSources;

    public ManualRegistrationStopper(ManualRegistrationStarter starter, BiFunction<AffineTransform3D, SourceAndConverterAndTimeRange, SourceAndConverter> registrationPolicy) {
        this.starter = starter;
        this.registrationPolicy = registrationPolicy;
    }

    @Override
    public void run() {

        // Gets the final transformation
        AffineTransform3D transform3D = this.starter.getCurrentTransform().copy();

        // Stops BdvHandle listener
        this.starter.getBdvHandle().getViewerPanel().removeTransformListener(starter.getListener());

        // Removes temporary TransformedSourceAndConverter - a two step process in order to improve performance
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

            transformedSources[i] = registrationPolicy.apply(transform3D, new SourceAndConverterAndTimeRange(sac, starter.bdvHandle.getViewerPanel().state().getCurrentTimepoint()));
            if (starter.getOriginallyDisplayedSourceAndConverter().contains(sac)) {
                transformedSacsToDisplay.add(transformedSources[i]);
            }
        }

        // Calls display ( array for better performance )
        SourceAndConverterServices.getSourceAndConverterDisplayService().show(starter.getBdvHandle(),
                transformedSacsToDisplay.toArray(new SourceAndConverter[transformedSacsToDisplay.size()]));

    }

    public SourceAndConverter[] getTransformedSources() {
        return transformedSources;
    }
}
