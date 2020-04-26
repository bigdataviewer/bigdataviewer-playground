package sc.fiji.bdvpg.scijava.converters;

import bdv.util.BdvHandle;
import org.scijava.convert.AbstractConverter;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;

import java.util.Optional;

@Plugin(type = org.scijava.convert.Converter.class)
public class StringToBdvHandle<I extends String, O extends BdvHandle> extends AbstractConverter<I, O> {
    @Parameter
    ObjectService os;

    @Override
    public <T> T convert(Object src, Class<T> dest) {
        Optional<BdvHandle> ans =  os.getObjects(BdvHandle.class).stream().filter(bdvh ->
                (BdvHandleHelper.getWindowTitle(bdvh).equals(src))
        ).findFirst();
        if (ans.isPresent()) {
            return (T) ans.get();
        } else {
            return  null;
        }
    }

    @Override
    public Class<O> getOutputType() {
        return (Class<O>) BdvHandle.class;
    }

    @Override
    public Class<I> getInputType() {
        return (Class<I>) String.class;
    }
}
