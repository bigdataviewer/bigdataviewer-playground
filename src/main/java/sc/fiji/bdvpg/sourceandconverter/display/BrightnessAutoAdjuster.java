package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.services.BdvService;

public class BrightnessAutoAdjuster implements Runnable
{
	private final SourceAndConverter sac;
	private final double cumulativeMinCutoff;
	private final double cumulativeMaxCutoff;
	private final int timePoint;



	public BrightnessAutoAdjuster( final SourceAndConverter sac, int timePoint )
	{
		this(sac, timePoint, 0.01, 0.99 );
	}

	public BrightnessAutoAdjuster( final SourceAndConverter sac, final int timePoint, final double cumulativeMinCutoff, final double cumulativeMaxCutoff )
	{
		this.sac = sac;
		this.cumulativeMinCutoff = cumulativeMinCutoff;
		this.cumulativeMaxCutoff = cumulativeMaxCutoff;
		this.timePoint = timePoint;
	}

	@Override
	public void run()
	{
		if ( !sac.getSpimSource().isPresent( timePoint ) )
			return;
		if ( !UnsignedShortType.class.isInstance( sac.getSpimSource().getType() ) )
			return;

		@SuppressWarnings( "unchecked" )
		final RandomAccessibleInterval< UnsignedShortType > img = ( RandomAccessibleInterval< UnsignedShortType > ) sac.getSpimSource().getSource( timePoint, sac.getSpimSource().getNumMipmapLevels() - 1 );
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

		//minMaxGroup.getMinBoundedValue().setCurrentValue( min );
		//minMaxGroup.getMaxBoundedValue().setCurrentValue( max );
		BdvService.getSourceDisplayService().getConverterSetup( sac ).setDisplayRange(min, max);
	}

}
