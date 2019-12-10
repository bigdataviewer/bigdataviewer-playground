package sc.fiji.bdv.sourcehandling;

import bdv.util.BdvHandle;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdv.BDVSingleton;

import javax.swing.*;

public class AddSourceFromFileClickBehaviour implements ClickBehaviour
{
	private final BdvHandle bdvHandle;

	public AddSourceFromFileClickBehaviour( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	public void install( String trigger )
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getTriggerbindings(), "" );
		behaviours.behaviour( this, "Open source from file", trigger ) ;
	}

	@Override
	public void click( int x, int y )
	{
		SwingUtilities.invokeLater( () -> {
			final JFileChooser jFileChooser = new JFileChooser();
			if ( jFileChooser.showOpenDialog( bdvHandle.getViewerPanel() ) == JFileChooser.APPROVE_OPTION )
			{
				final String absolutePath = jFileChooser.getSelectedFile().getAbsolutePath();
				new SourceLoaderAndAdder( bdvHandle, absolutePath ).run();
			};
		});
	}

	public static void main( String[] args ) throws SpimDataException
	{
		final BdvHandle bdvHandle = BDVSingleton.getInstance( new XmlIoSpimData().load( "/Users/tischer/Documents/bigdataviewer-playground/src/test/resources/mri-stack.xml" ) );
		new AddSourceFromFileClickBehaviour( bdvHandle ).install( "ctrl O");
	}
}

