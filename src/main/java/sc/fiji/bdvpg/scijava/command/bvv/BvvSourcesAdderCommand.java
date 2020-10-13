package sc.fiji.bdvpg.scijava.command.bvv;

import bdv.viewer.SourceAndConverter;
import bvv.util.BvvHandle;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bvv.BvvViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BVV>Show Sources in BVV",
    description = "Show sources in a BigVolumeViewer window - limited to 16 bit images")
public class BvvSourcesAdderCommand implements Command {

    @Parameter(label = "Select BVV Window(s)")
    BvvHandle bvvh;

    @Parameter(label="Adjust View on Source")
    boolean adjustViewOnSource;

    @Parameter(label = "Select source(s)")
    SourceAndConverter[] sacs;

    @Override
    public void run() {

        for (SourceAndConverter sac : sacs) {
            bvvh.getViewerPanel().addSource(sac, SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(sac));
        }

        if ((adjustViewOnSource) && (sacs.length>0)) {
            new BvvViewerTransformAdjuster(bvvh, sacs[0]).run();
        }

    }
}
