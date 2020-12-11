/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.bdvpg.bdv;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import net.imglib2.*;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;

/**
 * BDVUtils
 * Author: @haesleinhuepf, @tischi
 * 12 2019
 */
public class BdvUtils
{
    /**
     * Creates a viewer transform with a new center position.
     *
     * Author: @tischi
     * - 11 2020
     *
     * @param bdvHandle
     *          the bdvHandle, used to fetch the current viewerTransform and the current window size
     * @param xyz
     * 			target coordinates for the new center in physical units
     * @return
     *          viewerTransform that keeps the orientation of the current viewerTransform but with a shifted center
     */
    public static AffineTransform3D getViewerTransformWithNewCenter( BdvHandle bdvHandle, double[] xyz )
    {
        final AffineTransform3D currentViewerTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( currentViewerTransform );

        AffineTransform3D adaptedViewerTransform = currentViewerTransform.copy();

        // ViewerTransform notes:
        // - applyInverse: coordinates in viewer => coordinates in image
        // - apply: coordinates in image => coordinates in viewer

        final double[] targetPositionInViewerInPixels = new double[ 3 ];
        currentViewerTransform.apply( xyz, targetPositionInViewerInPixels );

        for ( int d = 0; d < 3; d++ )
        {
            targetPositionInViewerInPixels[ d ] *= -1;
        }

        adaptedViewerTransform.translate( targetPositionInViewerInPixels );

        final double[] windowCentreInViewerInPixels = new double[ 3 ];
        windowCentreInViewerInPixels[ 0 ] = bdvHandle.getViewerPanel().getDisplay().getWidth() / 2.0;
        windowCentreInViewerInPixels[ 1 ] = bdvHandle.getViewerPanel().getDisplay().getHeight() / 2.0;

        adaptedViewerTransform.translate( windowCentreInViewerInPixels );

        return adaptedViewerTransform;
    }

    public static double getViewerVoxelSpacing( BdvHandle bdv ) {
        final int windowWidth = bdv.getViewerPanel().getDisplay().getWidth();
        final int windowHeight = bdv.getViewerPanel().getDisplay().getHeight();

        final AffineTransform3D viewerTransform = new AffineTransform3D();
        bdv.getViewerPanel().state().getViewerTransform( viewerTransform );

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

    public static boolean isSourceIntersectingCurrentView( BdvHandle bdv, Source source, boolean is2D ) {
        if (source.getSource(0,0) == null) {
            // Overlays have no RAI -> discard them
            return false;
        }

        final Interval interval = getSourceGlobalBoundingInterval( source, bdv.getViewerPanel().state().getCurrentTimepoint() );

        final Interval viewerInterval =
                Intervals.smallestContainingInterval(
                        getViewerGlobalBoundingInterval( bdv ) );

        boolean intersects = false;
        if (is2D) {
            intersects = !Intervals.isEmpty(
                    intersect2D(interval, viewerInterval));
        } else {
            intersects = !Intervals.isEmpty(
                    Intervals.intersect( interval, viewerInterval ) );
        }
        return intersects;
    }

    public static FinalInterval intersect2D( final Interval intervalA, final Interval intervalB ) {
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

    public static FinalRealInterval getViewerGlobalBoundingInterval(BdvHandle bdvHandle) {
        AffineTransform3D viewerTransform = new AffineTransform3D();
        bdvHandle.getViewerPanel().state().getViewerTransform( viewerTransform );
        viewerTransform = viewerTransform.inverse();
        final long[] min = new long[ 3 ];
        final long[] max = new long[ 3 ];
        max[ 0 ] = bdvHandle.getViewerPanel().getWidth();
        max[ 1 ] = bdvHandle.getViewerPanel().getHeight();
        final FinalRealInterval realInterval
                = viewerTransform.estimateBounds( new FinalInterval( min, max ) );
        return realInterval;
    }

    public static Interval getSourceGlobalBoundingInterval( Source< ? > source, int timepoint ) {
        final AffineTransform3D sourceTransform = getSourceTransform( source, timepoint );
        final RandomAccessibleInterval< ? > rai = source.getSource(timepoint,0);
        final Interval interval =
                Intervals.smallestContainingInterval( sourceTransform.estimateBounds( rai ) );
        return interval;
    }

    public static AffineTransform3D getSourceTransform( Source< ? > source, int timepoint ) {
        return getSourceTransform(source, timepoint, 0);
    }

    /**
     * Returns the highest level where the sourceandconverter voxel spacings
     * are inferior or equals to the requested ones.
     *
     * @param source the source
     * @param voxelSpacings voxel spacings of the source
     * @return the optimal level for image visualization
     */
    public static int getLevel( Source< ? > source, double... voxelSpacings ) {
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

    public static AffineTransform3D getSourceTransform( Source source, int t, int level ) {
        AffineTransform3D sourceTransform = new AffineTransform3D();
        source.getSourceTransform( t, level, sourceTransform );
        return sourceTransform;
    }

    public static double[] getCalibration( Source source, int level ) {
        final AffineTransform3D sourceTransform = new AffineTransform3D();

        source.getSourceTransform( 0, level, sourceTransform );

        final double[] calibration = getScale( sourceTransform );

        return calibration;
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

    public static double[] getDisplayRange( ConverterSetup converterSetup ) {
        final double displayRangeMin = converterSetup.getDisplayRangeMin();
        final double displayRangeMax = converterSetup.getDisplayRangeMax();

        return new double[]{ displayRangeMin, displayRangeMax };
    }

}
