package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>Show Sources In Multiple BDV Windows")
public class MultiBdvSourcesAdderCommand implements Command {

    @Parameter(label = "Select BDV Windows")
    BdvHandle[] bdvhs;

    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sacs;

    @Override
    public void run() {
        for (BdvHandle bdvh : bdvhs) {
            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvh, sacs);
        }
    }
}
