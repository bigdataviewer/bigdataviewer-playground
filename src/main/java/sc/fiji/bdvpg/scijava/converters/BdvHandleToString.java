package sc.fiji.bdvpg.scijava.converters;

import bdv.util.BdvHandle;
import org.scijava.convert.AbstractConverter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;


@Plugin(type = org.scijava.convert.Converter.class)
public class BdvHandleToString extends AbstractConverter<BdvHandle, String> {

    @Override
    public <T> T convert(Object o, Class<T> aClass) {
        return (T) BdvHandleHelper.getWindowTitle((BdvHandle) o);
    }

    @Override
    public Class<String> getOutputType() {
        return String.class;
    }

    @Override
    public Class<BdvHandle> getInputType() {
        return BdvHandle.class;
    }
}
