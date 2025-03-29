/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package bdv.util;

//import bdv.SpimSource;
import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.util.Intervals;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CroppedSource< T extends NumericType<T> & NativeType<T>> implements Source<T>
{
    private final Source<T> wrappedSource;
    private final String name;
    private final RealInterval crop;
    private final boolean cropMinToZero;
    protected final DefaultInterpolators< T > interpolators;
    private HashMap< Integer, Interval > levelToVoxelInterval;

    public CroppedSource( Source<T> source, String name, RealInterval crop, boolean cropMinToZero )
    {
        this.wrappedSource = source;
        this.name = name;
        this.crop = crop;
        this.cropMinToZero = cropMinToZero;
        this.interpolators = new DefaultInterpolators();

        initCropIntervals( source, crop );
    }


    private void initCropIntervals( Source< T > source, RealInterval crop )
    {
        final AffineTransform3D transform3D = new AffineTransform3D();
        levelToVoxelInterval = new HashMap<>();
        for ( int l = 0; l < source.getNumMipmapLevels(); l++ )
        {
            source.getSourceTransform( 0, l, transform3D );
            final Interval voxelInterval = Intervals.smallestContainingInterval( transform3D.inverse().estimateBounds( crop ) );
            levelToVoxelInterval.put( l, voxelInterval );
        }
    }

    public Source<?> getWrappedSource() {
        return wrappedSource;
    }

    @Override
    public boolean isPresent(int t) {
        return wrappedSource.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval<T> getSource(int t, int level)
    {
        final IntervalView< T > intervalView = Views.interval( wrappedSource.getSource( t, level ), levelToVoxelInterval.get( level ) );

        if ( cropMinToZero )
            return Views.zeroMin( intervalView );
        else
            return intervalView;
    }

    @Override
    public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation method)
    {
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval< T >>
                zeroExtendedCrop = Views.extendZero( getSource( t, level ) );
        RealRandomAccessible< T > realRandomAccessible = Views.interpolate( zeroExtendedCrop, interpolators.get(method) );
        return realRandomAccessible;
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform)
    {
        wrappedSource.getSourceTransform( t, level, transform );
    }

    @Override
    public T getType() {
        return wrappedSource.getType();
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return wrappedSource.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return wrappedSource.getNumMipmapLevels();
    }

}
