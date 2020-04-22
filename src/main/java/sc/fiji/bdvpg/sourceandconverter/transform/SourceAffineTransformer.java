package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.function.Function;

/**
 * This action aaplies an AffineTransform onto a SourceAndConverter
 * Both the non volatile and the volatile spimsource, if present, are wrapped
 * Another option could be to check whether the spimsource are already wrapped, and then concatenate the transforms
 * TODO : write this alternative action, or set a transform in place flag in this action
 * Limitation : the affine transform is identical for all timepoints
 *
 * Note : the converters are cloned during this wrapping
 * Another option could have been to use the same converters
 * the transform is passed by value, not by reference, so it cannot be updated later on
 */


public class SourceAffineTransformer implements Runnable, Function<SourceAndConverter, SourceAndConverter> {

    SourceAndConverter sourceIn;
    AffineTransform3D at3D;
    SourceAndConverter sourceOut;

    public SourceAffineTransformer(SourceAndConverter src, AffineTransform3D at3D) {
        this.sourceIn = src;
        this.at3D = at3D;
    }

    @Override
    public void run() {
       sourceOut = apply(sourceIn);
    }

    public SourceAndConverter getSourceOut() {
        return apply(sourceIn);//sourceOut;
    }

    public SourceAndConverter apply(SourceAndConverter in) {
        SourceAndConverter sac;
        TransformedSource src = new TransformedSource(in.getSpimSource());
        src.setFixedTransform(at3D);
        if (in.asVolatile()!=null) {
            TransformedSource vsrc = new TransformedSource(in.asVolatile().getSpimSource(), src);
            SourceAndConverter vout = new SourceAndConverter<>(vsrc, SourceAndConverterUtils.cloneConverter(in.asVolatile().getConverter()));
            sac = new SourceAndConverter<>(src, SourceAndConverterUtils.cloneConverter(in.getConverter()), vout);
        } else {
            sac = new SourceAndConverter<>(src, SourceAndConverterUtils.cloneConverter(in.getConverter()));
        }
        //SourceAndConverterServices.getSourceAndConverterService().register(sac);
        return sac;
    }
}
