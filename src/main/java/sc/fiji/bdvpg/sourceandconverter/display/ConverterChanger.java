package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;

import java.util.function.Function;

public class ConverterChanger implements Runnable, Function<SourceAndConverter, SourceAndConverter> {

    SourceAndConverter sac_in;

    SourceAndConverter sac_out;

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
        if (sourceAndConverter.asVolatile()!=null) {
            return new SourceAndConverter(
                    sourceAndConverter.getSpimSource(),
                    nonVolatileConverter,
                    new SourceAndConverter<>(sourceAndConverter.asVolatile().getSpimSource(),volatileConverter)
            );
        } else {
            return new SourceAndConverter(
                    sourceAndConverter.getSpimSource(),
                    nonVolatileConverter);
        }
    }
}
