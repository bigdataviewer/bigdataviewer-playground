package sc.fiji.bdv;

import bdv.util.Bdv;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;

import java.util.ArrayList;
import java.util.List;

public abstract class BdvUtils
{
	public static int getSourceIndex( Bdv bdv, Source< ? > source )
	{
		final List< SourceState< ? > > sources =
				bdv.getBdvHandle().getViewerPanel().getState().getSources();

		for ( int i = 0; i < sources.size(); ++i )
			if ( sources.get( i ).getSpimSource().equals( source ) )
				return i;

		return -1;
	}

	public static Source< ? > getSource( Bdv bdv, int sourceIndex )
	{
		final List< SourceState< ? > > sources =
				bdv.getBdvHandle().getViewerPanel().getState().getSources();

		return sources.get( sourceIndex ).getSpimSource();
	}

	public static Source< ? > getVolatileSource( Bdv bdv, int sourceIndex )
	{
		final List< SourceState< ? > > sources =
				bdv.getBdvHandle().getViewerPanel().getState().getSources();

		return sources.get( sourceIndex ).asVolatile().getSpimSource();
	}

	public static double[] getDisplayRange( Bdv bdv, int sourceId )
	{
		final double displayRangeMin = bdv.getBdvHandle().getSetupAssignments()
				.getConverterSetups().get( sourceId ).getDisplayRangeMin();
		final double displayRangeMax = bdv.getBdvHandle().getSetupAssignments()
				.getConverterSetups().get( sourceId ).getDisplayRangeMax();

		return new double[]{ displayRangeMin, displayRangeMax };
	}

	public static String getSourceName( Bdv bdv, int sourceId )
	{
		return bdv.getBdvHandle().getViewerPanel().getState().getSources()
				.get( sourceId ).getSpimSource().getName();
	}

	public static int getSourceIndex( Bdv bdv, String sourceName )
	{
		return getSourceNames( bdv ).indexOf( sourceName );
	}

	public static ArrayList< String > getSourceNames( Bdv bdv )
	{
		final ArrayList< String > sourceNames = new ArrayList<>();

		final List< SourceState< ? > > sources = bdv.getBdvHandle().getViewerPanel().getState().getSources();

		for ( SourceState source : sources )
			sourceNames.add( source.getSpimSource().getName() );

		return sourceNames;
	}

	public static void repaint( Bdv bdv )
	{
		bdv.getBdvHandle().getViewerPanel().requestRepaint();
	}
}
