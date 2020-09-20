package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import org.junit.Test;
import sc.fiji.bdvpg.bdv.ScreenShotMaker;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ProjectionModeChanger;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

/**
 * ScreenShotDemo
 * <p>
 * <p>
 * <p>
 * Author: Tischi
 * 12 2019
 */
public class ScreenShotDemo
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

        final ISourceAndConverterService sacService = SourceAndConverterServices.getSourceAndConverterService();

        final List< SourceAndConverter > sacs = sacService.getSourceAndConverters();

        showSacs( bdvHandle, sacs );

        new ProjectionModeChanger( new SourceAndConverter[]{ sacs.get( 0 ) }, Projection.PROJECTION_MODE_AVG, true ).run();

        ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle );
        screenShotMaker.setPhysicalPixelSpacingInXY( 0.5, "micron" );
        screenShotMaker.getRgbScreenShot().show();
        screenShotMaker.getRawScreenShot().show();
    }

    public static void showSacs( BdvHandle bdvHandle, List< SourceAndConverter > sacs )
    {
        sacs.forEach( sac -> {
                SourceAndConverterServices.getSourceAndConverterDisplayService().show( bdvHandle, sac );
                new ViewerTransformAdjuster( bdvHandle, sac ).run();
                new BrightnessAutoAdjuster( sac, 0 ).run();
        } );
    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }
}
