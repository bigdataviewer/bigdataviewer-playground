package sc.fiji.bdvpg.bdv;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.function.BiFunction;

public class ManualRegistrationStopper implements Runnable {

    ManualRegistrationStarter starter;

    BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter> registrationPolicy;// = ManualRegistrationStopper::createNewTransformedSourceAndConverter;

    SourceAndConverter[] transformedSources;

    public static SourceAndConverter createNewTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        SourceAndConverter transformedSac = new SourceAffineTransformer(sac, affineTransform3D).getSourceOut();
        // Not completely safe : we assume the active bdv is the one selected
        BdvService.getSourceAndConverterDisplayService().show(transformedSac);
        return transformedSac;
    }

    public static SourceAndConverter mutateTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert sac.getSpimSource() instanceof TransformedSource;
        AffineTransform3D at3D = new AffineTransform3D();
        ((TransformedSource)sac.getSpimSource()).getFixedTransform(at3D);
        ((TransformedSource)sac.getSpimSource()).setFixedTransform(at3D.preConcatenate(affineTransform3D));
        // Not completely safe : we assume the active bdv is the one selected
        BdvService.getSourceAndConverterDisplayService().show(sac);
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
