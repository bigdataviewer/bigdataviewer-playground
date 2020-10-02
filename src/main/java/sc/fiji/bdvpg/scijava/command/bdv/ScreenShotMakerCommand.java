package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
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

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Screenshot",
        label = "Creates a screenshot of a BDV view, the resolution can be chosen to upscale or downscale" +
                " the image compared to the original window. A single RGB image resulting from the projection" +
                " of all sources is displayed. Raw image data can also be exported in grayscale.")
public class ScreenShotMakerCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter(label="Target Pixel Size (in XY)")
    public double targetPixelSizeInXY = 1;

    @Parameter(label="Pixel Size Unit")
    public String targetPixelUnit = "Pixels";

    @Parameter(label="Show Raw Data")
    public boolean showRawData = false;

    @Override
    public void run() {
        ScreenShotMaker screenShotMaker = new ScreenShotMaker(bdvh);
        screenShotMaker.setPhysicalPixelSpacingInXY(targetPixelSizeInXY, targetPixelUnit);
        screenShotMaker.getRgbScreenShot().show();
        if(showRawData) screenShotMaker.getRawScreenShot().show();
    }
}
