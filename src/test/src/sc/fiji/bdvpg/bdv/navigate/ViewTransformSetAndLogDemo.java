package src.sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.bdv.navigate.ViewTransformator;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformLogger;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

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

        // Initializes static SourceService and Display Service
        BdvService.InitScijavaServices();

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because Bdv needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes Bdv Source
        Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");
        SourceAndConverter sac = SourceAndConverterUtils.makeSourceAndConverter(source);

        // Creates a BdvHandle
        BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

        // Show the source
        BdvService.getSourceDisplayService().show(bdvHandle, sac);

        // Adjust view on source
        new ViewerTransformAdjuster(bdvHandle, sac).run();

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
