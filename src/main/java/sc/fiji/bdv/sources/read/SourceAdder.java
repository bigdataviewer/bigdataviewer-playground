package sc.fiji.bdv.sources.read;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;
import sc.fiji.bdv.BdvUtils;
import sc.fiji.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdv.sources.display.BrightnessAdjuster;

public class SourceAdder implements Runnable
{
	private final BdvHandle bdvHandle;
	private final Source source;
	private final boolean autoContrast;
	private final boolean autoAdjustViewerTransform;

	public SourceAdder( BdvHandle bdvHandle, Source source )
	{
		this( bdvHandle, source, true, true );
	}

	public SourceAdder( BdvHandle bdvHandle, Source source,
						boolean autoContrast, boolean autoAdjustViewerTransform )
	{
		this.bdvHandle = bdvHandle;
		this.source = source;
		this.autoContrast = autoContrast;
		this.autoAdjustViewerTransform = autoAdjustViewerTransform;
	}

	@Override
	public void run()
	{
		BdvFunctions.show( source, BdvOptions.options().addTo( bdvHandle ) );

		final int numSources = bdvHandle.getSetupAssignments().getMinMaxGroups().size();
		//BdvUtils.initBrightness( bdvHandle, 0.01, 0.99, numSources - 1  );
		/*
		if ( autoContrast )
		{
			new BrightnessAdjuster( bdvHandle, source, 0.01, 0.99 ).run();
		}
		*/
		if ( autoAdjustViewerTransform )
		{
			new ViewerTransformAdjuster( bdvHandle, source ).run();
		}
	}
}
