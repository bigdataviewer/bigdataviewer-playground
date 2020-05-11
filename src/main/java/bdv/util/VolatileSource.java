package bdv.util;

import bdv.util.volatiles.SharedQueue;
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
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.*;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import java.util.concurrent.ConcurrentHashMap;

public class VolatileSource<T extends NumericType<T>, V extends Volatile< T > & NumericType< V >> implements Source<V> {

    final Source<T> originSource;

    protected final DefaultInterpolators< V > interpolators = new DefaultInterpolators<>();

    final SharedQueue queue;

    ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, RandomAccessibleInterval>> cachedRAIs = new ConcurrentHashMap<>();

    public VolatileSource(final Source resampledSource) {
        this.originSource = resampledSource;
        queue = new SharedQueue(2);
    }

    public VolatileSource(final Source resampledSource, final SharedQueue queue) {
        this.originSource = resampledSource;
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

    static public Volatile getVolatileOf(NumericType t) {
        if (t instanceof UnsignedShortType) return new VolatileUnsignedShortType();

        if (t instanceof UnsignedIntType) return new VolatileUnsignedIntType();

        if (t instanceof UnsignedByteType) return new VolatileUnsignedByteType();

        if (t instanceof FloatType) return new VolatileFloatType();

        if (t instanceof ARGBType) return new VolatileARGBType();
        return null;
    }
}
