package sc.fiji.bdv.sourcehandling;

import bdv.util.BdvHandle;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.BehaviourInstaller;

public class AddSourceFromFileClickBehaviourInstaller implements ClickBehaviour
{
	private final BdvHandle bdvHandle;

	public AddSourceFromFileClickBehaviourInstaller( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	public void install( String name, String trigger )
	{
		new BehaviourInstaller( bdvHandle, this ).install( "AddSourceFromFile", "ctrl O" );
	}

	@Override
	public void click( int x, int y )
	{
		SourceHandlingUtils.showFileDialogLoadAndAddSource( bdvHandle );
	}

	public static void main( String[] args ) throws SpimDataException
	{
		final BdvHandle bdvHandle = BDVSingleton.getInstance( new XmlIoSpimData().load( "/Users/tischer/Documents/bigdataviewer-playground/src/test/resources/mri-stack.xml" ) );
		new AddSourceFromFileClickBehaviourInstaller( bdvHandle ).install( "Open source from file", "ctrl O" );
	}
}

