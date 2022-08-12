/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.sourceandconverter.importer;

import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.*;
import net.imglib2.algorithm.util.Grids;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.interpolation.neighborsearch.NearestNeighborSearchInterpolatorFactory;
import net.imglib2.neighborsearch.NearestNeighborSearch;
import net.imglib2.neighborsearch.NearestNeighborSearchOnKDTree;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.Random;
import java.util.function.Supplier;

public class VoronoiSourceGetter implements Runnable, Supplier<SourceAndConverter<?>> {

    // Size of the image in pixels
    final long[] imgSize;
    // Number of random points that will define voronoi cells
    final int numPts;
    // Flags if the image should be computed completely
    final boolean copyImg;

    public VoronoiSourceGetter(final long[] imgSize, int numPts, boolean copyImg) {
        this.imgSize = imgSize;
        this.numPts = numPts;
        this.copyImg = copyImg;
    }

    public void run() {
        // Useless
    }

    @Override
    public SourceAndConverter<FloatType> get() {
        RandomAccessibleInterval<FloatType> voronoi = getVoronoiTestLabelImage(imgSize, numPts, copyImg);
        Source<FloatType> s = new RandomAccessibleIntervalSource<>( voronoi, new FloatType(), new AffineTransform3D(), "Voronoi_"+numPts+" Pts_["+imgSize[0]+","+imgSize[1]+","+imgSize[2]+"]" );
        return SourceAndConverterHelper.createSourceAndConverter(s);
    }

    public static RandomAccessibleInterval<FloatType> getVoronoiTestLabelImage(final long[] imgTestSize, int numPts, boolean copyImg) {

        // the interval in which to create random points
        FinalInterval interval = new FinalInterval( imgTestSize );

        // create an IterableRealInterval
        IterableRealInterval< FloatType > realInterval = createRandomPoints( interval, numPts );

        // using nearest neighbor search we will be able to return a value an any position in space
        NearestNeighborSearch< FloatType > search =
                new NearestNeighborSearchOnKDTree<>(
                        new KDTree<>( realInterval ) );

        // make it into RealRandomAccessible using nearest neighbor search
        RealRandomAccessible< FloatType > realRandomAccessible =
                Views.interpolate( search, new NearestNeighborSearchInterpolatorFactory<>() );

        // convert it into a RandomAccessible which can be displayed
        RandomAccessible< FloatType > randomAccessible = Views.raster( realRandomAccessible );

        // set the initial interval as area to view
        RandomAccessibleInterval< FloatType > labelImage = Views.interval( randomAccessible, interval );

        if (copyImg) {
            final ArrayImg<FloatType,?> labelImageCopy = new ArrayImgFactory( Util.getTypeFromInterval( labelImage ) ).create( labelImage );

            // Image copied to avoid computing it on the fly
            // https://github.com/imglib/imglib2-algorithm/blob/47cd6ed5c97cca4b316c92d4d3260086a335544d/src/main/java/net/imglib2/algorithm/util/Grids.java#L221 used for parallel copy

            Grids.collectAllContainedIntervals(imgTestSize, new int[]{64, 64, 64})
                    .forEach(blockinterval -> copy(labelImage, Views.interval(labelImageCopy, blockinterval)));

            // Alternative non parallel copy
            //LoopBuilder.setImages(labelImage, labelImageCopy).forEachPixel(Type::set);
            return labelImageCopy;

        } else {

            return labelImage;
        }
    }

    /**
     * Copy from a {@link SourceAndConverter} that is just RandomAccessible to an IterableInterval. Latter one defines
     * size and location of the copy operation. It will query the same pixel locations of the
     * IterableInterval in the RandomAccessible. It is up to the developer to ensure that these
     * coordinates match.
     *
     * Note that both, input and output could be Views, Img or anything that implements
     * those interfaces.
     *
     * @param source - a RandomAccess as sourceandconverter that can be infinite
     * @param target - an IterableInterval as target
     * @param <T> - type the accessed values
     */
    public static < T extends Type< T >> void copy(final RandomAccessible< T > source,
                                                   final IterableInterval< T > target )
    {
        // create a cursor that automatically localizes itself on every move
        Cursor< T > targetCursor = target.localizingCursor();
        RandomAccess< T > sourceRandomAccess = source.randomAccess();

        // iterate over the input cursor
        while ( targetCursor.hasNext())
        {
            // move input cursor forward
            targetCursor.fwd();

            // set the output cursor to the position of the input cursor
            sourceRandomAccess.setPosition( targetCursor );

            // set the value of this pixel of the output image, every Type supports T.set( T type )
            targetCursor.get().set( sourceRandomAccess.get() );
        }

    }

    /**
     * Create a number of n-dimensional random points in a certain interval
     * having a random intensity 0...1
     *
     * @param interval - the interval in which points are created
     * @param numPoints - the amount of points
     *
     * @return a RealPointSampleList (which is an IterableRealInterval)
     */
    public static RealPointSampleList< FloatType > createRandomPoints(
            RealInterval interval, int numPoints )
    {
        // the number of dimensions
        int numDimensions = interval.numDimensions();

        // a random number generator
        Random rnd = new Random( 2001);//System.currentTimeMillis() );

        // a list of Samples with coordinates
        RealPointSampleList< FloatType > elements =
                new RealPointSampleList<>( numDimensions );

        for ( int i = 0; i < numPoints; ++i )
        {
            RealPoint point = new RealPoint( numDimensions );

            for ( int d = 0; d < numDimensions; ++d )
                point.setPosition( rnd.nextDouble() *
                        ( interval.realMax( d ) - interval.realMin( d ) ) + interval.realMin( d ), d );

            // add a new element with a random intensity in the range 0...1
            elements.add( point, new FloatType( rnd.nextFloat()*255 ) );
        }

        return elements;
    }


}
