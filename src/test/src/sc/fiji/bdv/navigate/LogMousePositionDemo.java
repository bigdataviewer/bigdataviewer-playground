package sc.fiji.bdv.navigate;

import bdv.util.BdvHandle;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
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
public class LogMousePositionDemo {
    public static void main(String... args) {
        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        //Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "name");

        // Open BigDataViewer and show the image
        BdvHandle bdvHandle = BDVSingleton.getInstance(rai, "name");

        // add a click behavior for logging mouse positions
        new ClickBehaviourInstaller( bdvHandle, (x, y ) -> new PositionLogger( bdvHandle ).run() ).install( "Log mouse position", "ctrl D" );

        // log the current position
        new PositionLogger( bdvHandle ).run();
    }
}
