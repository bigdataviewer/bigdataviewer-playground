package sc.fiji.bdvpg.bdv.projector;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class AccumulateAverageProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{
	public static AccumulateProjectorFactory< ARGBType > factory = new AccumulateProjectorFactory< ARGBType >()
	{
		@Override
		public AccumulateAverageProjectorARGB createAccumulateProjector(
				final ArrayList< VolatileProjector > sourceProjectors,
				final ArrayList< Source< ? > > sources,
				final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
				final RandomAccessibleInterval< ARGBType > targetScreenImage,
				final int numThreads,
				final ExecutorService executorService )
		{
			return new AccumulateAverageProjectorARGB(
					sourceProjectors,
					sources,
					sourceScreenImages,
					targetScreenImage,
					numThreads,
					executorService );
		}
	};

	public AccumulateAverageProjectorARGB(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< Source< ? > > sources,
			final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		super( sourceProjectors, sourceScreenImages, target, numThreads, executorService );
	}

	@Override
	protected void accumulate(
			final Cursor< ? extends ARGBType >[] accesses,
			final ARGBType target )
	{
		int aAvg = 0, rAvg = 0, gAvg = 0, bAvg = 0, n = 0;
		int aAccu = 0, rAccu = 0, gAccu = 0, bAccu = 0;

		for ( final Cursor< ? extends ARGBType > access : accesses )
		{
			final int value = access.get().get();
			final int a = ARGBType.alpha( value );
			final int r = ARGBType.red( value );
			final int g = ARGBType.green( value );
			final int b = ARGBType.blue( value );

			if ( a == 0 )
			{
				continue;
			}

			aAvg += a;
			rAvg += r;
			gAvg += g;
			bAvg += b;
			n++;
		}

		if ( n > 0 )
		{
			aAvg /= n;
			rAvg /= n;
			gAvg /= n;
			bAvg /= n;
		}

		aAccu += aAvg;
		rAccu += rAvg;
		gAccu += gAvg;
		bAccu += bAvg;

		if ( aAccu > 255 )
			aAccu = 255;
		if ( rAccu > 255 )
			rAccu = 255;
		if ( gAccu > 255 )
			gAccu = 255;
		if ( bAccu > 255 )
			bAccu = 255;

		target.set( ARGBType.rgba( rAccu, gAccu, bAccu, aAccu ) );
	}

}
