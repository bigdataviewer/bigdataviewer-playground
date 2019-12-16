package sc.fiji.bdv.source.read;

import bdv.util.BdvHandle;
import loci.common.CaseInsensitiveLocation;
import mpicbg.spim.data.SpimData;
import net.imagej.ImageJ;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.ClickBehaviourInstaller;
import sc.fiji.bdv.MenuAdder;
import sc.fiji.bdv.sources.read.SourceLoader;
import sc.fiji.bdv.sources.read.ui.SourcesLoaderAndAdderCommand;

public class SourceReaderDemo
{
	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final String filePath = "src/test/resources/mri-stack.xml";

		final SourceLoader sourceLoader = new SourceLoader( filePath );
		sourceLoader.run();
		final SpimData spimData = sourceLoader.getSpimData();

		final BdvHandle bdvHandle = BDVSingleton.getInstance( spimData );

		new ClickBehaviourInstaller( bdvHandle, ( x, y ) -> ij.command().run( SourcesLoaderAndAdderCommand.class, true ) ).install( "SourceLoadingBehaviour", "ctrl L" );


		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getTriggerbindings(), "myBehaviours" );
		behaviours.behaviour( ( ClickBehaviour ) ( x, y ) -> ij.command().run( SourcesOpenerCommand.class, true ), "SourcesOpenerCommand", "ctrl O" ) ;

	}
}
