package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Set Number Of Timepoints")
public class BdvSetTimepointsNumberCommand implements Command {
    @Parameter
    BdvHandle[] bdvhs;

    @Parameter
    int numberOfTimePoints;

    public void run() {
        for (BdvHandle bdvh : bdvhs) {
            bdvh.getViewerPanel().setNumTimepoints(numberOfTimePoints);
        }
    }

}
