package sc.fiji.bdv.screenshot;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Util;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.ClickBehaviourInstaller;

/**
 * ViewTransformSetAndLogDemo
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class ScreenshotDemo {
    public static void main(String[] args) {

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        //Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "name");

        // Open BigDataViewer and show the image
        BdvHandle bdvHandle = BDVSingleton.getInstance(rai, "name");
        //BdvFunctions.show(rai, "name", BdvOptions.options().addTo(bdvHandle));
        //BdvFunctions.show(rai, "name", BdvOptions.options());

        // add a click behaviour to BDV for making screenshots
        new ClickBehaviourInstaller( bdvHandle, (x, y ) -> new ScreenShotMaker( bdvHandle ).getScreenshot().show() ).install( "Make screenshot", "ctrl D" );

        // retrieve a screenshot from the BDV
        ScreenShotMaker screenShotMaker = new ScreenShotMaker(bdvHandle);
        screenShotMaker.setPhysicalPixelSpacingInXY(0.5, "micron");
        ImagePlus screenShot = screenShotMaker.getScreenshot();
        screenShot.show();
    }
}
