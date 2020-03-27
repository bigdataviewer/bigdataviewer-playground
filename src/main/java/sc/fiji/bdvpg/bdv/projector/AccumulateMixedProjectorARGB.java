package sc.fiji.bdvpg.bdv.projector;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjector;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static sc.fiji.bdvpg.bdv.projector.Projection.*;

public class AccumulateMixedProjectorARGB extends AccumulateProjector< ARGBType, ARGBType >
{
	private final String[] projectionModes;
	private int[] sourceOrder;

	public AccumulateMixedProjectorARGB(
			BdvHandle bdvHandle,
			final ArrayList< VolatileProjector > sourceProjectors,
			final ArrayList< Source< ? > > sources,
			final ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			final RandomAccessibleInterval< ARGBType > target,
			final int numThreads,
			final ExecutorService executorService )
	{
		super( sourceProjectors, sourceScreenImages, target, numThreads, executorService );
		this.projectionModes = getProjectionModes( bdvHandle, sources );
		sourceOrder = getSourcesOrder( projectionModes );
	}

	public static int[] getSourcesOrder( String[] projectionModes )
	{
		boolean containsExclusiveProjectionMode = false;
		for ( String projectionMode : projectionModes )
		{
			if ( projectionMode.contains( Projection.PROJECTION_MODE_OCCLUDING ) )
			{
				containsExclusiveProjectionMode = true;
				break;
			}
		}

		final int numSources = projectionModes.length;

		int[] sourceOrder = new int[ numSources ];
		if ( containsExclusiveProjectionMode )
		{
			int j = 0;

			// first the exclusive ones
			for ( int i = 0; i < numSources; i++ )
				if ( projectionModes[ i ].contains( Projection.PROJECTION_MODE_OCCLUDING ) )
					sourceOrder[ j++ ] = i;

			// then the others
			for ( int i = 0; i < numSources; i++ )
				if ( ! projectionModes[ i ].contains( Projection.PROJECTION_MODE_OCCLUDING ) )
					sourceOrder[ j++ ] = i;
		}
		else
		{
			for ( int i = 0; i < numSources; i++ )
				sourceOrder[ i ] = i;
		}

		return sourceOrder;
	}

	@Override
	protected void accumulate(
			final Cursor< ? extends ARGBType >[] accesses,
			final ARGBType target )
	{
		final int argbIndex = getArgbIndex( accesses, sourceOrder, projectionModes );
		target.set( argbIndex );
	}

	public static int getArgbIndex( Cursor< ? extends ARGBType >[] accesses, int[] sourceOrder, String[] projectionModes )
	{
		int aAvg = 0, rAvg = 0, gAvg = 0, bAvg = 0, n = 0;
		int aAccu = 0, rAccu = 0, gAccu = 0, bAccu = 0;

		boolean skipNonExclusiveSources = false;

		for ( int sourceIndex : sourceOrder )
		{
			final int argb = accesses[ sourceIndex ].get().get();
			final int a = ARGBType.alpha( argb );
			final int r = ARGBType.red( argb );
			final int g = ARGBType.green( argb );
			final int b = ARGBType.blue( argb );

			if ( a == 0 ) continue;

			final boolean isExclusive = projectionModes[ sourceIndex ].contains( PROJECTION_MODE_OCCLUDING );

			if ( a != 0 && isExclusive ) skipNonExclusiveSources = true;

			if ( skipNonExclusiveSources && ! isExclusive ) continue;

			if ( projectionModes[ sourceIndex ].contains( Projection.PROJECTION_MODE_SUM ) )
			{
				aAccu += a;
				rAccu += r;
				gAccu += g;
				bAccu += b;
			}
			else if ( projectionModes[ sourceIndex ].contains( Projection.PROJECTION_MODE_AVG ) )
			{
				aAvg += a;
				rAvg += r;
				gAvg += g;
				bAvg += b;
				n++;
			}

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

		return ARGBType.rgba( rAccu, gAccu, bAccu, aAccu );
	}

	public static String[] getProjectionModes( BdvHandle bdvHandle, ArrayList< Source< ? > > sources )
	{
		// We need to reconstitute the sequence of action that lead to the current indexes

		// Getting the sources present in the BdvHandle
		List<SourceAndConverter<?>> sacsInBdvHandle = SourceAndConverterServices
				.getSourceAndConverterDisplayService()
				.getSourceAndConverterOf(bdvHandle);

		// Fetching the indexes of visible sources in the BdvHandle
		List<Integer> visibleIndexes = bdvHandle.getViewerPanel().getState().getVisibleSourceIndices();
		// In ascending order
		Collections.sort(visibleIndexes);

		SourceAndConverter[] visibleSacArray = new SourceAndConverter[visibleIndexes.size()];

		for (int idx = 0; idx<visibleIndexes.size(); idx++) {
			visibleSacArray[idx] = sacsInBdvHandle.get(visibleIndexes.get(idx));
		}

		final List< SourceAndConverter > visibleSacs = Arrays.asList(visibleSacArray);//SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters();
		final String[] projectionModes = getProjectionModes( visibleSacs );

		return projectionModes;
	}

	public static String[] getProjectionModes( List< SourceAndConverter > visibleSacs )
	{
		final int numSources = visibleSacs.size();

		final String[] projectionModes = new String[ numSources ];

		final ISourceAndConverterService sacService = SourceAndConverterServices.getSourceAndConverterService();

		for ( int sourceIndex = 0; sourceIndex < numSources; sourceIndex++ )
		{
			final SourceAndConverter sac = visibleSacs.get( sourceIndex );

			final String projectionMode = (String) sacService.getMetadata( sac, PROJECTION_MODE );

			if ( projectionMode == null ) {
				projectionModes[ sourceIndex ] = PROJECTION_MODE_SUM;
			} else {
				projectionModes[ sourceIndex ] = projectionMode;
			}

		}
		return projectionModes;
	}

}
