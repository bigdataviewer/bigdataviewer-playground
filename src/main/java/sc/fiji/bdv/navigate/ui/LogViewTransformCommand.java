package sc.fiji.bdv.navigate.ui;

import bdv.util.BdvHandle;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.navigate.ViewerTransformLogger;
import sc.fiji.bdv.scijava.ScijavaBdvDefaults;
import sc.fiji.bdv.screenshot.ScreenShotMaker;

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
        bdvHandle = BDVSingleton.getInstance();

        new ViewerTransformLogger(bdvHandle).run();
    }
}
