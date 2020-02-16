package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Display>Create New Source (Set Color)")
public class ColorSourceCreatorCommand implements Command {

    @Parameter
    ColorRGB color = new ColorRGB(255,255,255);

    @Parameter
    SourceAndConverter[] sources_in;

    @Override
    public void run() {
        for (SourceAndConverter source_in : sources_in) {
            ARGBType imglib2color = new ARGBType(ARGBType.rgba(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));

            Converter c = SourceAndConverterUtils.createConverter(source_in.getSpimSource());
            assert c instanceof ColorConverter;
            ((ColorConverter) c).setColor(imglib2color);

            Converter vc = null;
            if (source_in.asVolatile() != null) {
                vc = SourceAndConverterUtils.createConverter(source_in.asVolatile().getSpimSource());
                ((ColorConverter) vc).setColor(imglib2color);
            }

            ConverterChanger cc = new ConverterChanger(source_in, c, vc);
            cc.run();
            cc.get();
        }
    }

}
