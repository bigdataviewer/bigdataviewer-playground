package src.sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

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
        SourceAndConverterServices.InitScijavaServices();

        // Gets active BdvHandle instance
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        // Import SpimData object
        SpimDataFromXmlImporter sdix = new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml");

        AbstractSpimData asd = sdix.get();

        // Register to the sourceandconverter service
        SourceAndConverterServices.getSourceAndConverterService().register(asd);

        SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverterFromSpimdata(asd).forEach( source -> {
            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, source);
        });

        // Import SpimData object
        sdix = new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml");

        asd = sdix.get();

        // Register to the sourceandconverter service
        SourceAndConverterServices.getSourceAndConverterService().register(asd);

        SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverterFromSpimdata(asd).forEach( source -> {
            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, source);
        });

        new ViewerTransformAdjuster(bdvHandle, SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverterFromSpimdata(asd).get(0)).run();
    }
}
