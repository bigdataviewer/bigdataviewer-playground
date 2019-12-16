package sc.fiji.bdvpg.sourceandconverter;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;

import java.util.function.Function;

/**
 * Applies a source function (like affine transform) on a SourceAndConverter object
 * This allows to use the same converter ( TODO : check restriction on identical type to transfer converter)
 * And to perform the same function on both the concrete source and on the volatile source, if present
 */
public class SourceAndConverterApplySourceFunction implements Runnable, Function<SourceAndConverter, SourceAndConverter> {

    SourceAndConverter sacIn;
    Function<Source, Source> f;
    SourceAndConverter sacOut;

    public SourceAndConverterApplySourceFunction(SourceAndConverter sac, Function<Source, Source> f) {
        this.sacIn = sac;
        this.f = f;
    }

    public void run() {
        sacOut = apply(sacIn);
    }

    public SourceAndConverter getSourceAndConverterOut() {
        return sacOut;
    }

    @Override
    public SourceAndConverter apply(SourceAndConverter in) {
        Source src = f.apply(in.getSpimSource());
        if (in.asVolatile()!=null) {
            Source vsrc = f.apply(in.asVolatile().getSpimSource());
            SourceAndConverter vout = new SourceAndConverter<>(vsrc, in.asVolatile().getConverter());
            return new SourceAndConverter(src, in.getConverter(), vout);
        } else {
            return new SourceAndConverter(src, in.getConverter());
        }
    }
}
