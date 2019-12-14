package sc.fiji.bdv;

import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.state.ViewerState;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

public class BdvUtils
{
	public static void initBrightness(
			final BdvHandle bdvHandle,
			final double cumulativeMinCutoff,
			final double cumulativeMaxCutoff,
			int sourceIndex )
	{
		final ViewerState state = bdvHandle.getViewerPanel().getState();
		final SetupAssignments setupAssignments = bdvHandle.getSetupAssignments();

		final Source< ? > source = state.getSources().get( sourceIndex ).getSpimSource();
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
		final MinMaxGroup minmax = setupAssignments.getMinMaxGroups().get( sourceIndex );
		minmax.getMinBoundedValue().setCurrentValue( min );
		minmax.getMaxBoundedValue().setCurrentValue( max );
	}
}
