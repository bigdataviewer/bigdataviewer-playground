package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import sc.fiji.bdvpg.services.BdvService;

import java.util.function.Function;

public class ConverterChanger implements Runnable, Function<SourceAndConverter, SourceAndConverter> {

    SourceAndConverter sac_in;

    Converter nonVolatileConverter;

    Converter volatileConverter;

    public ConverterChanger(SourceAndConverter sac, Converter cvtnv, Converter cvt) {
        sac_in = sac;
        nonVolatileConverter = cvtnv;
        volatileConverter = cvt;
    }

    @Override
    public void run() {
        // Nothing
    }

    public SourceAndConverter get() {
        return apply(sac_in);
    }

    @Override
    public SourceAndConverter apply(SourceAndConverter sourceAndConverter) {
        SourceAndConverter sac;
        if (sourceAndConverter.asVolatile()!=null) {
            sac = new SourceAndConverter(
                    sourceAndConverter.getSpimSource(),
                    nonVolatileConverter,
                    new SourceAndConverter<>(sourceAndConverter.asVolatile().getSpimSource(),volatileConverter)
            );
        } else {
            sac = new SourceAndConverter(
                    sourceAndConverter.getSpimSource(),
                    nonVolatileConverter);
        }
        BdvService.getSourceService().register(sac);
        return sac;
    }
}
