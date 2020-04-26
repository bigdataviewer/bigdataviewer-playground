package sc.fiji.bdvpg.scijava.converters;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import org.scijava.Named;
import org.scijava.convert.AbstractConverter;
import org.scijava.plugin.Plugin;

import java.util.stream.Stream;

@Plugin(type = org.scijava.convert.Converter.class)
public class SourceAndConverterArrayToNamed extends AbstractConverter<SourceAndConverter[], Named> {

    @Override
    public <T> T convert(Object o, Class<T> aClass) {
        if (o instanceof SourceAndConverter[]) {

            String[] sacsNames = Stream.of((SourceAndConverter[]) o).map(sac -> sac.getSpimSource().getName()).toArray(String[]::new);
            // Honestly annoying and not efficient, but hopefully escaped character safe
            final String name = new Gson().toJson(sacsNames);

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
        } else return null;
    }

    @Override
    public Class<Named> getOutputType() {
        return Named.class;
    }

    @Override
    public Class<SourceAndConverter[]> getInputType() {
        return SourceAndConverter[].class;
    }
}
