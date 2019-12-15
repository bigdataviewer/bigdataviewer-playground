package sc.fiji.bdv.sources.transform;

import bdv.viewer.Source;
import bdv.img.WarpedSource;
import net.imglib2.realtransform.RealTransform;

import java.util.function.Function;

//NOTE:
// Wrapping the source. If the source is already a transformed source, the transforn can be concatenated directly
// But the choice here is to wrap it again
// Another information : the transform is duplicated during the call to setFixedTransform ->
// Transform not passed by reference

public class SourceRealTransform implements Runnable, Function<Source,Source> {

    Source sourceIn;
    RealTransform rt;
    WarpedSource sourceOut;

    public SourceRealTransform(Source src, RealTransform rt) {
        this.sourceIn = src;
        this.rt = rt;
    }

    @Override
    public void run() {
        sourceOut = (WarpedSource) apply(sourceIn);
    }

    public Source getSourceOut() {
        return sourceOut;
    }

    public Source apply(Source in) {
        WarpedSource out = new WarpedSource(in, "Transformed_"+in.getName());
        out.updateTransform(rt);
        out.setIsTransformed(true);
        return out;
    }
}
