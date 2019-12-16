package src.sc.fiji.bdv.navigate;

import bdv.util.BdvHandle;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.bdv.BDVSingleton;
import sc.fiji.bdvpg.bdv.ClickBehaviourInstaller;
import sc.fiji.bdvpg.bdv.navigate.ViewTransformator;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformLogger;

/**
 * ViewTransformSetAndLogDemo
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class ViewTransformSetAndLogDemo {
    public static void main(String[] args) {
        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        //Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "name");

        // Open BigDataViewer and show the image
        BdvHandle bdvHandle = BDVSingleton.getInstance(rai, "name");

        // add a click behavior for logging transforms
        new ClickBehaviourInstaller( bdvHandle, (x, y ) -> new ViewerTransformLogger( bdvHandle ).run() ).install( "Log view transform", "ctrl D" );

        // log transform
        new ViewerTransformLogger(bdvHandle).run();

        // update transform
        AffineTransform3D affineTransform3D = new AffineTransform3D();
        affineTransform3D.rotate(2, 45);
        new ViewTransformator(bdvHandle, affineTransform3D).run();

        // log transform
        new ViewerTransformLogger(bdvHandle).run();

    }
}
