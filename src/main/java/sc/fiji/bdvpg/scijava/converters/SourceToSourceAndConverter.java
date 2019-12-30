package sc.fiji.bdvpg.scijava.converters;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import org.scijava.convert.AbstractConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;

@Plugin(type = org.scijava.convert.Converter.class)
public class SourceToSourceAndConverter<I extends Source, O extends SourceAndConverter> extends AbstractConverter<I, O> {
    @Parameter
    BdvSourceDisplayService bsds;

    @Override
    public <T> T convert(Object src, Class<T> dest) {
        return (T) bsds.getSourceAndConverter((Source) src);
    }

    @Override
    public Class<O> getOutputType() {
        return (Class<O>) SourceAndConverter.class;
    }

    @Override
    public Class<I> getInputType() {
        return (Class<I>) Source.class;
    }
}
