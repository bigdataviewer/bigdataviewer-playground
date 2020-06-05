package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.ScreenShotMaker;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
/**
 * ScreenShotMakerCommand
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>Screenshot")
public class ScreenShotMakerCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter
    public double targetPixelSizeInXY = 1;

    @Parameter
    public String targetPixelUnit = "Pixels";

    @Parameter
    public boolean showRawData = false;

    @Override
    public void run() {
        ScreenShotMaker screenShotMaker = new ScreenShotMaker(bdvh);
        screenShotMaker.setPhysicalPixelSpacingInXY(targetPixelSizeInXY, targetPixelUnit);
        screenShotMaker.getRgbScreenShot().show();
        if(showRawData) screenShotMaker.getRawScreenShot().show();
    }
}
