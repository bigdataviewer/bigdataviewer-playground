package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.source.register.BigWarpLauncher;

import java.util.Arrays;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BigWarp>Launch BigWarp",
        label = "Starts BigWarp from existing sources")
public class BigWarpLauncherCommand implements Command {

    @Parameter
    String bigWarpName;

    @Parameter
    Source[] movingSources;

    @Parameter
    Source[] fixedSources;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvhQ;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvhP;


    @Parameter(type = ItemIO.OUTPUT)
    Source[] warpedSources;

    public void run() {

        // Launch BigWarp
        BigWarpLauncher bwl = new BigWarpLauncher(Arrays.asList(movingSources), Arrays.asList(fixedSources), bigWarpName);
        bwl.run();

        // Output bdvh handles -> will be put in the object service
        bdvhQ = bwl.getBdvHandleQ();
        bdvhP = bwl.getBdvHandleP();

        // TODO
        // BdvHandleHelper.setBdvHandleCloseOperation(bdvhP, );
        warpedSources = new Source[movingSources.length];

        for (int i=0;i<warpedSources.length;i++) {
            warpedSources[i] = bdvhP.getViewerPanel().getState().getSources().get(i).getSpimSource();
        }
    }

}
