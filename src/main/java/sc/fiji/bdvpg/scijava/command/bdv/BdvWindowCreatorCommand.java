package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.*;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.projector.AccumulateAverageProjectorARGB;
import sc.fiji.bdvpg.bdv.projector.AccumulateMixedProjectorARGB;
import sc.fiji.bdvpg.bdv.projector.ProjectionTypes;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Create Empty BDV Frame",
    label = "Creates an empty Bdv window")
public class BdvWindowCreatorCommand implements Command {

    @Parameter(label = "Create a 2D Bdv window")
    public boolean is2D = false;

    @Parameter(label = "Title of the new Bdv window")
    public String windowTitle = "Bdv";

    @Parameter(label = "Interpolate")
    public boolean interpolate = false;

    /**
     * This triggers: BdvHandlePostprocessor
     */
    @Parameter(type = ItemIO.OUTPUT)
    public BdvHandle bdvh;

    @Parameter(choices = { ProjectionTypes.MIXED_PROJECTOR, ProjectionTypes.SUM_PROJECTOR, ProjectionTypes.AVERAGE_PROJECTOR})
    public String projector;

    @Override
    public void run() {
        //------------ BdvHandleFrame
        BdvOptions opts = BdvOptions.options().frameTitle(windowTitle);
        if (is2D) opts = opts.is2D();

        switch (projector) {
            case ProjectionTypes.MIXED_PROJECTOR:
                opts = opts.accumulateProjectorFactory(AccumulateMixedProjectorARGB.factory);
            case ProjectionTypes.SUM_PROJECTOR:
                // Default projector
                break;
            case ProjectionTypes.AVERAGE_PROJECTOR:
                opts = opts.accumulateProjectorFactory(AccumulateAverageProjectorARGB.factory);
                break;
            default:
        }

        BdvCreator creator = new BdvCreator(opts, interpolate);
        creator.run();
        bdvh = creator.get();
    }
}
