package sc.fiji.bdv.sources.read;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;

public class SourceAdder implements Runnable
{
	private final BdvHandle bdvHandle;
	private final Source source;
	private final boolean autoContrast;

	public SourceAdder( BdvHandle bdvHandle, Source source )
	{
		this( bdvHandle, source, true );
	}

	public SourceAdder( BdvHandle bdvHandle, Source source, boolean autoContrast )
	{
		this.bdvHandle = bdvHandle;
		this.source = source;
		this.autoContrast = autoContrast;
	}

	@Override
	public void run()
	{
		BdvFunctions.show( source, BdvOptions.options().addTo( bdvHandle ) );

		final int numSources = bdvHandle.getSetupAssignments().getMinMaxGroups().size();
		//BdvUtils.initBrightness( bdvHandle, 0.01, 0.99, numSources - 1  );
	}
}
