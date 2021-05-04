package sc.fiji.bdvpg.scijavacommand;

import bdv.util.BdvHandle;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;

/**
 * Test command to demo {@link sc.fiji.bdvpg.scijava.BdvScijavaHelper}
 */
@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Playground>Another sub menu>Rename Bdv Window")
public class RenameBdv implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter(label = "New Title")
    String title;

    @Override
    public void run() {
        BdvHandleHelper.setWindowTitle(bdvh, title);
    }
}
