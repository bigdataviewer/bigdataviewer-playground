package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.util.ResampledSource;
import bdv.util.VolatileSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.function.Function;

public class SourceResampler implements Runnable, Function<SourceAndConverter, SourceAndConverter> {

    SourceAndConverter sac_in;

    SourceAndConverter model;

    boolean reuseMipMaps;

    boolean interpolate;

    boolean cache;

    public SourceResampler(SourceAndConverter sac_in, SourceAndConverter model, boolean reuseMipmaps, boolean cache, boolean interpolate) {
        this.reuseMipMaps = reuseMipmaps;
        this.model = model;
        this.sac_in = sac_in;
        this.interpolate = interpolate;
        this.cache = cache;
    }

    @Override
    public void run() {

    }

    public SourceAndConverter get() {
        return apply(sac_in);
    }

    @Override
    public SourceAndConverter apply(SourceAndConverter src) {
        Source srcRsampled =
                new ResampledSource(
                        src.getSpimSource(),
                        model.getSpimSource(),
                        reuseMipMaps,
                        cache,
                        interpolate);

        SourceAndConverter sac;
        if (src.asVolatile()!=null) {
            SourceAndConverter vsac;
            Source vsrcRsampled;
            if (cache) {
                vsrcRsampled = new VolatileSource(srcRsampled);
            } else {
                vsrcRsampled = new ResampledSource(
                        src.asVolatile().getSpimSource(),
                        model.getSpimSource(),
                        reuseMipMaps,
                        false,
                        interpolate);
            }
            vsac = new SourceAndConverter(vsrcRsampled,
                    SourceAndConverterUtils.cloneConverter(src.asVolatile().getConverter(), src.asVolatile()));
            sac = new SourceAndConverter<>(srcRsampled,
                    SourceAndConverterUtils.cloneConverter(src.getConverter(), src),vsac);
        } else {
            sac = new SourceAndConverter<>(srcRsampled,
                    SourceAndConverterUtils.cloneConverter(src.getConverter(), src));
        }



        return sac;
    }
}
