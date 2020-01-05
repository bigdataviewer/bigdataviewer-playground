package sc.fiji.bdvpg.bdv.source.display;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.source.append.SourceAdder;
import sc.fiji.bdvpg.services.BdvService;
import sc.fiji.bdvpg.source.importer.SourceLoader;
import sc.fiji.bdvpg.source.importer.samples.VoronoiSourceGetter;

public class BrightnessAutoAdjusterDemo
{
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		BdvService.InitScijavaServices();

		// Creates a BdvHandle
		BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

		final Source source = getMriSource();
		addSource( bdvHandle, source );

		// Voronoi
		final Source voronoiSource = new VoronoiSourceGetter( new long[]{ 512, 512, 1 }, 256, true ).get();
		addSource( bdvHandle, voronoiSource );
	}

	public static Source getMriSource()
	{
		final String filePath = "src/test/resources/mri-stack.xml";
		final SourceLoader sourceLoader = new SourceLoader( filePath );
		sourceLoader.run();
		return sourceLoader.getSource( 0 );
	}

	public static void addSource( BdvHandle bdvHandle, Source source )
	{
		new SourceAdder( bdvHandle, source ).run();
		new ViewerTransformAdjuster( bdvHandle, source ).run();
		new BrightnessAutoAdjuster( source,0 ).run();
	}
}
