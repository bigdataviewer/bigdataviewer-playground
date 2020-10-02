package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformLogger;
import sc.fiji.bdvpg.log.Logger;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

/**
 * ViewTransformLoggerCommand
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Log view transform",
        label="Outputs the current view transfrom of a BDV window into the standard IJ logger")

public class ViewTransformLoggerCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter
    LogService ls;

    @Override
    public void run() {
        new ViewerTransformLogger(bdvh, new Logger() {
            @Override
            public void out(String msg) {
                ls.info(msg);
            }

            @Override
            public void err(String msg) {
                ls.error(msg);
            }
        }).run();
    }
}
