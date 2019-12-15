package sc.fiji.bdv.sources.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.function.Function;

//NOTE:
// Wrapping the source. If the source is already a transformed source, the transforn can be concatenated directly
// But the choice here is to wrap it again
// Another information : the transform is duplicated during the call to setFixedTransform ->
// Transform not passed by reference

public class SourceAffineTransform implements Runnable, Function<Source, Source> {

    Source sourceIn;
    AffineTransform3D at3D;
    TransformedSource sourceOut;

    public SourceAffineTransform(Source src, AffineTransform3D at3D) {
        this.sourceIn = src;
        this.at3D = at3D;
    }

    @Override
    public void run() {
       sourceOut = (TransformedSource) apply(sourceIn);
    }

    public Source getSourceOut() {
        return sourceOut;
    }

    public Source apply(Source in) {
        TransformedSource out = new TransformedSource(in);
        out.setFixedTransform(at3D);
        return out;
    }
}
