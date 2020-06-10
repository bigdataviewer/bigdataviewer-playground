package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.display.ProjectionModeChanger;

import static sc.fiji.bdvpg.bdv.projector.Projection.PROJECTION_MODE_AVG;
import static sc.fiji.bdvpg.bdv.projector.Projection.PROJECTION_MODE_SUM;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Display>Set Sources Projection Mode")
public class SourceAndConverterProjectionModeChangerCommand implements Command {

    @Parameter(label = "Projection Mode", choices = { PROJECTION_MODE_SUM, PROJECTION_MODE_AVG })
    String projectionMode = PROJECTION_MODE_SUM;

    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sacs;

    @Parameter(label="Add Source(s) to occluding layer")
    boolean addToOccludingLayer = false;

    @Override
    public void run() {
        new ProjectionModeChanger( sacs, projectionMode, addToOccludingLayer ).run();
    }

}
