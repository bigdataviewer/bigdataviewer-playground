package sc.fiji.bdvpg.sourceandconverter.display;

public class BrightnessAutoAdjusterDemo
{
	/*
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		BdvService.InitScijavaServices();

		// Creates a BdvHandle
		BdvHandle bdvHandle = BdvService.getSourceDisplayService().getActiveBdv();

		final Source sourceandconverter = getMriSource();
		addSource( bdvHandle, sourceandconverter );

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

	public static void addSource( BdvHandle bdvHandle, Source sourceandconverter )
	{
		new SourceAdder( bdvHandle, sourceandconverter ).run();
		new ViewerTransformAdjuster( bdvHandle, sourceandconverter ).run();
		new BrightnessAutoAdjuster( sourceandconverter,0 ).run();
	}
	*/
}
