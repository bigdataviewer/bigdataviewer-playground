package bdv.util;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * Resamples on the fly a source based on another source
 * The origin source is accessed through its RealRandomAccessible representation :
 * - It can be accessed at any 3d point in space, with real valued coordinates : it's a scalar field
 *
 * The srcResamplingModel is there to define a portion of space and how it is sampled :
 * - through its RandomAccessibleInterval bounds
 * - and the Source affine transform
 *
 * The returned resampled is a source which is:
 * - the sampling of the scalar field
 * - at the points which are defined by the model source
 *
 * Options:
 * - reuseMipmaps allows to reuse mipmaps of both the origin and the model source in the resampling
 * - interpolate specifies whether the origin source should be interpolated of not in the resampling process
 *
 * Note:
 * - To be present at a certain timepoint, both the origin and the model source need to exist
 * - There is no duplication of data
 *
 * @author Nicolas Chiaruttini, BIOP EPFL
 * @param <T>
 */

public class ResampledSource< T extends NumericType<T> & NativeType<T>> implements Source<T> {

    /**
     * Origin source
     */
    Source<T> origin;

    /**
     * Model source
     */
    Source<?> resamplingModel;

    Interpolation originInterpolation;

    protected final DefaultInterpolators< T > interpolators = new DefaultInterpolators<>();

    boolean reuseMipMaps;

    public ResampledSource(Source<T> source, Source<T> resamplingModel, boolean reuseMipMaps, boolean originInterpolation) {
        this.origin=source;
        this.resamplingModel=resamplingModel;
        this.reuseMipMaps=reuseMipMaps;
        if (originInterpolation) {
            this.originInterpolation = Interpolation.NLINEAR;
        } else {
            this.originInterpolation = Interpolation.NEARESTNEIGHBOR;
        }
    }

    public Source getOriginalSource() {
        return origin;
    }

    public Source getModelResamplerSource() {
        return resamplingModel;
    }

    @Override
    public boolean isPresent(int t) {
        return origin.isPresent(t)&&resamplingModel.isPresent(t);
    }

    @Override
    public RandomAccessibleInterval<T> getSource(int t, int level) {
        // Get current model source transformation
        AffineTransform3D at = new AffineTransform3D();
        resamplingModel.getSourceTransform(t,reuseMipMaps?level:0,at);

        // Get bounds of model source RAI
        // TODO check if -1 is necessary
        long sx = resamplingModel.getSource(t,reuseMipMaps?level:0).dimension(0)-1;
        long sy = resamplingModel.getSource(t,reuseMipMaps?level:0).dimension(1)-1;
        long sz = resamplingModel.getSource(t,reuseMipMaps?level:0).dimension(2)-1;


        // Get scalar field of origin source
        final RealRandomAccessible<T> ipimg = origin.getInterpolatedSource(t, reuseMipMaps?level:0, originInterpolation);

        // Gets randomAccessible... ( with appropriate transform )
        at = at.inverse();
        AffineTransform3D atOrigin = new AffineTransform3D();
        origin.getSourceTransform(t, reuseMipMaps?level:0, atOrigin);
        at.concatenate(atOrigin);
        RandomAccessible<T> ra = RealViews.affine(ipimg, at); // Gets the view

        // ... interval
        RandomAccessibleInterval<T> view =
                Views.interval(ra, new long[]{0, 0, 0}, new long[]{sx, sy, sz}); //Sets the interval

        return view;
    }

    @Override
    public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation method) {
        final T zero = getType();
        zero.setZero();
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval< T >>
                eView = Views.extendZero(getSource( t, level ));
        RealRandomAccessible< T > realRandomAccessible = Views.interpolate( eView, interpolators.get(method) );
        return realRandomAccessible;
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        resamplingModel.getSourceTransform(t,reuseMipMaps?level:0,transform);
    }

    @Override
    public T getType() {
        return origin.getType();
    }

    @Override
    public String getName() {
        return origin.getName()+"_ResampledLike_"+resamplingModel.getName();
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return resamplingModel.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return reuseMipMaps?origin.getNumMipmapLevels():1;
    }
}
