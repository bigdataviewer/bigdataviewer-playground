package sc.fiji.bdvpg.bdv.sourceAndConverter;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.source.append.SourceAndConverterBdvAdder;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterLoader;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAdjuster;

public class SourceAndConverterLoaderDemo
{
	public static void main( String[] args )
	{
		final BdvHandle bdvHandle = createBdv();
		final SourceAndConverter< ? > sourceAndConverter = loadSourceAndConverter();
		new SourceAndConverterBdvAdder( bdvHandle, sourceAndConverter ).run();
		new ViewerTransformAdjuster( bdvHandle, sourceAndConverter.getSpimSource() ).run();
		new BrightnessAdjuster( bdvHandle, sourceAndConverter, 0, 255.0 ).run();
	}

	public static SourceAndConverter< ? > loadSourceAndConverter()
	{
		final SourceAndConverterLoader loader = new SourceAndConverterLoader( "src/test/resources/mri-stack.xml" );
		return loader.getSourceAndConverter( 0 );
	}

	public static BdvHandle createBdv()
	{
		final BdvCreator bdvCreator = new BdvCreator( false );
		bdvCreator.run();
		return bdvCreator.get();
	}
}
