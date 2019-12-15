package sc.fiji.bdv.source.read;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;
import sc.fiji.bdv.navigate.ViewerTransformAdjuster;

public class SourceAdder implements Runnable
{
	private final BdvHandle bdvHandle;
	private final Source source;

	public SourceAdder( BdvHandle bdvHandle, Source source )
	{
		this.bdvHandle = bdvHandle;
		this.source = source;
	}

	@Override
	public void run()
	{
		BdvFunctions.show( source, BdvOptions.options().addTo( bdvHandle ) );
	}
}
