package sc.fiji.bdv.sources.read;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.ClickBehaviourInstaller;
import sc.fiji.bdv.MenuAdder;

import javax.swing.*;

public class SourceLoaderAndAdder implements Runnable
{
	private final BdvHandle bdvHandle;
	private final String filePath;

	public SourceLoaderAndAdder( BdvHandle bdvHandle )
	{
		this( bdvHandle, null );
	}

	public SourceLoaderAndAdder( BdvHandle bdvHandle, String filePath )
	{
		this.bdvHandle = bdvHandle;
		this.filePath = filePath;
	}

	@Override
	public void run()
	{
		if ( filePath == null )
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
		else
		{
			final Source source = new SourceLoader( filePath ).getSource( 0 );
			new SourceAdder( bdvHandle, source ).run();
		}
	}

	public static void main( String[] args )
	{
		final BdvHandle bdvHandle = BDVSingleton.getInstance();

		final MenuAdder menuAdder = new MenuAdder( bdvHandle, e -> new SourceLoaderAndAdder( bdvHandle ).run() );
		menuAdder.addMenu( "Source", "Add [Ctrl+A]" );

		new ClickBehaviourInstaller( bdvHandle, ( x, y ) -> new SourceLoaderAndAdder( bdvHandle ).run() ).install( "AddSourceFromFile", "ctrl A" );
	}

}
