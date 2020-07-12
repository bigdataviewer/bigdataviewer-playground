package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ProjectionModeChanger;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

public class PerfOpenMultipleSpimDataTest
{
	public static void main( String[] args )
	{
		// Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// Gets active BdvHandle instance
		BdvHandle bdv = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();
		for (int i=0;i<100;i++) {
			// Import SpimData
			new SpimDataFromXmlImporter( "src/test/resources/mri-stack.xml" ).run();
			new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();
			new SpimDataFromXmlImporter( "src/test/resources/mri-stack-shiftedY.xml" ).run();
			System.out.println(i);
		}
	}
}
