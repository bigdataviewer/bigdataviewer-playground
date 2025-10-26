package bdv.util;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutOfBoundsColorChangedSource<T extends NumericType<T>> implements Source<T> {

    protected static final Logger logger = LoggerFactory.getLogger(
            OutOfBoundsColorChangedSource.class);

    transient protected final DefaultInterpolators<T> interpolators =
            new DefaultInterpolators<>();

    /**
     * Origin source of type {@link T}
     */
    final Source<T> origin;

    final T outOfBoundsValue;

    public OutOfBoundsColorChangedSource(Source<T> source, T outOfBoundsValue)
    {
        this.origin = source;
        this.outOfBoundsValue = outOfBoundsValue;
    }

    @Override
    public boolean isPresent(int t) {
        return origin.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval<T> getSource(int t, int level) {
        return origin.getSource(t, level);
    }

    @Override
    public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation method) {
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval<T>> eView =
                Views.extendValue(getSource(t, level), outOfBoundsValue);
        return Views.interpolate(eView, interpolators.get(method));
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        origin.getSourceTransform(t, level, transform);
    }

    @Override
    public T getType() {
        return origin.getType();
    }

    @Override
    public String getName() {
        return origin.getName();
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return origin.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return origin.getNumMipmapLevels();
    }
}
