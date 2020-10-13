package sc.fiji.bdvpg.scijava.command.bvv;

import bvv.util.BvvHandle;
import bvv.util.BvvOptions;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bvv.BvvCreator;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BVV>Create Empty BVV Frame",
    description = "Creates an empty Bdv window")
public class BvvWindowCreatorCommand implements Command {

    @Parameter(label = "Title of the new BVV window")
    public String windowTitle = "BVV";

    @Parameter(label = "Number of timepoints (1 for a single timepoint)")
    public int nTimepoints = 1;

    /**
     * TODO This triggers: BvvHandlePostprocessor
     */
    @Parameter(type = ItemIO.OUTPUT)
    public BvvHandle bvvh;

    @Override
    public void run() {
        //------------ BdvHandleFrame
        BvvOptions opts = BvvOptions.options().frameTitle(windowTitle);

        BvvCreator creator = new BvvCreator(opts, nTimepoints);
        creator.run();
        bvvh = creator.get();
    }
}
