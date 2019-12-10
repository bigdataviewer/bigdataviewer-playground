package sc.fiji.bdv.sources.read;

import bdv.util.BdvHandle;
import bdv.viewer.Source;

public class SourceLoaderAndAdder implements Runnable
{
	private final BdvHandle bdvHandle;
	private final String filePath;

	public SourceLoaderAndAdder( BdvHandle bdvHandle, String filePath )
	{
		this.bdvHandle = bdvHandle;

		this.filePath = filePath;
	}

	@Override
	public void run()
	{
		final Source source = new SourceLoader( filePath ).getSource( 0 );
		new SourceAdder( bdvHandle, source ).run();
	}
}
