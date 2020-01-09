package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterDuplicator;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Duplicate Sources")
public class SourcesDuplicatorCommand implements Command {

    @Parameter
    SourceAndConverter[] sources_in;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter[] sources_out;

    @Override
    public void run() {

        sources_out = new SourceAndConverter[sources_in.length];

        for (int i=0;i<sources_in.length;i++) {
            sources_out[i] = new SourceAndConverterDuplicator(sources_in[i]).get();
        }

    }

}
