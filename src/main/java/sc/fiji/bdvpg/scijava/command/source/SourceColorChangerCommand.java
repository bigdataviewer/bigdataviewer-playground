package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Set Sources Color")
public class SourceColorChangerCommand implements Command {

    @Parameter
    ColorRGB color = new ColorRGB(255,255,255);

    @Parameter
    SourceAndConverter[] sources_in;

    @Override
    public void run() {
        ARGBType imglib2color = new ARGBType(ARGBType.rgba(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));
        for (SourceAndConverter sac : sources_in) {
            new ColorChanger(sac, imglib2color).run();
        }
    }

}
