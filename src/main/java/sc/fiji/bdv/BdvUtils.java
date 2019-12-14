package sc.fiji.bdv;

import bdv.VolatileSpimSource;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.state.SourceState;
import bdv.viewer.state.ViewerState;
import net.imglib2.*;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import net.imglib2.view.Views;

import java.util.List;
import java.util.logging.Logger;

/**
 * BdvUtils
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf, @tischi
 * 12 2019
 */
public class BdvUtils {
    /**
     * TODO: does that make sense?
     *
     * @param bdv
     * @return
     */
    public static double[] getViewerVoxelSpacing( BdvHandle bdv )
    {
        final AffineTransform3D viewerTransform = new AffineTransform3D();
        bdv.getViewerPanel().getState().getViewerTransform( viewerTransform );

        final double[] zeroCanvas = { 0, 0, 0 };
        final double[] zeroGlobal = new double[ 3 ];

        final double[] oneCanvas = { 1, 1, 1 };
        final double[] oneGlobal = new double[ 3 ];

        viewerTransform.applyInverse( zeroGlobal, zeroCanvas );
        viewerTransform.applyInverse( oneGlobal, oneCanvas );

        final double[] viewerVoxelSpacing = new double[ 3 ];
        for ( int d = 0; d < 3; d++ )
            viewerVoxelSpacing[ d ] = Math.abs( zeroGlobal[ d ] - oneGlobal[ d ]);

        return viewerVoxelSpacing;
    }


    public static int getBdvWindowWidth( BdvHandle bdvHandle )
    {
        return bdvHandle.getViewerPanel().getDisplay().getWidth();
    }


    public static int getBdvWindowHeight( BdvHandle bdvHandle )
    {
        return bdvHandle.getViewerPanel().getDisplay().getHeight();
    }

    public static List< Integer > getVisibleSourceIndices(BdvHandle bdvHandle )
    {
        return bdvHandle.getViewerPanel().getState().getVisibleSourceIndices();
    }


    public static boolean isSourceIntersectingCurrentView( BdvHandle bdv, int sourceIndex, boolean is2D )
    {
        final Interval interval = getSourceGlobalBoundingInterval( bdv, sourceIndex );

        final Interval viewerInterval =
                Intervals.smallestContainingInterval(
                        getViewerGlobalBoundingInterval( bdv ) );

        boolean intersects = false;
        if (is2D) {
            intersects = !Intervals.isEmpty(
                    intersect2D(interval, viewerInterval));
        } else {
            intersects = ! Intervals.isEmpty(
                    Intervals.intersect( interval, viewerInterval ) );
        }
        return intersects;
    }

    public static FinalInterval intersect2D( final Interval intervalA, final Interval intervalB )
    {
        assert intervalA.numDimensions() == intervalB.numDimensions();

        final long[] min = new long[ 2 ];
        final long[] max = new long[ 2 ];
        for ( int d = 0; d < 2; ++d )
        {
            min[ d ] = Math.max( intervalA.min( d ), intervalB.min( d ) );
            max[ d ] = Math.min( intervalA.max( d ), intervalB.max( d ) );
        }
        return new FinalInterval( min, max );
    }

    public static FinalRealInterval getViewerGlobalBoundingInterval(BdvHandle bdvHandle )
    {
        AffineTransform3D viewerTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().getState().getViewerTransform( viewerTransform );
        viewerTransform = viewerTransform.inverse();
        final long[] min = new long[ 3 ];
        final long[] max = new long[ 3 ];
        max[ 0 ] = bdvHandle.getViewerPanel().getWidth();
        max[ 1 ] = bdvHandle.getViewerPanel().getHeight();
        final FinalRealInterval realInterval
                = viewerTransform.estimateBounds( new FinalInterval( min, max ) );
        return realInterval;
    }

    public static Interval getSourceGlobalBoundingInterval( BdvHandle bdvHandle, int sourceId )
    {
        final AffineTransform3D sourceTransform =
                getSourceTransform( bdvHandle, sourceId );
        final RandomAccessibleInterval< ? > rai =
                getRandomAccessibleInterval( bdvHandle, sourceId );
        final Interval interval =
                Intervals.smallestContainingInterval( sourceTransform.estimateBounds( rai ) );
        return interval;
    }

    public static AffineTransform3D getSourceTransform( BdvHandle bdvHandle, int sourceId )
    {
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().getState().getSources().get( sourceId ).getSpimSource().getSourceTransform( 0, 0, sourceTransform );
        return sourceTransform;
    }

    public static RandomAccessibleInterval< ? > getRandomAccessibleInterval( BdvHandle bdvHandle, int sourceId )
    {
        return bdvHandle.getViewerPanel().getState().getSources().get( sourceId ).getSpimSource().getSource( 0, 0 );
    }

    public static Source< ? > getSource( BdvHandle bdvHandle, int sourceIndex )
    {
        final List<SourceState< ? >> sources = bdvHandle.getViewerPanel().getState().getSources();

        return sources.get( sourceIndex ).getSpimSource();
    }



    /**
     * Returns the highest level where the sources voxel spacings are <= the requested ones.
     *
     *
     * @param source
     * @param voxelSpacings
     * @return
     */
    public static int getLevel( Source< ? > source, double... voxelSpacings )
    {
        final int numMipmapLevels = source.getNumMipmapLevels();
        final int numDimensions = voxelSpacings.length;

        for ( int level = numMipmapLevels - 1; level >= 0 ; level-- )
        {
            final double[] calibration = getCalibration( source, level );

            boolean allSpacingsSmallerThanRequested = true;

            for ( int d = 0; d < numDimensions; d++ )
            {
                if ( calibration[ d ] > voxelSpacings[ d ] )
                    allSpacingsSmallerThanRequested = false;
            }

            if ( allSpacingsSmallerThanRequested )
                return level;
        }
        return 0;
    }

    public static AffineTransform3D getSourceTransform( Source source, int t, int level )
    {
        AffineTransform3D sourceTransform = new AffineTransform3D();
        source.getSourceTransform( t, level, sourceTransform );
        return sourceTransform;
    }


    public static double[] getCalibration( Source source, int level )
    {
        final AffineTransform3D sourceTransform = new AffineTransform3D();

        source.getSourceTransform( 0, level, sourceTransform );

        final double[] calibration = getScale( sourceTransform );

        return calibration;
    }


    public static void repaint( BdvHandle bdvHandle )
    {
        bdvHandle.getViewerPanel().requestRepaint();
    }

    public static double[] getScale(AffineTransform3D sourceTransform) {
        double[] calibration = new double[3];

        for(int d = 0; d < 3; ++d) {
            double[] vector = new double[3];

            for(int i = 0; i < 3; ++i) {
                vector[i] = sourceTransform.get(d, i);
            }

            calibration[d] = LinAlgHelpers.length(vector);
        }

        return calibration;
    }


    public static ARGBType getSourceColor(BdvHandle bdvHandle, int sourceId )
    {
        return bdvHandle.getSetupAssignments().getConverterSetups().get( sourceId ).getColor();
    }

    public static double[] getDisplayRange( BdvHandle bdvHandle, int sourceId )
    {
        final double displayRangeMin = bdvHandle.getSetupAssignments()
                .getConverterSetups().get( sourceId ).getDisplayRangeMin();
        final double displayRangeMax = bdvHandle.getSetupAssignments()
                .getConverterSetups().get( sourceId ).getDisplayRangeMax();

        return new double[]{ displayRangeMin, displayRangeMax };
    }

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
