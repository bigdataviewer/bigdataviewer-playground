package sc.fiji.bdvpg.bdv.source.display;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import sc.fiji.bdvpg.bdv.BDVSingleton;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.source.append.SourceBdvAdder;
import sc.fiji.bdvpg.source.importer.SourceLoader;
import sc.fiji.bdvpg.source.importer.samples.VoronoiSourceGetter;

public class BrightnessAutoAdjusterDemo
{
	public static void main( String[] args )
	{
		// Mri stack
		BdvHandle bdvHandle = BDVSingleton.getInstance();

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
		new SourceBdvAdder( bdvHandle, source ).run();
		new ViewerTransformAdjuster( bdvHandle, source ).run();
		new BrightnessAutoAdjuster( bdvHandle, source ).run();
	}
}
