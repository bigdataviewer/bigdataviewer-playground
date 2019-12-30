package sc.fiji.bdvpg.bdv.sourceAndConverter;

import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.source.append.SourceAndConverterBdvAdder;
import sc.fiji.bdvpg.projector.AccumulateAverageProjectorARGB;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterLoader;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAdjuster;


/**
 * Demonstrates average projection of two sources.
 *
 * TODO: make projection mode optional or dependend on some source metadata.
 */
public class ProjectorDemo
{
	public static void main( String[] args )
	{
		final BdvHandle bdvHandle = createBdv();

		// add 1st source
		final SourceAndConverter< ? > sourceAndConverter = new SourceAndConverterLoader( "src/test/resources/mri-stack.xml" ).getSourceAndConverter( 0 );
		new SourceAndConverterBdvAdder( bdvHandle, sourceAndConverter ).run();
		new ViewerTransformAdjuster( bdvHandle, sourceAndConverter.getSpimSource() ).run();
		new BrightnessAdjuster( bdvHandle, sourceAndConverter, 10, 255.0 ).run();

		// add 2nd source
		final SourceAndConverter< ? > sourceAndConverter2 = new SourceAndConverterLoader( "src/test/resources/mri-stack-shiftedX.xml" ).getSourceAndConverter( 0 );
		new SourceAndConverterBdvAdder( bdvHandle, sourceAndConverter2 ).run();
		new BrightnessAdjuster( bdvHandle, sourceAndConverter2, 10, 255.0 ).run();
	}

	public static BdvHandle createBdv()
	{
		// specify average projector factory
		final BdvOptions bdvOptions = new BdvOptions()
				.accumulateProjectorFactory( AccumulateAverageProjectorARGB.factory );

		final BdvCreator bdvCreator = new BdvCreator( bdvOptions );
		bdvCreator.run();
		return bdvCreator.get();
	}
}
