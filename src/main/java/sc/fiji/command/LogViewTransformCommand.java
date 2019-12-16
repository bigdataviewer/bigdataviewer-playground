package sc.fiji.command;

import bdv.util.BdvHandle;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdv.navigate.ViewerTransformLogger;
import sc.fiji.scijava.ScijavaBdvDefaults;

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
    BdvHandle bdvHandle;

    @Override
    public void run() {
        new ViewerTransformLogger(bdvHandle).run();
    }
}
