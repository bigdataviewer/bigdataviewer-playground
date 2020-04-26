package sc.fiji.bdvpg.scijava.converters;

import bdv.util.BdvHandle;
import com.google.gson.Gson;
import org.scijava.Named;
import org.scijava.convert.AbstractConverter;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

@Plugin(type = org.scijava.convert.Converter.class)
public class BdvHandleArrayToNamed extends AbstractConverter<BdvHandle[], Named> {

    @Override
    public <T> T convert(Object o, Class<T> aClass) {
        if (o instanceof BdvHandle[]) {
            BdvHandle[] bdvhs = (BdvHandle[]) o;
            for (BdvHandle bdvh : bdvhs) {
                System.out.println(BdvHandleHelper.getWindowTitle(bdvh));
            }
            // Honestly annoying and not efficient, but hopefully escaped character safe
            String[] bdvhsNames = Stream.of(bdvhs).map(BdvHandleHelper::getWindowTitle).toArray(String[]::new);
            final String name = new Gson().toJson(bdvhsNames);

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
    public Class<BdvHandle[]> getInputType() {
        return BdvHandle[].class;
    }
}
