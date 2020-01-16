package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

/**
 * Manual Registration Demo
 * Author: Nicolas Chiaruttini
 * 01 2020
 */
public class ManualRegistrationDemo {

    final public static int OutputNewTransformedSourceAndConverter = 0;
    final public static int MutateTransformedSourceAndConverter = 1;

    public static boolean isTransforming = false;

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

        // Creates a BdvHandle
        BdvHandle bdvHandle = BdvService.getSourceAndConverterDisplayService().getNewBdv();

        // Creates SourceAndConverter Reference
        SourceAndConverter sacReference = SourceAndConverterUtils.createSourceAndConverter(source);

        //int demoMode = OutputNewTransformedSourceAndConverter;
        int demoMode = MutateTransformedSourceAndConverter;


        if (demoMode == OutputNewTransformedSourceAndConverter) {

            SourceAndConverter sacToTransform;
            sacToTransform = SourceAndConverterUtils.createSourceAndConverter(source);
            new ColorChanger(sacToTransform, new ARGBType(ARGBType.rgba(255, 0, 0, 0))).run();

            BdvService.getSourceAndConverterDisplayService().show(bdvHandle, sacReference);
            BdvService.getSourceAndConverterDisplayService().show(bdvHandle, sacToTransform);

            // Adjust view on sourceandconverter
            new ViewerTransformAdjuster(bdvHandle, sacReference).run();

            ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacToTransform);
            ManualRegistrationStopper manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    ManualRegistrationStopper::createNewTransformedSourceAndConverter
            );

            manualRegistrationStarter.run();

            new ClickBehaviourInstaller(bdvHandle, (x, y) -> {
                manualRegistrationStopper.run();
            }).install("Stop Transformation", "ctrl M");

        } else if (demoMode == MutateTransformedSourceAndConverter) {

            SourceAndConverter sacToTransform;
            sacToTransform = SourceAndConverterUtils.createSourceAndConverter(source);
            sacToTransform = new SourceAffineTransformer(sacToTransform, new AffineTransform3D()).getSourceOut();
            new ColorChanger(sacToTransform, new ARGBType(ARGBType.rgba(255, 0, 0, 0))).run();

            BdvService.getSourceAndConverterDisplayService().show(bdvHandle, sacReference);
            BdvService.getSourceAndConverterDisplayService().show(bdvHandle, sacToTransform);

            // Adjust view on sourceandconverter
            new ViewerTransformAdjuster(bdvHandle, sacReference).run();

            ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacToTransform);
            ManualRegistrationStopper manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    ManualRegistrationStopper::mutateTransformedSourceAndConverter
            );

            isTransforming = false;

            new ClickBehaviourInstaller(bdvHandle, (x,y) -> {
                if (isTransforming) {
                    manualRegistrationStopper.run();
                } else {
                    manualRegistrationStarter.run();
                }
                isTransforming = !isTransforming;
            }).install("Toggle Transformation", "ctrl M");
        }


    }
}
