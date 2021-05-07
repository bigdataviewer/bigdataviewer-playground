package sc.fiji.bdvpg.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;

/**
 * Empty interface which allows to duplicate custom converters in dependent repositories
 */
public interface ICloneableConverter {

    Converter duplicateConverter(SourceAndConverter source);

}
