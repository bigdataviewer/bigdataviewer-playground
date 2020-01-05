package src.sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.spimdata.importer.SpimDataImporterXML;

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
        BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

        // Import SpimData object
        SpimDataImporterXML sdix = new SpimDataImporterXML("src/test/resources/mri-stack.xml");

        AbstractSpimData asd = sdix.get();

        // Register to the source service
        BdvService.getSourceService().register(asd);

        BdvService.getSourceService().getSourcesFromSpimdata(asd).forEach(source -> {
            BdvService.getSourceDisplayService().show(bdvHandle, source);
        });

        // Import SpimData object
        sdix = new SpimDataImporterXML("src/test/resources/mri-stack-shiftedX.xml");

        asd = sdix.get();

        // Register to the source service
        BdvService.getSourceService().register(asd);

        BdvService.getSourceService().getSourcesFromSpimdata(asd).forEach(source -> {
            BdvService.getSourceDisplayService().show(bdvHandle, source);
        });

        new ViewerTransformAdjuster(bdvHandle, BdvService.getSourceService().getSourcesFromSpimdata(asd).get(0)).run();

        //new SourcesLoaderAndAdder( bdvHandle, "src/test/resources/mri-stack.xml" ).run();

        //final SourcesLoaderAndAdder loaderAndAdder = new SourcesLoaderAndAdder( bdvHandle, "src/test/resources/mri-stack-shiftedX.xml" );
        //loaderAndAdder.setAutoAdjustViewerTransform( true );
        //loaderAndAdder.setAutoContrast( true );
        //loaderAndAdder.run();
    }
}
