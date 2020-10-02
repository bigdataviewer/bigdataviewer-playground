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

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Show Sources",
        label = "Adds one or several sources to an existing BDV window")
public class BdvSourcesAdderCommand implements Command {

    @Parameter(label="Select BDV Window")
    BdvHandle bdvh;

    @Parameter(label="Select Source(s)")
    SourceAndConverter[] sacs;

    @Parameter(label="Auto Contrast")
    boolean autoContrast;

    @Parameter(label="Adjust View on Source")
    boolean adjustViewOnSource;

    @Override
    public void run() {

        SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvh, sacs);
        if (autoContrast) {
            for (SourceAndConverter sac : sacs) {
                int timepoint = bdvh.getViewerPanel().getState().getCurrentTimepoint();
                new BrightnessAutoAdjuster(sac, timepoint).run();
            }
        }

        if ((adjustViewOnSource) && (sacs.length>0)) {
            new ViewerTransformAdjuster(bdvh, sacs[0]).run();
        }
    }
}
