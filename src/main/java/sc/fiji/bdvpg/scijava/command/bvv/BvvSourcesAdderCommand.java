package sc.fiji.bdvpg.scijava.command.bvv;

import bdv.viewer.SourceAndConverter;
import bvv.util.BvvHandle;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bvv.BvvViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BVV>Show Sources in BVV")
public class BvvSourcesAdderCommand implements Command {

    @Parameter
    BvvHandle bvvh;

    @Parameter
    boolean adjustViewOnSource;

    @Parameter
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
