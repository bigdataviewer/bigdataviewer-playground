package sc.fiji.bdvpg.bdv.sourceandconverter.bigwarp;

public class BigWarpDemo {
    /*
    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        BdvService.iss = ij.get(BdvSourceAndConverterService.class);
        BdvService.isds = ij.get(BdvSourceAndConverterDisplayService.class);

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because BDV needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes BDV Source
        Source blobs = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");

        // Creates a BdvHandle
        BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

        // Show the sourceandconverter
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
        // Thanks to converters and services, the sourceandconverter are transfered as well as converter setups
        // with appropriate callbacks
        ij.command().run(BigWarpLauncherCommand.class, true,
                "movingSources", new Source[]{mandelbrot},
                        "fixedSources", new Source[]{blobs},
                        "bigWarpName", "Big Warp Demo"
                );

    }*/
}
