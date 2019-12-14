package sc.fiji.bdv.screenshot;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import ij.process.LUT;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccess;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import sc.fiji.bdv.lut.Luts;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static sc.fiji.bdv.BDVUtils.*;

/**
 * ScreenShotMaker
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf, @tischi
 *         December 2019
 */
public class ScreenShotMaker {

    CompositeImage screenShot = null;
    private BdvHandle bdvHandle;
    private double physicalPixelSpacingInXY = 1;
    private String physicalUnit = "Pixels";
    private boolean sourceInteractionWithViewerPlaneOnly2D = true;


    public ScreenShotMaker(BdvHandle bdvHandle) {
        this.bdvHandle = bdvHandle;
    }

    public void setPhysicalPixelSpacingInXY(double spacing, String unit) {
        screenShot = null;
        this.physicalPixelSpacingInXY = spacing;
        this.physicalUnit = unit;
    }

    public void setSourceInteractionWithViewerPlaneOnly2D(boolean sourceInteractionWithViewerPlaneOnly2D) {
        screenShot = null;
        this.sourceInteractionWithViewerPlaneOnly2D = sourceInteractionWithViewerPlaneOnly2D;
    }

    private void process() {
        if (screenShot != null) {
            return;
        }
        screenShot = captureView(bdvHandle, physicalPixelSpacingInXY, physicalUnit, sourceInteractionWithViewerPlaneOnly2D);
    }

    public ImagePlus getScreenshot() {
        process();
        return screenShot;
    }


    private static < R extends RealType< R >> CompositeImage captureView(
            BdvHandle bdv,
            double pixelSpacing,
            String voxelUnits,
            boolean checkSourceIntersectionWithViewerPlaneOnlyIn2D )
    {
        final AffineTransform3D viewerTransform = new AffineTransform3D();
        bdv.getViewerPanel().getState().getViewerTransform( viewerTransform );

        final double[] viewerVoxelSpacing = getViewerVoxelSpacing( bdv );

        double dxy = pixelSpacing / viewerVoxelSpacing[ 0 ] ;

        final int w = getBdvWindowWidth( bdv );
        final int h = getBdvWindowHeight( bdv );

        final long captureWidth = ( long ) Math.ceil( w / dxy );
        final long captureHeight = ( long ) Math.ceil( h / dxy );

        // TODO: Maybe capture Segmentations (or everything?) as ARGBType images?
        // Like this, the label masks would look as nice as in the Bdv.
        final ArrayList<RandomAccessibleInterval<UnsignedShortType>> captures = new ArrayList<>();
        final ArrayList<ARGBType> colors = new ArrayList<>();
        final ArrayList< Boolean > isSegmentations = new ArrayList<>();
        final ArrayList< double[] > displayRanges = new ArrayList<>();

        final List< Integer > sourceIndices = getVisibleSourceIndices( bdv );

        final int t = bdv.getViewerPanel().getState().getCurrentTimepoint();

        for ( int sourceIndex : sourceIndices )
        {
            if ( ! isSourceIntersectingCurrentView( bdv, sourceIndex, checkSourceIntersectionWithViewerPlaneOnlyIn2D ) ) continue;

            final RandomAccessibleInterval< UnsignedShortType > capture
                    = ArrayImgs.unsignedShorts( captureWidth, captureHeight );

            Source< ? > source = getSource( bdv, sourceIndex );

            final int level = getLevel( source, pixelSpacing );
            final AffineTransform3D sourceTransform =
                    getSourceTransform( source, t, level );

            AffineTransform3D viewerToSourceTransform = new AffineTransform3D();

            viewerToSourceTransform.preConcatenate( viewerTransform.inverse() );
            viewerToSourceTransform.preConcatenate( sourceTransform.inverse() );

            isSegmentations.add( false );

            Grids.collectAllContainedIntervals(
                    Intervals.dimensionsAsLongArray( capture ),
                    new int[]{100, 100}).parallelStream().forEach( interval ->
            {
                RealRandomAccess< ? extends RealType< ? > > sourceAccess =
                        getInterpolatedRealRandomAccess( t, source, level, true );

                final IntervalView< UnsignedShortType > crop = Views.interval( capture, interval );
                final Cursor< UnsignedShortType > captureCursor = Views.iterable( crop ).localizingCursor();
                final RandomAccess< UnsignedShortType > captureAccess = crop.randomAccess();

                final double[] canvasPosition = new double[ 3 ];
                final double[] sourceRealPosition = new double[ 3 ];

                while ( captureCursor.hasNext() )
                {
                    captureCursor.fwd();
                    captureCursor.localize( canvasPosition );
                    captureAccess.setPosition( captureCursor );
                    canvasPosition[ 0 ] *= dxy;
                    canvasPosition[ 1 ] *= dxy;
                    viewerToSourceTransform.apply( canvasPosition, sourceRealPosition );
                    sourceAccess.setPosition( sourceRealPosition );
                    captureAccess.get().setReal( sourceAccess.get().getRealDouble() );
                }
            });

            captures.add( capture );
            colors.add( getSourceColor( bdv, sourceIndex ) );
            displayRanges.add( getDisplayRange( bdv, sourceIndex) );
        }

        final double[] captureVoxelSpacing = new double[ 3 ];
        for ( int d = 0; d < 2; d++ )
            captureVoxelSpacing[ d ] = pixelSpacing;

        captureVoxelSpacing[ 2 ] = viewerVoxelSpacing[ 2 ]; // TODO: makes sense?

        if ( captures.size() > 0 )
            return asCompositeImage( captureVoxelSpacing, voxelUnits, captures, colors, displayRanges, isSegmentations );
        else
            return null;


    }

