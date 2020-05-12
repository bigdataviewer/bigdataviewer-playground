package sc.fiji.bdvpg.scijava.converters;

import bdv.viewer.SourceAndConverter;
import org.scijava.convert.AbstractConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.tree.TreePath;


@Plugin(type = org.scijava.convert.Converter.class)
public class StringToSourceAndConverterArray<I extends String> extends AbstractConverter<I, SourceAndConverter[]> {

    @Parameter
    SourceAndConverterService sacsService;

    @Override
    public <T> T convert(Object src, Class<T> dest) {
        System.out.println("Converter called");
        String str = (String) src;
        TreePath tp = sacsService.getUI().getTreePathFromString(str);
        if (tp!=null) {
            return (T) sacsService.getUI().getSourceAndConvertersFromTreePath(tp).toArray(new SourceAndConverter[0]);
        } else {
            return null;
        }

    }

    @Override
    public Class getOutputType() {
        return SourceAndConverter[].class;
    }

    @Override
    public Class<I> getInputType() {
        return (Class<I>) String.class;
    }
}