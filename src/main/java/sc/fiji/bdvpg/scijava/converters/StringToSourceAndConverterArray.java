package sc.fiji.bdvpg.scijava.converters;

import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import org.scijava.convert.AbstractConverter;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

<<<<<<< HEAD
import java.util.stream.Stream;
=======
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

>>>>>>> master

@Plugin(type = org.scijava.convert.Converter.class)
public class StringToSourceAndConverterArray extends AbstractConverter<String, SourceAndConverter[]> {

    @Parameter
    ConvertService cs;

    @Parameter
    LogService ls;

    @Parameter
    SourceAndConverterService sacsService;

    @Override
    public <T> T convert(Object src, Class<T> dest) {
        // From a gson list
        Gson gson = new Gson();
        try {
            String[] sacsNames = gson.fromJson((String) src, String[].class);
            return (T) Stream.of(sacsNames).map(name -> cs.convert(name, SourceAndConverter.class)).toArray(SourceAndConverter[]::new);
        } catch (Exception e1) {
            try {
                // Because IJ1 unboxing removes brackets. Just redo an attempt with brackets
                String[] sacsNames = gson.fromJson("["+src+"]", String[].class);
                return (T) Stream.of(sacsNames).map(name -> cs.convert(name, SourceAndConverter.class)).toArray(SourceAndConverter[]::new);
            } catch (Exception e2) {
                // For debugging
                // ls.error("Could not convert "+src+" to an array of BdvHandle");
                return null;
            }
        }

        // Todo : from a path in the tree view
        /*
         System.out.println("Converter called");
        String str = (String) src;
        TreePath tp = sacsService.getUI().getTreePathFromString(str);
        if (tp!=null) {
            return (T) sacsService.getUI().getSourceAndConvertersFromTreePath(tp).toArray(new SourceAndConverter[0]);//sacsService.getUI().getSourceAndConvertersFromTreePath(tp).toArray(new SourceAndConverter[0]);
        } else {
            return null;
        }
         */
    }

    @Override
    public Class getOutputType() {
        return SourceAndConverter[].class;
    }

    @Override
    public Class getInputType() {
        return String.class;
    }
}
