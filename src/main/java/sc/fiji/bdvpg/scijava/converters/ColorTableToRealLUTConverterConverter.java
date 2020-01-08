package sc.fiji.bdvpg.scijava.converters;

import net.imglib2.converter.Converter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorTable;
import org.scijava.convert.AbstractConverter;
import org.scijava.plugin.Plugin;

@Plugin(type = org.scijava.convert.Converter.class)
public class ColorTableToRealLUTConverterConverter<I extends ColorTable, O extends Converter> extends AbstractConverter<I, O> {
    @Override
    public <T> T convert(Object src, Class<T> dest) {
        ColorTable ct = (ColorTable) src;
        return (T) new RealLUTConverter<>(0,255,ct);
    }

    @Override
    public Class<O> getOutputType() {
        return (Class<O>) RealLUTConverter.class;
    }

    @Override
    public Class<I> getInputType() {
        return (Class<I>) ColorTable.class;
    }
}
