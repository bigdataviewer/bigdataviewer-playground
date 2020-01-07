package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.viewer.Source;
import bdv.img.WarpedSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.RealTransform;

import java.util.function.Function;

//NOTE:
// Wrapping the source. If the source is already a transformed source, the transform can be concatenated directly
// But the choice here is to wrap it again
// Another information : the transform is duplicated during the call to setFixedTransform ->
// Transform not passed by reference

public class SourceRealTransform implements Runnable, Function<SourceAndConverter,SourceAndConverter> {

    SourceAndConverter sourceIn;
    RealTransform rt;
    SourceAndConverter sourceOut;

    public SourceRealTransform(SourceAndConverter src, RealTransform rt) {
        this.sourceIn = src;
        this.rt = rt;
    }

    @Override
    public void run() {
        sourceOut = apply(sourceIn);
    }

    public SourceAndConverter getSourceOut() {
        return sourceOut;
    }

    public SourceAndConverter apply(SourceAndConverter in) {
        WarpedSource src = new WarpedSource(in.getSpimSource(), "Transformed_"+in.getSpimSource().getName());
        src.updateTransform(rt);
        src.setIsTransformed(true);
        if (in.asVolatile()!=null) {
            WarpedSource vsrc = new WarpedSource(in.asVolatile().getSpimSource(), "Transformed_"+in.asVolatile().getSpimSource().getName());//f.apply(in.asVolatile().getSpimSource());
            vsrc.updateTransform(rt);
            vsrc.setIsTransformed(true);
            SourceAndConverter vout = new SourceAndConverter<>(vsrc, in.asVolatile().getConverter());
            return new SourceAndConverter(src, in.getConverter(), vout);
        } else {
            return new SourceAndConverter(src, in.getConverter());
        }
    }
}
