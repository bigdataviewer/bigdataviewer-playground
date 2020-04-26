package sc.fiji.bdvpg.scijava.converters;

import bdv.viewer.SourceAndConverter;
import org.scijava.Named;
import org.scijava.convert.AbstractConverter;
import org.scijava.plugin.Plugin;

@Plugin(type = org.scijava.convert.Converter.class)
public class SourceAndConverterToNamed extends AbstractConverter<SourceAndConverter, Named> {

    @Override
    public <T> T convert(Object o, Class<T> aClass) {
        final String name = ((SourceAndConverter)o).getSpimSource().getName();
        Named boxedNamedObject = new Named() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public void setName(String name) {
                throw new UnsupportedOperationException();
            }
        };
        return (T) boxedNamedObject;
    }

    @Override
    public Class<Named> getOutputType() {
        return Named.class;
    }

    @Override
    public Class<SourceAndConverter> getInputType() {
        return SourceAndConverter.class;
    }
}
