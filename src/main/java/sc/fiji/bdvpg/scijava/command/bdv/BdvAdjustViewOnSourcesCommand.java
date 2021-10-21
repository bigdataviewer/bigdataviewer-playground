package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Adjust view on sources",
        description = "Adjust current Bdv view on the selected sources")
public class BdvAdjustViewOnSourcesCommand  implements BdvPlaygroundActionCommand {

    @Parameter(label="Select Source(s)")
    SourceAndConverter[] sacs;

    @Parameter(label = "Select BDV Window")
    BdvHandle bdvh;

    @Override
    public void run() {
        if (sacs.length>0) {
            new ViewerTransformAdjuster(bdvh, sacs).run();
        }
    }
}
