package sc.fiji.bdvpg.bdv.screenshot;

import bdv.util.BdvHandle;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.bdv.ScreenShotMaker;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

/**
 * ViewTransformSetAndLogDemo
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */
public class ScreenshotDemo
{
    public static void main(String[] args)
    {
        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Gets active BdvHandle instance
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        // Import SpimData
        new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").run();
        new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();

        // Show all sacs
        SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters().forEach( sac -> {
                SourceAndConverterServices.getSourceAndConverterDisplayService().show( bdvHandle, sac );
                new ViewerTransformAdjuster( bdvHandle, sac ).run();
                new BrightnessAutoAdjuster( sac, 0 ).run();
        } );

        // Retrieve screenshot from BDV
        ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle );
        screenShotMaker.setPhysicalPixelSpacingInXY( 0.5, "micron" );
        screenShotMaker.getRgbScreenShot().show();
        screenShotMaker.getRawScreenShot().show();
    }
}
