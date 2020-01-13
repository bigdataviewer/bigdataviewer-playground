package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporterAndRegisterer;

/**
 * Demonstrates visualisation of two spimData sources.
 *
 */
public class SpimDataDisplayDemo
{
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		BdvService.InitScijavaServices();

		// Gets active BdvHandle instance
		BdvHandle bdvHandle = BdvService.getSourceAndConverterDisplayService().getActiveBdv();

		// Import SpimData
		new SpimDataFromXmlImporterAndRegisterer("src/test/resources/mri-stack.xml").run();
		new SpimDataFromXmlImporterAndRegisterer("src/test/resources/mri-stack-shiftedX.xml").run();

		// Show all SourceAndConverter associated with above SpimData
		BdvService.getSourceAndConverterService().getSourceAndConverters().forEach( sac -> {
			BdvService.getSourceAndConverterDisplayService().show(bdvHandle, sac);
			new ViewerTransformAdjuster(bdvHandle, sac).run();
			new BrightnessAutoAdjuster(sac, 0).run();
		});
	}
}
