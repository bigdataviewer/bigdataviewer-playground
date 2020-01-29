package sc.fiji.bdvpg.source;

import bdv.viewer.Source;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;

public class SourcePhysicalIntervalDeterminer
{
	private final Interval interval;

	public SourcePhysicalIntervalDeterminer( Source< ? > source )
	{
		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform( 0, 0, sourceTransform );

		final RandomAccessibleInterval< ? > rai = source.getSource( 0, 0 );

		interval = Intervals.smallestContainingInterval( sourceTransform.estimateBounds( rai ) );
	}

	public Interval getInterval()
	{
		return interval;
	}
}
