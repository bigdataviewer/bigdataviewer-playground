package sc.fiji.source.read.ui;

import bdv.util.BdvHandle;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.ClickBehaviourInstaller;
import sc.fiji.bdv.MenuAdder;
import sc.fiji.source.read.SourcesLoaderAndAdder;
import sc.fiji.swing.MultipleFileSelector;

import javax.swing.*;

public class SourcesLoaderAndAdderSwingDialog implements Runnable
{
	private final BdvHandle bdvHandle;

	public SourcesLoaderAndAdderSwingDialog( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	@Override
	public void run()
	{
		SwingUtilities.invokeLater( () ->
		{

			final MultipleFileSelector fileSelector = new MultipleFileSelector();
			if ( ! fileSelector.showUI() ) return;
			String[] filePaths = fileSelector.getSelectedFilePaths();
			new SourcesLoaderAndAdder( bdvHandle, filePaths ).run();
		});
	}

	public static void main( String[] args )
	{
		final BdvHandle bdvHandle = BDVSingleton.getInstance( );

		final MenuAdder menuAdder = new MenuAdder( bdvHandle, e -> new SourcesLoaderAndAdderSwingDialog( bdvHandle ).run() );
		menuAdder.addMenu( "Sources", "Load Source(s)  [Ctrl+L]" );

		new ClickBehaviourInstaller( bdvHandle, ( x, y ) -> new SourcesLoaderAndAdderSwingDialog( bdvHandle ).run() ).install( "AddSourceFromFile", "ctrl L" );
	}
}