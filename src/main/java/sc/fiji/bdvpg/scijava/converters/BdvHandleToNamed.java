package sc.fiji.bdvpg.scijava.converters;

import bdv.util.BdvHandle;
import org.scijava.Named;
import org.scijava.convert.AbstractConverter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;


@Plugin(type = org.scijava.convert.Converter.class)
public class BdvHandleToNamed extends AbstractConverter<BdvHandle, Named> {

    @Override
    public <T> T convert(Object o, Class<T> aClass) {
        final String name = BdvHandleHelper.getWindowTitle((BdvHandle) o);

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
    public Class<BdvHandle> getInputType() {
        return BdvHandle.class;
    }
}
