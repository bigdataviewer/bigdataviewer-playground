package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.function.Function;

//NOTE:
// Wrapping the sourceandconverter. If the sourceandconverter is already a transformed sourceandconverter, the transform can be concatenated directly
// But the choice here is to wrap it again
// Another information : the transform is duplicated during the call to setFixedTransform ->
// Transform not passed by reference

/**
 * THIS FAILS FOR VOLATILE VIEW AND MANUAL TRANSFORMS! TODO : FIX THAT!
 */

public class SourceAffineTransform implements Runnable, Function<SourceAndConverter, SourceAndConverter> {

    SourceAndConverter sourceIn;
    AffineTransform3D at3D;
    SourceAndConverter sourceOut;

    public SourceAffineTransform(SourceAndConverter src, AffineTransform3D at3D) {
        this.sourceIn = src;
        this.at3D = at3D;
    }

    @Override
    public void run() {
       sourceOut = apply(sourceIn);
    }

    public SourceAndConverter getSourceOut() {
        return sourceOut;
    }

    public SourceAndConverter apply(SourceAndConverter in) {
        TransformedSource src = new TransformedSource(in.getSpimSource());
        src.setFixedTransform(at3D);
        if (in.asVolatile()!=null) {
            TransformedSource vsrc = new TransformedSource(in.asVolatile().getSpimSource(), src);
            SourceAndConverter vout = new SourceAndConverter<>(vsrc, SourceAndConverterUtils.cloneConverter(in.asVolatile().getConverter()));
            return new SourceAndConverter<>(src, SourceAndConverterUtils.cloneConverter(in.getConverter()), vout);
        } else {
            return new SourceAndConverter<>(src, SourceAndConverterUtils.cloneConverter(in.getConverter()));
        }
    }
}
