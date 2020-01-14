package sc.fiji.bdvpg.bdv.projector;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService;
import sc.fiji.bdvpg.services.BdvService;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class AccumulateMixedProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{
	public static AccumulateProjectorFactory< ARGBType > factory = new AccumulateProjectorFactory< ARGBType >()
	{
		@Override
		public AccumulateMixedProjectorARGB createAccumulateProjector(
				final ArrayList< VolatileProjector > sourceProjectors,
				final ArrayList< Source< ? > > sources,
				final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
				final RandomAccessibleInterval< ARGBType > targetScreenImage,
				final int numThreads,
				final ExecutorService executorService )
		{
			return new AccumulateMixedProjectorARGB(
					sourceProjectors,
					sources,
					sourceScreenImages,
					targetScreenImage,
					numThreads,
					executorService );
		}
	};

	private final ArrayList< Source< ? > > sourceList;

	public AccumulateMixedProjectorARGB(
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< Source< ? > > sources,
			final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		super( sourceProjectors, sourceScreenImages, target, numThreads, executorService );
		this.sourceList = sources;
	}

	@Override
	protected void accumulate(
			final Cursor< ? extends ARGBType >[] accesses,
			final ARGBType target )
	{
		int aAvg = 0, rAvg = 0, gAvg = 0, bAvg = 0, n = 0;
		int aAccu = 0, rAccu = 0, gAccu = 0, bAccu = 0;

		int sourceIndex = 0;

		final Map< SourceAndConverter, Map< String, Object > > sourceAndConverterToMetadata = BdvService.getSourceAndConverterService().getSourceAndConverterToMetadata();

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

			String projectionMode = getProjectionMode( sourceIndex, sourceAndConverterToMetadata );

			if ( projectionMode.equals( "Sum" ) )
			{
				aAccu += a;
				rAccu += r;
				gAccu += g;
				bAccu += b;
			}
			else if ( projectionMode.equals( "Avg" ))
			{
				aAvg += a;
				rAvg += r;
				gAvg += g;
				bAvg += b;
				n++;
			}

			sourceIndex++;
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

	private String getProjectionMode( int sourceIndex, Map< SourceAndConverter, Map< String, Object > > sourceAndConverterToMetadata )
	{
		Source< ? > source = sourceList.get( sourceIndex );
		if ( source instanceof TransformedSource )
			source = (( TransformedSource )source).getWrappedSource();

		String projectionMode = "Sum";

		for ( SourceAndConverter sac : sourceAndConverterToMetadata.keySet() )
		{
			if ( sac.getSpimSource().equals( source ) )
			{
				final Set< String > metadata = sourceAndConverterToMetadata.get( sac ).keySet();
				if ( metadata.contains( "ProjectionMode" ) )
				{
					projectionMode = (String) sourceAndConverterToMetadata.get( sac ).get( "ProjectionMode" );
				}
			}
		}
		return projectionMode;
	}

}
