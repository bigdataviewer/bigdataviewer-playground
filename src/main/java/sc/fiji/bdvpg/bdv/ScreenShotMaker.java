package sc.fiji.bdvpg.bdv;

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
import net.imglib2.converter.Converter;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static sc.fiji.bdvpg.bdv.BdvUtils.*;

/**
 * ScreenShotMaker
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf, @tischi
 *         December 2019
 */
public class ScreenShotMaker {

    private BdvHandle bdvHandle;
    private double physicalPixelSpacingInXY = 1;
    private String physicalUnit = "Pixels";
    private boolean sourceInteractionWithViewerPlaneOnly2D = false; // TODO: maybe remove in the future
    ImagePlus screenShot = null;
    private CompositeImage rawImageData = null;

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
        createScreenshot(bdvHandle, physicalPixelSpacingInXY, physicalUnit, sourceInteractionWithViewerPlaneOnly2D);
    }

    public ImagePlus getRgbScreenShot() {
        process();
        return screenShot;
    }

    public CompositeImage getRawScreenShot()
    {
        process();
        return rawImageData;
    }

    private void createScreenshot(
            BdvHandle bdv,
            double pixelSpacing,
            String voxelUnit,
            boolean checkSourceIntersectionWithViewerPlaneOnlyIn2D )
    {
        final AffineTransform3D viewerTransform = new AffineTransform3D();
        bdv.getViewerPanel().getState().getViewerTransform( viewerTransform );

        final double viewerVoxelSpacing = getViewerVoxelSpacing( bdv );
        double dxy = pixelSpacing / viewerVoxelSpacing;

        final int w = getBdvWindowWidth( bdv );
        final int h = getBdvWindowHeight( bdv );

        final long captureWidth = ( long ) Math.ceil( w / dxy );
        final long captureHeight = ( long ) Math.ceil( h / dxy );

        final ArrayList< RandomAccessibleInterval< UnsignedShortType > > captures = new ArrayList<>();
        final ArrayList< ARGBType > colors = new ArrayList<>();
        final ArrayList< double[] > displayRanges = new ArrayList<>();
        final List< Integer > sourceIndices = getVisibleSourceIndices( bdv );

        final int t = bdv.getViewerPanel().getState().getCurrentTimepoint();

        final RandomAccessibleInterval< ARGBType > argbCapture
                = ArrayImgs.argbs( captureWidth, captureHeight );

        for ( int sourceIndex : sourceIndices )
        {
            if ( ! isSourceIntersectingCurrentView( bdv, sourceIndex, checkSourceIntersectionWithViewerPlaneOnlyIn2D ) ) continue;

            final RandomAccessibleInterval< UnsignedShortType > realCapture
                    = ArrayImgs.unsignedShorts( captureWidth, captureHeight );

            Source< ? > source = getSource( bdv, sourceIndex );
            final Converter converter = getConverter( bdv, sourceIndex );

            final int level = getLevel( source, pixelSpacing );
            final AffineTransform3D sourceTransform =
                    BdvUtils.getSourceTransform( source, t, level );

            AffineTransform3D viewerToSourceTransform = new AffineTransform3D();
            viewerToSourceTransform.preConcatenate( viewerTransform.inverse() );
            viewerToSourceTransform.preConcatenate( sourceTransform.inverse() );

            // TODO: Once we have a logic for segmentation images,
            // make this choice depend on this
            boolean interpolate = true;

            Grids.collectAllContainedIntervals(
                    Intervals.dimensionsAsLongArray( argbCapture ),
                    new int[]{100, 100}).parallelStream().forEach( interval ->
            {
                RealRandomAccess< ? extends RealType< ? > > realTypeAccess =
                        getRealTypeRealRandomAccess( t, source, level, interpolate );
                RealRandomAccess< ? > access =
                        getRealRandomAccess( t, source, level, interpolate );

                // to collect raw data
                final IntervalView< UnsignedShortType > realCrop = Views.interval( realCapture, interval );
                final Cursor< UnsignedShortType > realCaptureCursor = Views.iterable( realCrop ).localizingCursor();
                final RandomAccess< UnsignedShortType > realCaptureAccess = realCrop.randomAccess();

                // to collect coloured data
                final IntervalView< ARGBType > argbCrop = Views.interval( argbCapture, interval );
                final RandomAccess< ARGBType > argbCaptureAccess = argbCrop.randomAccess();

                final double[] canvasPosition = new double[ 3 ];
                final double[] sourceRealPosition = new double[ 3 ];

                final ARGBType argbType = new ARGBType();

                while ( realCaptureCursor.hasNext() )
                {
                    realCaptureCursor.fwd();
                    realCaptureCursor.localize( canvasPosition );
                    realCaptureAccess.setPosition( realCaptureCursor );
                    argbCaptureAccess.setPosition( realCaptureCursor );

                    // canvasPosition is the position on the canvas, in calibrated units
                    // dxy is the step size that is needed to get the desired resolution in the
                    // output image
                    canvasPosition[ 0 ] *= dxy;
                    canvasPosition[ 1 ] *= dxy;

                    viewerToSourceTransform.apply( canvasPosition, sourceRealPosition );

                    setRealTypeVoxel( realTypeAccess, realCaptureAccess, sourceRealPosition );
                    setArgbTypeVoxel( converter, access, argbCaptureAccess, sourceRealPosition, argbType );
                }
            });

            captures.add( realCapture );
            // colors.add( getSourceColor( bdv, sourceIndex ) ); Not used, show GrayScale
            displayRanges.add( BdvUtils.getDisplayRange( bdv, sourceIndex) );
        }

        final double[] voxelSpacing = new double[ 3 ];
        for ( int d = 0; d < 2; d++ )
            voxelSpacing[ d ] = pixelSpacing;

        voxelSpacing[ 2 ] = viewerVoxelSpacing; // TODO: What to put here?

        if ( captures.size() > 0 )
        {
            screenShot = createRgbImage(
                    voxelUnit, argbCapture, voxelSpacing );
            rawImageData  = createCompositeImage(
                    voxelSpacing, voxelUnit, captures, colors, displayRanges );
        }
    }

    private void setArgbTypeVoxel( Converter converter, RealRandomAccess< ? > access, RandomAccess< ARGBType > argbCaptureAccess, double[] sourceRealPosition, ARGBType argbType )
    {
        access.setPosition( sourceRealPosition );
        final Object pixel = access.get();
        if ( pixel instanceof ARGBType )
            argbType.set( ( ARGBType ) pixel );
        else
            converter.convert( pixel, argbType );

        final int sourceARGBIndex = argbType.get();
        final int captureARGBIndex = argbCaptureAccess.get().get();
        int a = ARGBType.alpha( sourceARGBIndex ) + ARGBType.alpha( captureARGBIndex );
        int r = ARGBType.red( sourceARGBIndex ) + ARGBType.red( captureARGBIndex );
        int g = ARGBType.green( sourceARGBIndex )+ ARGBType.green( captureARGBIndex );
        int b = ARGBType.blue( sourceARGBIndex )+ ARGBType.blue( captureARGBIndex );

        if ( a > 255 )
            a = 255;
        if ( r > 255 )
            r = 255;
        if ( g > 255 )
            g = 255;
        if ( b > 255 )
            b = 255;

        argbCaptureAccess.get().set( ARGBType.rgba( r, g, b, a ) );
    }

    private void setRealTypeVoxel( RealRandomAccess< ? extends RealType< ? > > realTypeAccess, RandomAccess< UnsignedShortType > realCaptureAccess, double[] sourceRealPosition )
    {
        realTypeAccess.setPosition( sourceRealPosition );
        final RealType< ? > realType = realTypeAccess.get();
        realCaptureAccess.get().setReal( realType.getRealDouble() );
    }

    private RealRandomAccess< ? > getRealRandomAccess( int t, Source< ? > source, int level, boolean interpolate )
    {
        RealRandomAccess< ? > access = null;
        if ( interpolate )
            access = source.getInterpolatedSource( t, level, Interpolation.NLINEAR ).realRandomAccess();
        else
            access = source.getInterpolatedSource( t, level, Interpolation.NEARESTNEIGHBOR ).realRandomAccess();
        return access;
    }

    private static ImagePlus createRgbImage( String voxelUnit, RandomAccessibleInterval< ARGBType > argbCapture, double[] voxelSpacing )
    {
        final ImagePlus rgbImage = ImageJFunctions.wrap( argbCapture, "View Capture RGB" );

        IJ.run( rgbImage,
                "Properties...",
                "channels=" + 1
                        +" slices=1 frames=1 unit=" + voxelUnit
                        +" pixel_width=" + voxelSpacing[ 0 ]
                        +" pixel_height=" + voxelSpacing[ 1 ]
                        +" voxel_depth=" + voxelSpacing[ 2 ] );
        return rgbImage;
    }

    public static CompositeImage createCompositeImage(
            double[] voxelSpacing,
            String voxelUnit,
            ArrayList< RandomAccessibleInterval< UnsignedShortType > > rais,
            ArrayList< ARGBType > colors,
            ArrayList< double[] > displayRanges )
    {
        final RandomAccessibleInterval< UnsignedShortType > stack = Views.stack( rais );

        final ImagePlus imp = ImageJFunctions.wrap( stack, "View Capture Raw" );

        // duplicate: otherwise it is virtual and cannot be modified
        final ImagePlus dup = new Duplicator().run( imp );

        IJ.run( dup,
                "Properties...",
                "channels="+rais.size()
                        +" slices=1 frames=1 unit=" + voxelUnit
                        +" pixel_width=" + voxelSpacing[ 0 ]
                        +" pixel_height=" + voxelSpacing[ 1 ]
                        +" voxel_depth=" + voxelSpacing[ 2 ] );

        final CompositeImage compositeImage = new CompositeImage( dup );

        for ( int channel = 1; channel <= compositeImage.getNChannels(); ++channel )
        {
            // TODO: Maybe put different LUTs there?
            final LUT lut = compositeImage.createLutFromColor( Color.WHITE );
            compositeImage.setC( channel );
            compositeImage.setChannelLut( lut );
            final double[] range = displayRanges.get( channel - 1 );
            compositeImage.setDisplayRange( range[ 0 ], range[ 1 ] );
        }

        compositeImage.setTitle( "View Capture Raw" );
        return compositeImage;
    }

    public static RealRandomAccess< ? extends RealType< ? > >
    getRealTypeRealRandomAccess( int t, Source< ? > source, int level, boolean interpolate )
    {
        if ( interpolate )
            return (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource(t, level, Interpolation.NLINEAR).realRandomAccess();
        else
            return (RealRandomAccess<? extends RealType<?>>) source.getInterpolatedSource(t, level, Interpolation.NEARESTNEIGHBOR).realRandomAccess();
    }
}
