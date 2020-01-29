package bdv.util;

import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;

public class IntervalUtils
{
	public static FinalRealInterval expand( final Interval interval, final double[] border )
	{
		final int n = interval.numDimensions();
		final double[] min = new double[ n ];
		final double[] max = new double[ n ];
		interval.realMin( min );
		interval.realMax( max );
		for ( int d = 0; d < n; ++d )
		{
			min[ d ] -= border[ d ];
			max[ d ] += border[ d ];
		}
		return new FinalRealInterval( min, max );
	}
}
