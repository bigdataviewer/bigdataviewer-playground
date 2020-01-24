package sc.fiji.bdvpg.bdv;

import bdv.AbstractSpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

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

    /**
     * Wraps into transformed sources the registered sources
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter createNewTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        SourceAndConverter transformedSac = new SourceAffineTransformer(sac, affineTransform3D).getSourceOut();
        return transformedSac;
    }

    /**
     * provided a source was already a trasnformed source, updates the inner affineTransform3D
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter mutateTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert sac.getSpimSource() instanceof TransformedSource;
        AffineTransform3D at3D = new AffineTransform3D();
        ((TransformedSource)sac.getSpimSource()).getFixedTransform(at3D);
        ((TransformedSource)sac.getSpimSource()).setFixedTransform(at3D.preConcatenate(affineTransform3D));
        return sac;
    }

    /**
     * if a source has a linked spimdata, mutates the last registration to account for changes
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter mutateLastSpimdataTransformation(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sac).containsKey(SPIM_DATA_INFO);
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sac).get(SPIM_DATA_INFO) instanceof SourceAndConverterService.SpimDataInfo;

        SourceAndConverterService.SpimDataInfo sdi = ((SourceAndConverterService.SpimDataInfo)
                SourceAndConverterServices.getSourceAndConverterService()
                        .getSacToMetadata().get(sac).get(SPIM_DATA_INFO));

        // TODO : find a way to pass the ref of starter into this function ? but static looks great...
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        int timePoint = bdvHandle.getViewerPanel().getState().getCurrentTimepoint();

        ViewRegistration vr = sdi.asd.getViewRegistrations().getViewRegistration(timePoint,sdi.setupId);

        ViewTransform vt = vr.getTransformList().get(vr.getTransformList().size()-1);

        AffineTransform3D at3D = new AffineTransform3D();
        at3D.concatenate(vt.asAffine3D());
        at3D.concatenate(affineTransform3D);

        ViewTransform newvt = new ViewTransformAffine(vt.getName(), at3D);

        vr.getTransformList().remove(vt);
        vr.getTransformList().add(newvt);
        vr.updateModel();


        try {
            Method updateBdvSource = Class.forName("bdv.AbstractSpimSource").getDeclaredMethod("loadTimepoint", int.class);
            updateBdvSource.setAccessible(true);
            AbstractSpimSource ass = (AbstractSpimSource) sac.getSpimSource();
            updateBdvSource.invoke(ass, timePoint);

            if (sac.asVolatile() != null) {
                ass = (AbstractSpimSource) sac.asVolatile().getSpimSource();
                updateBdvSource.invoke(ass, timePoint);
            }

        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sac;
    }

    /**
     * if a source has a linked spimdata, appends a new transformation in the registration model
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter appendNewSpimdataTransformation(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sac).containsKey(SPIM_DATA_INFO);
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sac).get(SPIM_DATA_INFO) instanceof SourceAndConverterService.SpimDataInfo;

        SourceAndConverterService.SpimDataInfo sdi = ((SourceAndConverterService.SpimDataInfo)
                SourceAndConverterServices.getSourceAndConverterService()
                        .getSacToMetadata().get(sac).get(SPIM_DATA_INFO));

        // TODO : find a way to pass the ref of starter into this function ? but static looks great...
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        int timePoint = bdvHandle.getViewerPanel().getState().getCurrentTimepoint();

        ViewTransform newvt = new ViewTransformAffine("Manual transform", affineTransform3D);

        sdi.asd.getViewRegistrations().getViewRegistration(timePoint,sdi.setupId).preconcatenateTransform(newvt);
        sdi.asd.getViewRegistrations().getViewRegistration(timePoint,sdi.setupId).updateModel();

        try {
            Method updateBdvSource = Class.forName("bdv.AbstractSpimSource").getDeclaredMethod("loadTimepoint", int.class);
            updateBdvSource.setAccessible(true);
            AbstractSpimSource ass = (AbstractSpimSource) sac.getSpimSource();
            updateBdvSource.invoke(ass, timePoint);

            if (sac.asVolatile() != null) {
                ass = (AbstractSpimSource) sac.asVolatile().getSpimSource();
                updateBdvSource.invoke(ass, timePoint);
            }

        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sac;
    }

    /**
     *
     * branch between mutateTransformedSourceAndConverter and mutateLastSpimdataTransformation depending  on the source class
     *
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter mutate(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        if (sac.getSpimSource() instanceof AbstractSpimSource) {
            if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac,SPIM_DATA_INFO)!=null) {
                return mutateLastSpimdataTransformation(affineTransform3D, sac);
            } else {
                if (sac.getSpimSource() instanceof TransformedSource) {
                    return mutateTransformedSourceAndConverter(affineTransform3D,sac);
                } else {
                    return createNewTransformedSourceAndConverter(affineTransform3D,sac);
                }
            }
        } else if (sac.getSpimSource() instanceof TransformedSource) {
            return mutateTransformedSourceAndConverter(affineTransform3D,sac);
        } else {
            return createNewTransformedSourceAndConverter(affineTransform3D,sac);
        }
    }

    /**
     *  branch between createNewTransformedSourceAndConverter and appendNewSpimdataTransformation depending on the source class
     *
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter append(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        if (sac.getSpimSource() instanceof AbstractSpimSource) {
            if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac,SPIM_DATA_INFO)!=null) {
                return appendNewSpimdataTransformation(affineTransform3D, sac);
            } else {
                return createNewTransformedSourceAndConverter(affineTransform3D,sac);
            }
        } else {
            return createNewTransformedSourceAndConverter(affineTransform3D,sac);
        }
    }

    /**
     * Ignores registration
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter cancel(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        return sac;
    }

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
