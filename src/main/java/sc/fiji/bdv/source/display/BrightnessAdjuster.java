package sc.fiji.bdv.source.display;

import bdv.tools.brightness.MinMaxGroup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.state.ViewerState;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import sc.fiji.bdv.BdvUtils;

public class BrightnessAdjuster implements Runnable
{
	private final BdvHandle bdvHandle;
	private final Source< ? > source;
	private final double cumulativeMinCutoff;
	private final double cumulativeMaxCutoff;

	public BrightnessAdjuster( final BdvHandle bdvHandle, final Source< ? > source, final double cumulativeMinCutoff, final double cumulativeMaxCutoff )
	{
		this.bdvHandle = bdvHandle;
		this.source = source;
		this.cumulativeMinCutoff = cumulativeMinCutoff;
		this.cumulativeMaxCutoff = cumulativeMaxCutoff;
	}

	@Override
	public void run()
	{
		final ViewerState state = bdvHandle.getViewerPanel().getState();
		final MinMaxGroup minMaxGroup = BdvUtils.getMinMaxGroup( bdvHandle, source );

		final int timepoint = state.getCurrentTimepoint();
		if ( !source.isPresent( timepoint ) )
			return;
		if ( !UnsignedShortType.class.isInstance( source.getType() ) )
			return;
		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedShortType > img = ( RandomAccessibleInterval< UnsignedShortType > ) source.getSource( timepoint, source.getNumMipmapLevels() - 1 );
		final long z = ( img.min( 2 ) + img.max( 2 ) + 1 ) / 2;

		final int numBins = 6535;
		final Histogram1d< UnsignedShortType > histogram = new Histogram1d<>( Views.iterable( Views.hyperSlice( img, 2, z ) ), new Real1dBinMapper< UnsignedShortType >( 0, 65535, numBins, false ) );
		final DiscreteFrequencyDistribution dfd = histogram.dfd();
		final long[] bin = new long[] { 0 };
		double cumulative = 0;
		int i = 0;
		for ( ; i < numBins && cumulative < cumulativeMinCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int min = i * 65535 / numBins;
		for ( ; i < numBins && cumulative < cumulativeMaxCutoff; ++i )
		{
			bin[ 0 ] = i;
			cumulative += dfd.relativeFrequency( bin );
		}
		final int max = i * 65535 / numBins;

		minMaxGroup.getMinBoundedValue().setCurrentValue( min );
		minMaxGroup.getMaxBoundedValue().setCurrentValue( max );
	}

}
