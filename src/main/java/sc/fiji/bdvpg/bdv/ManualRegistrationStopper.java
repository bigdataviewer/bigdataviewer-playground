package sc.fiji.bdvpg.bdv;

import bdv.AbstractSpimSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

import static sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService.SPIM_DATA_INFO;

// TODO : Ensure volatile is working with source which are not AbstractSpimSource

public class ManualRegistrationStopper implements Runnable {

    ManualRegistrationStarter starter;

    BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter> registrationPolicy;// = ManualRegistrationStopper::createNewTransformedSourceAndConverter;

    SourceAndConverter[] transformedSources;

    public static SourceAndConverter createNewTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        SourceAndConverter transformedSac = new SourceAffineTransformer(sac, affineTransform3D).getSourceOut();
        return transformedSac;
    }

    public static SourceAndConverter mutateTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert sac.getSpimSource() instanceof TransformedSource;
        AffineTransform3D at3D = new AffineTransform3D();
        ((TransformedSource)sac.getSpimSource()).getFixedTransform(at3D);
        ((TransformedSource)sac.getSpimSource()).setFixedTransform(at3D.preConcatenate(affineTransform3D));
        // Not completely safe : we assume the active bdv is the one selected
        return sac;
    }

    public static SourceAndConverter mutateLastSpimdataTransformation(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert BdvService
                .getSourceAndConverterService()
                .getSourceAndConverterToMetadata().get(sac).containsKey(SPIM_DATA_INFO);
        assert BdvService
                .getSourceAndConverterService()
                .getSourceAndConverterToMetadata().get(sac).get(SPIM_DATA_INFO) instanceof BdvSourceAndConverterService.SpimDataInfo;

        BdvSourceAndConverterService.SpimDataInfo sdi = ((BdvSourceAndConverterService.SpimDataInfo)
                BdvService.getSourceAndConverterService()
                        .getSourceAndConverterToMetadata().get(sac).get(SPIM_DATA_INFO));

        // TODO : find a ref to starter
        BdvHandle bdvHandle = BdvService.getSourceAndConverterDisplayService().getActiveBdv();

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


    public static SourceAndConverter appendNewSpimdataTransformation(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert BdvService
                .getSourceAndConverterService()
                .getSourceAndConverterToMetadata().get(sac).containsKey(SPIM_DATA_INFO);
        assert BdvService
                .getSourceAndConverterService()
                .getSourceAndConverterToMetadata().get(sac).get(SPIM_DATA_INFO) instanceof BdvSourceAndConverterService.SpimDataInfo;

        BdvSourceAndConverterService.SpimDataInfo sdi = ((BdvSourceAndConverterService.SpimDataInfo)
                BdvService.getSourceAndConverterService()
                        .getSourceAndConverterToMetadata().get(sac).get(SPIM_DATA_INFO));

        // TODO : find a ref to starter
        BdvHandle bdvHandle = BdvService.getSourceAndConverterDisplayService().getActiveBdv();

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

    public ManualRegistrationStopper(ManualRegistrationStarter starter, BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter> registrationPolicy) {
        this.starter = starter;
        this.registrationPolicy = registrationPolicy;
    }

    public static SourceAndConverter mutate(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        if (sac.getSpimSource() instanceof AbstractSpimSource) {
            return mutateLastSpimdataTransformation(affineTransform3D, sac);
        } else if (sac.getSpimSource() instanceof TransformedSource) {
            return mutateTransformedSourceAndConverter(affineTransform3D,sac);
        } else {
            return createNewTransformedSourceAndConverter(affineTransform3D,sac);
        }
    }

    public static SourceAndConverter append(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        if (sac.getSpimSource() instanceof AbstractSpimSource) {
            return appendNewSpimdataTransformation(affineTransform3D, sac);
        } else {
            return createNewTransformedSourceAndConverter(affineTransform3D,sac);
        }
    }

    @Override
    public void run() {

        // Gets the final transformation
        AffineTransform3D transform3D = this.starter.getCurrentTransform().copy();

        // Stops BdvHandle listener
        this.starter.getBdvHandle().getViewerPanel().removeTransformListener(starter.getListener());

        // Removes temporary TransformedSourceAndConverter
        for (SourceAndConverter sac : this.starter.getTransformedSourceAndConverter()) {
            BdvService.getSourceAndConverterService().remove(sac);
        }

        int nSources = starter.getOriginalSourceAndConverter().length;
        transformedSources = new SourceAndConverter[nSources];

        // Applies the policy
        for (int i=0;i<nSources;i++) {
            SourceAndConverter sac  = this.starter.getOriginalSourceAndConverter()[i];
            transformedSources[i] = registrationPolicy.apply(transform3D, sac);
        }
    }

    public SourceAndConverter[] getTransformedSources() {
        return transformedSources;
    }
}
