package sc.fiji.bdvpg.bdv.source.view;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;

public class SourceViewer implements Runnable
{
	private final Source source;
	private BdvHandle bdvHandle;

	public SourceViewer( Source source )
	{
		this.source = source;
	}

	@Override
	public void run()
	{
		bdvHandle = BdvFunctions.show( source ).getBdvHandle();
	}

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}
}
