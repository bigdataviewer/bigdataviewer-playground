package sc.fiji.bdv.sources.read;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.ClickBehaviourInstaller;
import sc.fiji.bdv.MenuAdder;
import sc.fiji.swing.MultipleFileSelector;

import javax.swing.*;
import java.io.File;

public class SourcesLoaderAndAdder implements Runnable
{
	private final BdvHandle bdvHandle;
	private String[] filePaths;

	public SourcesLoaderAndAdder( BdvHandle bdvHandle )
	{
		this( bdvHandle, null );
	}

	public SourcesLoaderAndAdder( BdvHandle bdvHandle, String[] filePaths )
	{
		this.bdvHandle = bdvHandle;
		this.filePaths = filePaths;
	}

	@Override
	public void run()
	{
		SwingUtilities.invokeLater( () ->
		{
			if ( filePaths == null )
			{
				final MultipleFileSelector fileSelector = new MultipleFileSelector();
				if ( ! fileSelector.showUI() ) return;
				filePaths = fileSelector.getSelectedFilePaths();
			}

			for ( String filePath : filePaths )
			{
				final Source source = new SourceLoader( filePath ).getSource( 0 );
				new SourceAdder( bdvHandle, source ).run();
			}
		});
	}

	public static void main( String[] args )
	{
		final String filePath = SourcesLoaderAndAdder.class.getResource( "../src/test/resources/mri-stack.xml" ).getFile();

		final SourceLoader sourceLoader = new SourceLoader( filePath );
		sourceLoader.run();
		final SpimData spimData = sourceLoader.getSpimData();

		final BdvHandle bdvHandle = BDVSingleton.getInstance( spimData );

//		final MenuAdder menuAdder = new MenuAdder( bdvHandle, e -> new SourcesLoaderAndAdder( bdvHandle ).run() );
//		menuAdder.addMenu( "Sources", "Load Source(s)  [Ctrl+L]" );
//
//		new ClickBehaviourInstaller( bdvHandle, ( x, y ) -> new SourcesLoaderAndAdder( bdvHandle ).run() ).install( "AddSourceFromFile", "ctrl L" );
	}
}