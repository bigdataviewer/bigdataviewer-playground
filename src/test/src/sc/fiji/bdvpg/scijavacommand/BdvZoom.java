package sc.fiji.bdvpg.scijavacommand;

import bdv.util.BdvHandle;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

/**
 * Test command to demo {@link sc.fiji.bdvpg.scijava.BdvScijavaHelper}
 */
@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Playground>Zoom Controls")
public class BdvZoom extends InteractiveCommand {

    @Parameter(style = "slider", min = "1.1", max = "4", stepSize = "0.1")
    double zoom_factor;

    @Parameter(callback = "in")
    Button button_in;

    @Parameter(callback = "out")
    Button button_out;

    @Parameter
    BdvHandle bdvh;

    public void run() {
        bdvh.getViewerPanel().showMessage("Zoom factor: "+zoom_factor);
    }

    public void out() {
        bdvh.getViewerPanel().showMessage("Zoom Out");
    }

    public void in() {
        bdvh.getViewerPanel().showMessage("Zoom In");
    }
}
