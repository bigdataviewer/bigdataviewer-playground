package sc.fiji.bdvpg.bdv.sourceandconverter.bigwarp;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.importer.MandelbrotSourceGetter;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BigWarpDemo {

    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // load and convert an image
      /*  ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because BDV needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes BDV Source
        Source blobs = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");
        */
        final String filePath = "src/test/resources/mri-stack.xml";
        // Import SpimData
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(filePath);
        //importer.run();

        final AbstractSpimData spimData = importer.get();

        SourceAndConverter sacBlobs = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);

        // Creates a BdvHandle
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        // Creates SourceAndConverter Reference
        //SourceAndConverter sacBlobs = SourceAndConverterUtils.createSourceAndConverter(blobs);

        // Show the sourceandconverter
        SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sacBlobs);

        SourceAndConverter mandelbrot = new MandelbrotSourceGetter().get();

        SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, mandelbrot);

        SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(mandelbrot)
                .setColor(new ARGBType(ARGBType.rgba(255, 0, 255,0)));

        SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(sacBlobs)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        new BrightnessAutoAdjuster(mandelbrot, 0).run();

        new BrightnessAutoAdjuster(sacBlobs, 0).run();

        new ViewerTransformAdjuster(bdvHandle, sacBlobs).run();

        List<SourceAndConverter> movingSources = new ArrayList<>();
        movingSources.add(sacBlobs);

        List<SourceAndConverter> fixedSources = new ArrayList<>();
        fixedSources.add(mandelbrot);

        List<ConverterSetup> converterSetups = movingSources.stream().map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList());
        converterSetups.addAll(fixedSources.stream().map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList()));

        BigWarpLauncher bwl = new BigWarpLauncher(movingSources, fixedSources, "BigWarp Demo", converterSetups);
        bwl.run();

        bwl.getBigWarp().loadLandmarks( "src/test/resources/landmarks3d-demo.csv" );

        bwl.getBigWarp().toggleMovingImageDisplay();
        bwl.getBigWarp().matchActiveViewerPanelToOther();

        /*bwl.getBigWarp().closeAll();

        for (SourceAndConverter sac : bwl.getWarpedSources()) {
            SourceAndConverterServices.getSourceAndConverterService()
                    .register(sac);
        }*/

    }
}
