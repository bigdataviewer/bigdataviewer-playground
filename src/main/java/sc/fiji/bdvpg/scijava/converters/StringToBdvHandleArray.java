package sc.fiji.bdvpg.scijava.converters;

import bdv.util.BdvHandle;
import com.google.gson.Gson;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.util.stream.Stream;

@Plugin(type = org.scijava.convert.Converter.class)
public class StringToBdvHandleArray extends AbstractConverter<String, BdvHandle[]> {

    @Parameter
    ConvertService cs;

    @Parameter
    LogService ls;

    @Override
    public <T> T convert(Object src, Class<T> dest) {
        Gson gson = new Gson();
        try {
            String[] bdvhsNames = gson.fromJson((String) src, String[].class);
            return (T) Stream.of(bdvhsNames).map(name -> cs.convert(name, BdvHandle.class)).toArray(BdvHandle[]::new);
        } catch (Exception e1) {
            try {
                // Because IJ1 unboxing removes brackets. Just redo an attempt with brackets
                String[] bdvhsNames = gson.fromJson("["+src+"]", String[].class);
                return (T) Stream.of(bdvhsNames).map(name -> cs.convert(name, BdvHandle.class)).toArray(BdvHandle[]::new);
            } catch (Exception e2) {
                // For debugging
                // ls.error("Could not convert "+src+" to an array of BdvHandle");
                return null;
            }
        }
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
