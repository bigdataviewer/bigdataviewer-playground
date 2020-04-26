package sc.fiji.bdvpg.scijava.converters;

import bdv.util.BdvHandle;
import com.google.gson.Gson;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.util.stream.Stream;

@Plugin(type = org.scijava.convert.Converter.class)
public class StringToBdvHandleArray extends AbstractConverter<String, BdvHandle[]> {

    @Parameter
    ConvertService cs;

    @Override
    public <T> T convert(Object src, Class<T> dest) {
        Gson gson = new Gson();
        String[] bdvhsNames = gson.fromJson((String) src, String[].class);
        return (T) Stream.of(bdvhsNames).map(name -> cs.convert(name, BdvHandle.class)).toArray(BdvHandle[]::new);
    }

    @Override
    public Class<BdvHandle[]> getOutputType() {
        return BdvHandle[].class;
    }

    @Override
    public Class<String> getInputType() {
        return String.class;
    }
}
