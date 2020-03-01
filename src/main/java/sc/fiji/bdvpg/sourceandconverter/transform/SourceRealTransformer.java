package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.viewer.Source;
import bdv.img.WarpedSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.RealTransform;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.function.Function;

//NOTE:
// Wrapping the sourceandconverter. If the sourceandconverter is already a transformed sourceandconverter, the transform can be concatenated directly
// But the choice here is to wrap it again
// Another information : the transform is duplicated during the call to setFixedTransform ->
// Transform not passed by reference

public class SourceRealTransformer implements Runnable, Function<SourceAndConverter,SourceAndConverter> {

    SourceAndConverter sourceIn;
    RealTransform rt;
    SourceAndConverter sourceOut;

    public SourceRealTransformer(SourceAndConverter src, RealTransform rt) {
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
            SourceAndConverter vout = new SourceAndConverter<>(vsrc, SourceAndConverterUtils.cloneConverter(in.asVolatile().getConverter()));
            return new SourceAndConverter(src, SourceAndConverterUtils.cloneConverter(in.getConverter()), vout);
        } else {
            return new SourceAndConverter(src, SourceAndConverterUtils.cloneConverter(in.getConverter()));
        }
    }
}
