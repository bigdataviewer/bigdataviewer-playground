package sc.fiji.bdvpg.sourceandconverter.importer;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.function.Function;

public class SourceAndConverterDuplicator implements Runnable, Function<SourceAndConverter, SourceAndConverter> {

    SourceAndConverter sac_in;

    public SourceAndConverterDuplicator(SourceAndConverter sac) {
        sac_in = sac;
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
        if (sourceAndConverter.asVolatile() != null) {
            sac = new SourceAndConverter(
                    sourceAndConverter.getSpimSource(),
                    SourceAndConverterUtils.cloneConverter(sourceAndConverter.getConverter()),
                    new SourceAndConverter(sourceAndConverter.asVolatile().getSpimSource(),
                            SourceAndConverterUtils.cloneConverter(sourceAndConverter.asVolatile().getConverter()))
            );
        } else {
            sac = new SourceAndConverter(
                    sourceAndConverter.getSpimSource(),
                    SourceAndConverterUtils.cloneConverter(sourceAndConverter.getConverter()));
        }
        SourceAndConverterServices.getSourceAndConverterService().register(sac);
        return sac;
    }

}