package src.sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporterAndRegisterer;

/**
 * ViewerTransformAdjusterDemo
 * <p>
 * <p>
 * <p>
 * Author: @tischi
 * 12 2019
 */
public class ViewerTransformAdjusterDemo {
    public static void main(String[] args)
    {

        // Initializes static SourceService and Display Service
        BdvService.InitScijavaServices();

        // Gets active BdvHandle instance
        BdvHandle bdvHandle = BdvService.getSourceAndConverterDisplayService().getActiveBdv();

        // Import SpimData object
        SpimDataFromXmlImporterAndRegisterer sdix = new SpimDataFromXmlImporterAndRegisterer("src/test/resources/mri-stack.xml");

        AbstractSpimData asd = sdix.get();

        // Register to the sourceandconverter service
        BdvService.getSourceAndConverterService().register(asd);

        BdvService.getSourceAndConverterService().getSourceAndConverterFromSpimdata(asd).forEach( source -> {
            BdvService.getSourceAndConverterDisplayService().show(bdvHandle, source);
        });

        // Import SpimData object
        sdix = new SpimDataFromXmlImporterAndRegisterer("src/test/resources/mri-stack-shiftedX.xml");

        asd = sdix.get();

        // Register to the sourceandconverter service
        BdvService.getSourceAndConverterService().register(asd);

        BdvService.getSourceAndConverterService().getSourceAndConverterFromSpimdata(asd).forEach( source -> {
            BdvService.getSourceAndConverterDisplayService().show(bdvHandle, source);
        });

        new ViewerTransformAdjuster(bdvHandle, BdvService.getSourceAndConverterService().getSourceAndConverterFromSpimdata(asd).get(0)).run();
    }
}
