package sc.fiji.bdvpg.scijava.converters;

import bdv.viewer.SourceAndConverter;
import org.scijava.convert.AbstractConverter;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Optional;

@Plugin(type = org.scijava.convert.Converter.class)
public class StringToSourceAndConverter<I extends String, O extends SourceAndConverter> extends AbstractConverter<I, O> {

    @Parameter
    ObjectService os;

    @Override
    public <T> T convert(Object src, Class<T> dest) {
        Optional<SourceAndConverter> ans =  os.getObjects(SourceAndConverter.class).stream().filter(sac ->
                (sac.getSpimSource().getName().equals(src))
        ).findFirst();
        if (ans.isPresent()) {
            return (T) ans.get();
        } else {
            return  null;
        }
    }

    @Override
    public Class<O> getOutputType() {
        return (Class<O>) SourceAndConverter.class;
    }

    @Override
    public Class<I> getInputType() {
        return (Class<I>) String.class;
    }
}
