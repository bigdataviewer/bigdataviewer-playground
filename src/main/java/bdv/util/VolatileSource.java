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
package bdv.util;

import bdv.cache.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.*;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link VolatileSource} simply wraps and cache volatileviews of a {@link Source}
 * which can be made Volatile thanks to {@link VolatileViews#wrapAsVolatile}
 * That's not always possible!
 *
 * A {@link SharedQueue} can be passed as an argument in the constructor to control more finely
 * the volatile fetching jobs
 *
 * Use case : see {@link ResampledSource}
 *
 * @param <T> concrete pixel {@link net.imglib2.type.Type} linked to:
 * @param <V> {@link Volatile} type
 */

public class VolatileSource<T extends NumericType<T>, V extends Volatile< T > & NumericType< V >> implements Source<V> {

    final Source<T> originSource;

    protected final DefaultInterpolators< V > interpolators = new DefaultInterpolators<>();

    final SharedQueue queue;

    final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, RandomAccessibleInterval<V>>> cachedRAIs = new ConcurrentHashMap<>();

    public VolatileSource(final Source<T> source) {
        this.originSource = source;
        queue = new SharedQueue(2);
    }

    public VolatileSource(final Source<T> originSource, final SharedQueue queue) {
        this.originSource = originSource;
        this.queue = queue;
    }

    @Override
    public boolean isPresent(int t) {
        return originSource.isPresent(t);
    }

    public RandomAccessibleInterval<V> buildSource(int t, int level) {
        return VolatileViews.wrapAsVolatile(originSource.getSource(t,level), queue);
    }

    @Override
    public RandomAccessibleInterval<V> getSource(int t, int level) {
        if (!cachedRAIs.containsKey(t)) {
            cachedRAIs.put(t, new ConcurrentHashMap<>());
        }

        if (!cachedRAIs.get(t).containsKey(level)) {
            RandomAccessibleInterval<V> nonCached = buildSource(t,level);

            cachedRAIs.get(t).put(level,nonCached);
        }

        return cachedRAIs.get(t).get(level);
    }

    @Override
    public RealRandomAccessible<V> getInterpolatedSource(int t, int level, Interpolation method) {
        final V zero = getType();
        zero.setZero();
        ExtendedRandomAccessibleInterval<V, RandomAccessibleInterval< V >>
                eView = Views.extendZero(getSource( t, level ));
        //noinspection UnnecessaryLocalVariable
        RealRandomAccessible< V > realRandomAccessible = Views.interpolate( eView, interpolators.get(method) );
        return realRandomAccessible;
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        originSource.getSourceTransform(t,level,transform);
    }

    @Override
    public V getType() {
        return (V) getVolatileOf(originSource.getType());
    }

    @Override
    public String getName() {
        return originSource.getName();
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return originSource.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return originSource.getNumMipmapLevels();
    }

    /**
     * TODO : this helper class should be already somewhere else!
     * @param t a NumericType instance
     * @return the volatile equivalent class of this NumericType instance
     */

    static public Volatile<? extends NumericType> getVolatileOf(NumericType<?> t) {
        if (t instanceof UnsignedShortType) return new VolatileUnsignedShortType();

        if (t instanceof UnsignedIntType) return new VolatileUnsignedIntType();

        if (t instanceof UnsignedByteType) return new VolatileUnsignedByteType();

        if (t instanceof FloatType) return new VolatileFloatType();

        if (t instanceof ARGBType) return new VolatileARGBType();
        return null;
    }
}
