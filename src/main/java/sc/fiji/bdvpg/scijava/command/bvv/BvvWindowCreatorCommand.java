package sc.fiji.bdvpg.scijava.command.bvv;

import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.render.AccumulateProjectorFactory;
import bvv.util.BvvHandle;
import bvv.util.BvvOptions;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.projector.AccumulateAverageProjectorARGB;
import sc.fiji.bdvpg.bdv.projector.AccumulateMixedProjectorARGBFactory;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.bvv.BvvCreator;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bvv>Create Empty BVV Frame",
    label = "Creates an empty Bdv window")
public class BvvWindowCreatorCommand implements Command {

    @Parameter(label = "Title of the new Bvv window")
    public String windowTitle = "Bvv";

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
