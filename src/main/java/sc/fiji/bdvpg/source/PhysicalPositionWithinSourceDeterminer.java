package sc.fiji.bdvpg.source;

import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import sc.fiji.bdvpg.bdv.BdvUtils;

import java.util.function.Function;

public class PhysicalPositionWithinSourceDeterminer implements Function<Source,Boolean>
{
	private final RealPoint realPointPhyiscalUnits;

	public PhysicalPositionWithinSourceDeterminer( RealPoint realPointPhysicalUnits )
	{
		this.realPointPhyiscalUnits = realPointPhysicalUnits;
	}

	private static RealPoint getPositionInSourceVoxelUnits(
			Source source,
			RealPoint realPointPhysicalUnits,
			int t,
			int level )
	{
		int n = 3;

		final AffineTransform3D sourceTransform =
				BdvUtils.getSourceTransform( source, t, level );

		final RealPoint sourcePositionVoxelUnits = new RealPoint( n );

		sourceTransform.inverse().apply(
				realPointPhysicalUnits, sourcePositionVoxelUnits );

		return sourcePositionVoxelUnits;
	}

	public static boolean isPositionInSourceInterval(
			Source< ? > source,
			RealPoint position,
			int t,
			int level )
	{
		final RandomAccessibleInterval< ? > rai = source.getSource( t, level );
		final VoxelDimensions voxelDimensions = source.getVoxelDimensions();

		/**
		 * TODO: make the interval larger, taking into acount the voxel-dimensions;
		 * this is particularly necessary for sources that are practically 2D
		 * ...one could also check whether the 3rd dimension of the rai is a singleton
 		 */

		final boolean contains = Intervals.contains( rai, position );

		return contains;
	}

	@Override
	public Boolean apply( Source source )
	{
		final RealPoint realPointVoxelUnits = getPositionInSourceVoxelUnits( source, this.realPointPhyiscalUnits, 0, 0 );
		final boolean positionInSourceInterval = isPositionInSourceInterval( source, realPointVoxelUnits, 0, 0 );

		return positionInSourceInterval;
	}
}
