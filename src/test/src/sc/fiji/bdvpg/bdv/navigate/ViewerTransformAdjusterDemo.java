package src.sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SacServices;
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
        SacServices.InitScijavaServices();

        // Gets active BdvHandle instance
        BdvHandle bdvHandle = SacServices.getSacDisplayService().getActiveBdv();

        // Import SpimData object
        SpimDataFromXmlImporter sdix = new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml");

        AbstractSpimData asd = sdix.get();

        // Register to the sourceandconverter service
        SacServices.getSacService().register(asd);

        SacServices.getSacService().getSourceAndConverterFromSpimdata(asd).forEach( source -> {
            SacServices.getSacDisplayService().show(bdvHandle, source);
        });

        // Import SpimData object
        sdix = new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml");

        asd = sdix.get();

        // Register to the sourceandconverter service
        SacServices.getSacService().register(asd);

        SacServices.getSacService().getSourceAndConverterFromSpimdata(asd).forEach( source -> {
            SacServices.getSacDisplayService().show(bdvHandle, source);
        });

        new ViewerTransformAdjuster(bdvHandle, SacServices.getSacService().getSourceAndConverterFromSpimdata(asd).get(0)).run();
    }
}
