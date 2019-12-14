package sc.fiji.bdv.sources.read;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;
import sc.fiji.bdv.BdvUtils;
import sc.fiji.bdv.navigate.ViewerTransformAdjuster;

public class SourceAdder implements Runnable
{
	private final BdvHandle bdvHandle;
	private final Source source;
	private final boolean autoContrast;
	private final boolean adjustViewerTransform;

	public SourceAdder( BdvHandle bdvHandle, Source source )
	{
		this( bdvHandle, source, true, true );
	}

	public SourceAdder( BdvHandle bdvHandle, Source source,
						boolean autoContrast, boolean adjustViewerTransform )
	{
		this.bdvHandle = bdvHandle;
		this.source = source;
		this.autoContrast = autoContrast;
		this.adjustViewerTransform = adjustViewerTransform;
	}

	@Override
	public void run()
	{
		BdvFunctions.show( source, BdvOptions.options().addTo( bdvHandle ) );

		if ( autoContrast )
		{
			final int numSources = bdvHandle.getSetupAssignments()
					.getMinMaxGroups().size();

			final int lastSource = numSources - 1;

			BdvUtils.initBrightness( bdvHandle, 0.01,
					0.99, lastSource );
		}

		if ( adjustViewerTransform )
		{
			final ViewerTransformAdjuster adjuster =
					new ViewerTransformAdjuster( bdvHandle, source );
			adjuster.run();
		}
	}
}
