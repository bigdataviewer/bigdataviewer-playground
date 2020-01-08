package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import net.imagej.display.ColorTables;
import net.imagej.lut.LUTService;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.display.ColorTable;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.convert.ConvertService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import org.scijava.util.ColorRGBA;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterDisplayService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Set Source Color")

public class ColorConverterChangerCommand extends DynamicCommand {

    @Parameter
    ColorRGB color;

    @Parameter
    SourceAndConverter source_in;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter source_out;

    @Override
    public void run() {

        ARGBType imglib2color = new ARGBType(ARGBType.rgba(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));

        Converter c = SourceAndConverterUtils.createConverter(source_in.getSpimSource());
        assert c instanceof ColorConverter;
        ((ColorConverter) c).setColor(imglib2color);

        Converter vc =null;
        if (source_in.asVolatile()!=null) {
            vc = SourceAndConverterUtils.createConverter(source_in.asVolatile().getSpimSource());
            ((ColorConverter) vc).setColor(imglib2color);
        }

        ConverterChanger cc = new ConverterChanger(source_in, c, vc);
        cc.run();
        source_out = cc.get();
    }

}
