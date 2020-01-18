package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.services.SacServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ProjectionModeChanger;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

public class ProjectionModeChangerDemo
{
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		SacServices.InitScijavaServices();

		// Gets active BdvHandle instance
		BdvHandle bdv = SacServices.getSacDisplayService().getActiveBdv();

		// Import SpimData
		new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").run();
		new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();
		new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedY.xml").run();

		final List< SourceAndConverter > sourceAndConverters = SacServices.getSacService().getSourceAndConverters();

		// Show all three sacs
		sourceAndConverters.forEach( sac -> {
			SacServices.getSacDisplayService().show(bdv, sac);
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
