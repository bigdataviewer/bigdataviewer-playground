package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.log.SystemLogger;
import sc.fiji.bdvpg.services.SacServices;

import java.util.function.Consumer;

/**
 * In contrast to ConverterChanger, this action do not create a new SourceAndConverter to change the color
 * of the displayed source. However this means that the converter has to be of instance ColorConverter
 * One way around this is to create more generic converters from the beginning but this would have drawbacks:
 * - how to save the converter in spimdata ?
 */
public class ColorChanger implements Runnable, Consumer<SourceAndConverter> {

    SourceAndConverter sac;
    ARGBType color;

    public ColorChanger(SourceAndConverter sac, ARGBType color) {
        this.sac = sac;
        this.color = color;
    }

    @Override
    public void run() {
        accept(sac);
    }

    @Override
    public void accept(SourceAndConverter sourceAndConverter) {
        if (sourceAndConverter.getConverter() instanceof ColorConverter) {
            ((ColorConverter) sourceAndConverter.getConverter()).setColor(color);
            if (sourceAndConverter.asVolatile()!=null) {
                ((ColorConverter) sourceAndConverter.asVolatile().getConverter()).setColor(color);
            }
            // Updates display, if any
            if ( SacServices.getSacDisplayService()!=null)
                SacServices.getSacDisplayService().getConverterSetup(sourceAndConverter).setColor(color);
        } else {
            new SystemLogger().err("sourceAndConverter Converter is not an instance of Color Converter");
        }
    }
}
