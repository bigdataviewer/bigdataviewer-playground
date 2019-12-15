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

	public SourcesLoaderAndAdder( BdvHandle bdvHandle, String filePath )
	{
		this( bdvHandle, new String[]{ filePath } );
	}

	public SourcesLoaderAndAdder( BdvHandle bdvHandle, String[] filePaths )
	{
		this.bdvHandle = bdvHandle;
		this.filePaths = filePaths;
	}

	@Override
	public void run()
	{
		for ( String filePath : filePaths )
		{
			final SourceLoader sourceLoader = new SourceLoader( filePath );
			sourceLoader.run();
			final Source source = sourceLoader.getSource( 0 );

			new SourceAdder( bdvHandle, source ).run();
		}
	}
}