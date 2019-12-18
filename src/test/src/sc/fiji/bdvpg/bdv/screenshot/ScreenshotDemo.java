package src.sc.fiji.bdvpg.bdv.screenshot;

import bdv.util.BdvHandle;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import sc.fiji.bdvpg.bdv.BDVSingleton;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.bdv.MenuAdder;
import sc.fiji.bdvpg.bdv.source.screenshot.ScreenShotMaker;

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
        //BdvFunctions.show(rai, "name", BdvOptions.options().addTo(bdvh));
        //BdvFunctions.show(rai, "name", BdvOptions.options());

        // add a click behaviour to BDV for making screenshots
        new ClickBehaviourInstaller( bdvHandle, (x, y ) -> new ScreenShotMaker( bdvHandle ).getScreenshot().show() ).install( "Make screenshot", "ctrl D" );

        // add a menu entry to the BDV
        new MenuAdder(bdvHandle, (e) -> { new ScreenShotMaker(bdvHandle).getScreenshot().show();}).addMenu("Edit", "Screenshot");

        // retrieve a screenshot from the BDV
        ScreenShotMaker screenShotMaker = new ScreenShotMaker(bdvHandle);
        screenShotMaker.setPhysicalPixelSpacingInXY(0.5, "micron");
        ImagePlus screenShot = screenShotMaker.getScreenshot();
        screenShot.show();
    }
}
