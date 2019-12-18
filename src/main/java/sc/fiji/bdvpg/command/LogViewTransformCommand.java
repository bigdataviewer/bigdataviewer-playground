package sc.fiji.bdvpg.command;

import bdv.util.BdvHandle;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformLogger;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

/**
 * LogViewTransformCommand
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Log view transform")
public class LogViewTransformCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Override
    public void run() {
        new ViewerTransformLogger(bdvh).run();
    }
}
