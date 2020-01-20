package sc.fiji.bdvpg.bdv;

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
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

/**
 * Manual Registration Demo
 * Author: Nicolas Chiaruttini
 * 01 2020
 */
public class ManualRegistrationDemo {

    final public static int CreateNewTransformedSourceAndConverter = 0;
    final public static int MutateTransformedSourceAndConverter = 1;

    final public static int AppendNewSpimdataTransformation = 2;
    final public static int MutateLastSpimdataTransformation = 3;


    public static boolean isTransforming = false;

    public static void main(String[] args) {

        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because Bdv needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes Bdv Source
        Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");

        // Creates a BdvHandle
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();

        // Creates SourceAndConverter Reference
        SourceAndConverter sacReference = SourceAndConverterUtils.createSourceAndConverter(source);

        //int demoMode = CreateNewTransformedSourceAndConverter;
        //int demoMode = MutateTransformedSourceAndConverter;

        int demoMode = AppendNewSpimdataTransformation;


        if (demoMode == CreateNewTransformedSourceAndConverter) {

            SourceAndConverter sacToTransform;
            sacToTransform = SourceAndConverterUtils.createSourceAndConverter(source);
            new ColorChanger(sacToTransform, new ARGBType(ARGBType.rgba(255, 0, 0, 0))).run();

            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sacReference);
            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sacToTransform);

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

            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sacReference);
            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sacToTransform);

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
        } else if (demoMode == MutateLastSpimdataTransformation) {
            AbstractSpimData asd =  new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").get();

            // Show all SourceAndConverter associated with above SpimData
            SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters().forEach( sac -> {
                SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac);
                new BrightnessAutoAdjuster(sac, 0).run();
            });

            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sacReference);
            new ViewerTransformAdjuster(bdvHandle, sacReference).run();

            List<SourceAndConverter> sacList = SourceAndConverterServices.getSourceAndConverterService()
                    .getSourceAndConverterFromSpimdata(asd);

            ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacList.toArray(new SourceAndConverter[sacList.size()]));
            ManualRegistrationStopper manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    ManualRegistrationStopper::mutateLastSpimdataTransformation
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

        }else if (demoMode == AppendNewSpimdataTransformation) {
            // TO complete
            // Import SpimData
            AbstractSpimData asd =  new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").get();

            // Show all SourceAndConverter associated with above SpimData
            SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters().forEach( sac -> {
                SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac);
                new BrightnessAutoAdjuster(sac, 0).run();
            });

            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sacReference);
            new ViewerTransformAdjuster(bdvHandle, sacReference).run();

            List<SourceAndConverter> sacList = SourceAndConverterServices.getSourceAndConverterService()
                    .getSourceAndConverterFromSpimdata(asd);


            ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacList.toArray(new SourceAndConverter[sacList.size()]));
            ManualRegistrationStopper manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    ManualRegistrationStopper::appendNewSpimdataTransformation
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
