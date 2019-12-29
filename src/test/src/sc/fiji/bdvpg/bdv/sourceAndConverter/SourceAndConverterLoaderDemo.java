package sc.fiji.bdvpg.bdv.sourceAndConverter;

import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.source.append.SourceAndConverterBdvAdder;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterLoader;

public class SourceAndConverterLoaderDemo
{
	public static void main( String[] args )
	{
		final BdvCreator bdvCreator = new BdvCreator( false );
		bdvCreator.run();
		final BdvHandle bdvHandle = bdvCreator.get();

		// load
		//
		final SourceAndConverterLoader loader = new SourceAndConverterLoader( "src/test/resources/mri-stack.xml" );
		final SourceAndConverter< ? > sourceAndConverter = loader.getSourceAndConverter( 0 );

		// add
		//
		final SourceAndConverterBdvAdder adder = new SourceAndConverterBdvAdder( bdvHandle, sourceAndConverter );
		adder.run();

		// change brightness
		//
		final BdvStackSource< ? > bdvStackSource = adder.getBdvStackSource();
		bdvStackSource.setDisplayRange( 0, 255 );

		// get the sac from the stacksource
		//
		final SourceAndConverter< ? > sac = bdvStackSource.getSources().get( 0 );
	}
}
