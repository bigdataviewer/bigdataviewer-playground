package sc.fiji.bdvpg.bdv.sourceAndConverter;

import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.source.append.SourceAndConverterBdvAdder;
import sc.fiji.bdvpg.projector.AccumulateProjectorARGB;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterLoader;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAdjuster;

public class ProjectorDemo
{
	public static void main( String[] args )
	{
		final BdvHandle bdvHandle = createBdv();
		final SourceAndConverter< ? > sourceAndConverter = new SourceAndConverterLoader( "src/test/resources/mri-stack.xml" ).getSourceAndConverter( 0 );
		new SourceAndConverterBdvAdder( bdvHandle, sourceAndConverter ).run();
		new ViewerTransformAdjuster( bdvHandle, sourceAndConverter.getSpimSource() ).run();
		new BrightnessAdjuster( bdvHandle, sourceAndConverter, 0, 255.0 ).run();
	}

	public static BdvHandle createBdv()
	{

		final BdvOptions bdvOptions = new BdvOptions().accumulateProjectorFactory( AccumulateProjectorARGB.factory );
		final BdvCreator bdvCreator = new BdvCreator( bdvOptions );
		bdvCreator.run();
		return bdvCreator.get();
	}
}
