package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

/**
 * Demonstrates visualisation of two spimData sources.
 *
 */
public class SpimDataDisplayDemo
{
	public static void main( String[] args )
	{
		// Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// Gets active BdvHandle instance
		BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

		// Import SpimData
		new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").run();
		new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();

		// Show all SourceAndConverter associated with above SpimData
		SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters().forEach( sac -> {
			SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac);
			new ViewerTransformAdjuster(bdvHandle, sac).run();
			new BrightnessAutoAdjuster(sac, 0).run();
		});
	}
}
