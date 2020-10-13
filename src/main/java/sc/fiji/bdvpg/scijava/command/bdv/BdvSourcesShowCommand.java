package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Show Sources (new Bdv window)",
        description = "Displays one or several sources into a new BDV window")
public class BdvSourcesShowCommand implements Command {

    @Parameter(label="Select Source(s)")
    SourceAndConverter[] sacs;

    @Parameter(label="Auto Contrast")
    boolean autoContrast;

    @Parameter(label="Adjust View on Source")
    boolean adjustViewOnSource;

    @Parameter(label = "Create a 2D BDV window")
    public boolean is2D = false;

    @Parameter(label = "Title of the new BDV window")
    public String windowTitle = "BDV";

    @Parameter(label = "Interpolate")
    public boolean interpolate = false;

    @Parameter(label = "Number of timepoints (1 for a single timepoint)")
    public int nTimepoints = 1;

    /**
     * This triggers: BdvHandlePostprocessor
     */
    @Parameter(type = ItemIO.OUTPUT)
    public BdvHandle bdvh;

    @Parameter(choices = { Projection.MIXED_PROJECTOR, Projection.SUM_PROJECTOR, Projection.AVERAGE_PROJECTOR})
    public String projector;

    @Override
    public void run() {
        BdvWindowCreatorCommand creator = new BdvWindowCreatorCommand();
        creator.interpolate = interpolate;
        creator.projector = projector;
        creator.is2D = is2D;
        creator.nTimepoints = nTimepoints;
        creator.windowTitle = windowTitle;
        creator.run();
        bdvh = creator.bdvh;

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
