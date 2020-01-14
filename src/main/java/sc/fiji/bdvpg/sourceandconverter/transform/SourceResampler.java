package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.function.Function;

public class SourceResampler implements Runnable, Function<SourceAndConverter, SourceAndConverter> {

    SourceAndConverter sac_in;

    SourceAndConverter model;

    boolean reuseMipMaps;

    public SourceResampler(SourceAndConverter sac_in, SourceAndConverter model, boolean reuseMipmaps) {
        this.reuseMipMaps = reuseMipmaps;
        this.model = model;
        this.sac_in = sac_in;
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
                        reuseMipMaps);

        SourceAndConverter sac;
        if (src.asVolatile()!=null) {
            SourceAndConverter vsac;
            Source vsrcRsampled =
                    new ResampledSource(
                            src.asVolatile().getSpimSource(),
                            model.getSpimSource(),
                            reuseMipMaps);
            vsac = new SourceAndConverter(vsrcRsampled,
                    SourceAndConverterUtils.cloneConverter(src.asVolatile().getConverter()));
            sac = new SourceAndConverter<>(srcRsampled,
                    SourceAndConverterUtils.cloneConverter(src.getConverter()),vsac);
        } else {
            sac = new SourceAndConverter<>(srcRsampled,
                    SourceAndConverterUtils.cloneConverter(src.getConverter()));
        }

        BdvService.getSourceService().register(sac);

        return sac;
    }
}
