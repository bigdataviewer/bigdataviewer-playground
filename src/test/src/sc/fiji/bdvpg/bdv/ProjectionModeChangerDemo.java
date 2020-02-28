package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ProjectionModeChanger;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

public class ProjectionModeChangerDemo
{
	public static void main( String[] args )
	{
		// Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// Gets active BdvHandle instance
		BdvHandle bdv = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

		// Import SpimData
		new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").run();
		new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();
		new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedY.xml").run();

		final List< SourceAndConverter > sourceAndConverters = SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters();

		// Show all three sacs
		sourceAndConverters.forEach( sac -> {
			SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdv, sac);
			new ViewerTransformAdjuster(bdv, sac).run();
			new BrightnessAutoAdjuster(sac, 0).run();
		});

		// For the first two, change the projection mode to avg (default is sum, if it is not set)
		final SourceAndConverter[] sacs = new SourceAndConverter[ 2 ];
		sacs[ 0 ] = sourceAndConverters.get( 0 );
		sacs[ 1 ] = sourceAndConverters.get( 1 );

		new ProjectionModeChanger( sacs, Projection.PROJECTION_MODE_AVG ).run();

	}
}
