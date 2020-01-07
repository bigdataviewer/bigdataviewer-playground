package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.function.Function;

//NOTE:
// Wrapping the source. If the source is already a transformed source, the transform can be concatenated directly
// But the choice here is to wrap it again
// Another information : the transform is duplicated during the call to setFixedTransform ->
// Transform not passed by reference

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
        TransformedSource src = new TransformedSource(in.getSpimSource());//f.apply(in.getSpimSource());
        src.setFixedTransform(at3D);
        if (in.asVolatile()!=null) {
            TransformedSource vsrc = new TransformedSource(in.asVolatile().getSpimSource());//f.apply(in.asVolatile().getSpimSource());
            vsrc.setFixedTransform(at3D);
            SourceAndConverter vout = new SourceAndConverter<>(vsrc, in.asVolatile().getConverter());
            return new SourceAndConverter(src, in.getConverter(), vout);
        } else {
            return new SourceAndConverter(src, in.getConverter());
        }
    }
}
