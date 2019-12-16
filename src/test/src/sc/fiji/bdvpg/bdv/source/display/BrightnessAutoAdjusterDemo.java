package sc.fiji.bdvpg.bdv.source.display;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import sc.fiji.bdvpg.bdv.BDVSingleton;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.source.append.SourceAdder;
import sc.fiji.bdvpg.source.importer.SourceLoader;

public class BrightnessAutoAdjusterDemo
{
	public static void main( String[] args )
	{
		// Open BigDataViewer
		BdvHandle bdvHandle = BDVSingleton.getInstance();

		final String filePath = "src/test/resources/mri-stack.xml";

		final SourceLoader sourceLoader = new SourceLoader( filePath );
		sourceLoader.run();
		final Source source = sourceLoader.getSource( 0 );

		new SourceAdder( bdvHandle, source ).run();
		new ViewerTransformAdjuster( bdvHandle, source ).run();
		new BrightnessAutoAdjuster( bdvHandle, source ).run();
	}
}
