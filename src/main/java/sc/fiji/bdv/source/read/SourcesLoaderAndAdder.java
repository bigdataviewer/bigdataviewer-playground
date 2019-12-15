package sc.fiji.bdv.source.read;

import bdv.util.BdvHandle;
import bdv.viewer.Source;

public class SourcesLoaderAndAdder implements Runnable
{
	private final BdvHandle bdvHandle;
	private String[] filePaths;
	private boolean autoContrast = true;
	private boolean autoAdjustViewerTransform = true;

	public SourcesLoaderAndAdder( BdvHandle bdvHandle, String filePath )
	{
		this( bdvHandle, new String[]{ filePath } );
	}

	public SourcesLoaderAndAdder( BdvHandle bdvHandle, String[] filePaths )
	{
		this.bdvHandle = bdvHandle;
		this.filePaths = filePaths;
	}

	public void setAutoContrast( boolean autoContrast )
	{
		this.autoContrast = autoContrast;
	}

	public void setAutoAdjustViewerTransform( boolean autoAdjustViewerTransform )
	{
		this.autoAdjustViewerTransform = autoAdjustViewerTransform;
	}

	@Override
	public void run()
	{
		for ( String filePath : filePaths )
		{
			final SourceLoader sourceLoader = new SourceLoader( filePath );
			sourceLoader.run();
			final Source source = sourceLoader.getSource( 0 );

			new SourceAdder( bdvHandle, source, autoContrast, autoAdjustViewerTransform ).run();
		}
	}
}