package sc.fiji.bdvpg.bdv.source.bigwarp;

import bdv.util.BdvHandle;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.source.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.scijava.command.bdv.BigWarpLauncherCommand;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;
import sc.fiji.bdvpg.scijava.services.BdvSourceService;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.source.importer.samples.MandelbrotSourceGetter;

public class BigWarpDemo {
    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        BdvService.iss = ij.get(BdvSourceService.class);
        BdvService.isds = ij.get(BdvSourceDisplayService.class);

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because Bdv needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes Bdv Source
        Source blobs = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");

        // Creates a BdvHandle
        BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

        // Show the source
        BdvService.getSourceDisplayService().show(bdvHandle, blobs);

        Source mandelbrot = new MandelbrotSourceGetter().get();

        BdvService.getSourceDisplayService().show(bdvHandle, mandelbrot);

        BdvService.getSourceDisplayService().getConverterSetup(mandelbrot)
                .setColor(new ARGBType(ARGBType.rgba(255, 0, 255,0)));

        BdvService.getSourceDisplayService().getConverterSetup(blobs)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        new BrightnessAutoAdjuster(mandelbrot, 0).run();

        new BrightnessAutoAdjuster(blobs, 0).run();

        new ViewerTransformAdjuster(bdvHandle, blobs).run();

        // Doable without SciJava Services but annoying...
        // Thanks to converters and services, the source are transfered as well as converter setups
        // with appropriate callbacks
        ij.command().run(BigWarpLauncherCommand.class, true,
                "movingSources", new Source[]{mandelbrot},
                        "fixedSources", new Source[]{blobs},
                        "bigWarpName", "Big Warp Demo"
                );

    }
}