    public static RealRandomAccess< ? extends RealType< ? > >
    getInterpolatedRealRandomAccess( int t, Source< ? > source, int level, boolean interpolate )
    {
        if ( interpolate )
            return (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource(t, level, Interpolation.NLINEAR).realRandomAccess();
        else
            return (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource(t, level, Interpolation.NEARESTNEIGHBOR).realRandomAccess();

            /*
        RealRandomAccess< ? extends RealType< ? > > sourceAccess;
        if ( interpolate )
            sourceAccess = getInterpolatedRealTypeNonVolatileRealRandomAccess( sources, t, level, Interpolation.NLINEAR );
        else
            sourceAccess = getInterpolatedRealTypeNonVolatileRealRandomAccess( sources, t, level, Interpolation.NEARESTNEIGHBOR );

        return sourceAccess;*/
    }

    public static boolean isInterpolate( Source< ? > source )
    {
        boolean interpolate = true;
        return interpolate;
    }


    public static CompositeImage asCompositeImage(
            double[] voxelSpacing,
            String voxelUnit,
            ArrayList< RandomAccessibleInterval< UnsignedShortType > > rais,
            ArrayList< ARGBType > colors,
            ArrayList< double[] > displayRanges,
            ArrayList< Boolean > isSegmentations )
    {
        final RandomAccessibleInterval< UnsignedShortType > stack = Views.stack( rais );

        final ImagePlus imp = ImageJFunctions.wrap( stack, "Bdv View Capture" );

        // duplicate: otherwise it is virtual and cannot be modified
        final ImagePlus dup = new Duplicator().run( imp );

        IJ.run( dup,
                "Properties...",
                "channels="+rais.size()
                        +" slices=1 frames=1 physicalUnit="+voxelUnit
                        +" pixel_width=" + voxelSpacing[ 0 ]
                        +" pixel_height=" + voxelSpacing[ 1 ]
                        +" voxel_depth=" + voxelSpacing[ 2 ] );

        final CompositeImage compositeImage = new CompositeImage( dup );

        for ( int channel = 1; channel <= compositeImage.getNChannels(); ++channel )
        {
            final Boolean isSegmentation = isSegmentations.get( channel - 1 );

            if ( isSegmentation )
            {
                final LUT lut = Luts.glasbeyLutIJ();
                compositeImage.setC( channel );
                compositeImage.setChannelLut( lut );
            }
            else
            {
                Color color = new Color( colors.get( channel - 1 ).get() );
                final LUT lut = compositeImage.createLutFromColor( color );
                compositeImage.setC( channel );
                compositeImage.setChannelLut( lut );
                final double[] range = displayRanges.get( channel - 1 );
                compositeImage.setDisplayRange( range[ 0 ], range[ 1 ] );
            }
        }

        compositeImage.setTitle( "Bdv View Capture" );
        IJ.run( compositeImage, "Make Composite", "" );

        return compositeImage;
    }


}
