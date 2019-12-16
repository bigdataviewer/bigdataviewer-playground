package sc.fiji.bdv.screenshot.ui;

import bdv.util.BdvHandle;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.scijava.ScijavaBdvDefaults;
import sc.fiji.bdv.screenshot.ScreenShotMaker;

/**
 * ScreenShotCommand
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Screenshot")
public class ScreenShotCommand implements Command {

    @Parameter
    BdvHandle bdvHandle;

    @Parameter
    public double targetPixelSizeInXY = 1;

    @Parameter
    public String targetPixelUnit = "Pixels";

    @Override
    public void run() {
        ScreenShotMaker screenShotMaker = new ScreenShotMaker(bdvHandle);
        screenShotMaker.setPhysicalPixelSpacingInXY(targetPixelSizeInXY, targetPixelUnit);
        ImagePlus image = screenShotMaker.getScreenshot();
        image.show();
    }
}
