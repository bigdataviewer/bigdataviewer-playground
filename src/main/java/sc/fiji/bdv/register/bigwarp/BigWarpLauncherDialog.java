package sc.fiji.bdv.register.bigwarp;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import ij.gui.GenericDialog;
import sc.fiji.bdv.BdvUtils;

import java.util.List;

public class BigWarpLauncherDialog
{
	private final BdvHandle bdvHandle;
	private final List< Integer > sourceIndices;

	private Source< ? > movingVolatileSource;
	private Source< ? > fixedVolatileSource;
	private double[] displayRangeMovingSource;
	private double[] displayRangeFixedSource;

	/**
	 *
	 * @param bdvHandle
	 * @param sourceIndices the source selection dialog will only show sources that are present in this list.
	 */
	public BigWarpLauncherDialog( BdvHandle bdvHandle, List< Integer > sourceIndices )
	{
		this.bdvHandle = bdvHandle;
		this.sourceIndices = sourceIndices;
	}

	public BigWarpLauncherDialog( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
		sourceIndices = bdvHandle.getViewerPanel().getState().getVisibleSourceIndices();
	}

	public boolean showDialog( )
	{
		final GenericDialog gd = new GenericDialog( "Register Images in Big Warp" );

		final String[] sourceNames = new String[ sourceIndices.size() ];
		for ( int i = 0; i < sourceNames.length ; i++ )
			sourceNames[ i ]  = BdvUtils.getSourceName( bdvHandle, sourceIndices.get( i ) );

		gd.addChoice( "Moving image", sourceNames, sourceNames[ 0 ] );
		gd.addChoice( "Fixed image", sourceNames, sourceNames[ 1 ] );

		gd.showDialog();
		if ( gd.wasCanceled() ) return false;

		final int movingSourceIndex = BdvUtils.getSourceIndex( bdvHandle, gd.getNextChoice() );
		movingVolatileSource = BdvUtils.getVolatileSource( bdvHandle, movingSourceIndex );
		displayRangeMovingSource = BdvUtils.getDisplayRange( bdvHandle, movingSourceIndex );

		final int fixedSourceIndex = BdvUtils.getSourceIndex( bdvHandle, gd.getNextChoice() );
		fixedVolatileSource = BdvUtils.getVolatileSource( bdvHandle, fixedSourceIndex );
		displayRangeFixedSource = BdvUtils.getDisplayRange( bdvHandle, fixedSourceIndex );

		return true;
	}

	public Source< ? > getMovingVolatileSource()
	{
		return movingVolatileSource;
	}

	public Source< ? > getFixedVolatileSource()
	{
		return fixedVolatileSource;
	}

	public double[] getDisplayRangeMovingSource()
	{
		return displayRangeMovingSource;
	}

	public double[] getDisplayRangeFixedSource()
	{
		return displayRangeFixedSource;
	}
}
