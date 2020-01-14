package sc.fiji.bdvpg.bdv;

import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.Bdv;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.state.SourceState;
import ij.IJ;
import net.imglib2.*;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;

import java.util.List;

/**
 * BDVUtils
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf, @tischi
 * 12 2019
 */
public class BdvUtils
{
    public static double getViewerVoxelSpacing( BdvHandle bdv )
    {
        final int windowWidth = getBdvWindowWidth( bdv );
        final int windowHeight = getBdvWindowHeight( bdv );

        final AffineTransform3D viewerTransform = new AffineTransform3D();
        bdv.getViewerPanel().getState().getViewerTransform( viewerTransform );

        final double[] physicalA = new double[ 3 ];
        final double[] physicalB = new double[ 3 ];

        viewerTransform.applyInverse( physicalA, new double[]{ 0, 0, 0} );
        viewerTransform.applyInverse( physicalB, new double[]{ 0, windowWidth, 0} );

        double viewerPhysicalWidth = LinAlgHelpers.distance( physicalA, physicalB );

        viewerTransform.applyInverse( physicalA, new double[]{ 0, 0, 0} );
        viewerTransform.applyInverse( physicalB, new double[]{ windowHeight, 0, 0} );

        double viewerPhysicalHeight = LinAlgHelpers.distance( physicalA, physicalB );

        final double viewerPhysicalVoxelSpacingX = viewerPhysicalWidth / windowWidth;
        final double viewerPhysicalVoxelSpacingY = viewerPhysicalHeight / windowHeight;

        System.out.println( "[DEBUG] windowWidth = " + windowWidth );
        System.out.println( "[DEBUG] windowHeight = " + windowHeight );
        System.out.println( "[DEBUG] viewerPhysicalWidth = " + viewerPhysicalWidth );
        System.out.println( "[DEBUG] viewerPhysicalHeight = " + viewerPhysicalHeight );
        System.out.println( "[DEBUG] viewerPhysicalVoxelSpacingX = " + viewerPhysicalVoxelSpacingX );
        System.out.println( "[DEBUG] viewerPhysicalVoxelSpacingY = " + viewerPhysicalVoxelSpacingY );

        return viewerPhysicalVoxelSpacingX;
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
     * Returns the highest level where the sourceandconverter voxel spacings are <= the requested ones.
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

    public static MinMaxGroup getMinMaxGroup( BdvHandle bdvHandle, Source< ? > source )
    {
        final int sourceIndex = getSourceIndex( bdvHandle, source );

        System.out.println("sourceINdex = "+ sourceIndex);

        final MinMaxGroup minmax = bdvHandle.getSetupAssignments().getMinMaxGroups().get( sourceIndex );

        return minmax;
    }


    /**
     * TODO: remove the print statements or make them some debug mode.
     *
     *
     * @param bdv
     * @param source
     * @return
     */
    public static int getSourceIndex( Bdv bdv, Source< ? > source )
    {
        final List< SourceState< ? > > sources =
                bdv.getBdvHandle().getViewerPanel().getState().getSources();

        System.out.println("look for : "+  source);

        for ( int i = 0; i < sources.size(); ++i ) {
            final Source< ? > bdvSource = sources.get( i ).getSpimSource();

            System.out.println("c:"+ bdvSource );

            if ( bdvSource instanceof TransformedSource )
            {
                final Source wrappedSource = ( ( TransformedSource ) bdvSource ).getWrappedSource();

                if ( wrappedSource.equals( source ) )
                    return i;
            }
            else
            {
                if ( bdvSource.equals( source ) )
                    return i;
            }
        }

        return -1;
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


}
