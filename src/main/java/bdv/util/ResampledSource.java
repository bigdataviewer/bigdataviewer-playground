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

import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.processors.SpimDataPostprocessor;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * A {@link ResampledSource} is a {@link Source} which is computed on the fly
 * by sampling an {@link ResampledSource#origin} source at space and time coordinates
 * defined by a model {@link ResampledSource#resamplingModel} source
 *
 * The returned resampled source is a source which is:
 * - the sampling of the origin source field {@link ResampledSource#origin}
 * at the points which are defined by the model source {@link ResampledSource#resamplingModel} source
 *
 * Note:
 * - To be present at a certain timepoint, both the origin and the model source need to exist
 * - There is no duplication of data, unless {@link ResampledSource#cache} is true
 * - proper reuse of mipmaps for model and origin work in certain conditions: see constructor documentation
 *
 * @param <T> Type of the output source, identical to the origin source
 *
 * @author Nicolas Chiaruttini, BIOP EPFL, 2020
 */

public class ResampledSource< T extends NumericType<T> & NativeType<T>> implements Source<T> {

    protected static Logger logger = LoggerFactory.getLogger(ResampledSource.class);

    /**
     * Hashmap to cache RAIs (mipmaps and timepoints), used only if {@link ResampledSource#cache} is true
     */
    transient ConcurrentHashMap<Integer, ConcurrentHashMap<Integer,RandomAccessibleInterval<T>>> cachedRAIs
            = new ConcurrentHashMap<>();

    /**
     * Origin source of type {@link T}
     */
    Source<T> origin;

    /**
     * Model source, no need to be of type {@link T}
     */
    Source<?> resamplingModel;

    protected final DefaultInterpolators< T > interpolators = new DefaultInterpolators<>();

    protected Interpolation originInterpolation;

    boolean reuseMipMaps;

    boolean cache;

    /**
     * The origin source is accessed through its RealRandomAccessible representation :
     * - It can be accessed at any 3d point in space, with real valued coordinates : it's a field of {@link T} objects
     *
     * The model source defines a portion of space and how it is sampled :
     *  - through its RandomAccessibleInterval bounds
     *  - and the Source affine transform
     *  - and mipmaps, if reuseMipMaps is true
     *
     * @param source origin source
     *
     * @param resamplingModel model source used for resampling the origin source
     *
     * @param reuseMipMaps allows to reuse mipmaps of both the origin and the model source in the resampling
     *  mipmap reuse tries to be clever by matching the voxel size between the model source and the origin source
     *  so for instance the model source mipmap level 0 will resample the origin mipmap level 2, if the voxel size
     *  of the origin is much smaller then the model (and provided that the origin is also a multiresolution source)
     *  the way the matching is performed is specified in {@link SourceAndConverterHelper#bestLevel(Source, int, double)}.
     *  For more details and limitation, please read the documentation in the linked method above
     *
     * @param cache specifies whether the result of the resampling should be cached.
     *  This allows for a fast access of resampled source after the first computation - but the synchronization with
     *  the origin and model source is lost.
     *  TODO : check how the cache can be accessed / reset
     *
     * @param originInterpolation specifies whether the origin source should be interpolated of not in the resampling process
     *
     */
    public ResampledSource(Source<T> source, Source<T> resamplingModel, boolean reuseMipMaps, boolean cache, boolean originInterpolation) {
        this.origin=source;
        this.resamplingModel=resamplingModel;
        this.reuseMipMaps=reuseMipMaps;
        this.cache = cache;
        if (originInterpolation) {
            this.originInterpolation = Interpolation.NLINEAR;
        } else {
            this.originInterpolation = Interpolation.NEARESTNEIGHBOR;
        }
        computeMipMapsCorrespondance();
    }

    Map<Integer, Integer> mipmapModelToOrigin = new HashMap<>();

    List<Double> originVoxSize;

    private int bestMatch(double voxSize) {
        if (originVoxSize==null) {
            computeOriginSize();
        }
        int level = 0;
        while((originVoxSize.get(level)<voxSize)&&(level<originVoxSize.size()-1)) {
            level=level+1;
        }
        return level;
    }

    private void computeOriginSize() {
        originVoxSize = new ArrayList<>();
        Source<?> rootOrigin = origin;

        while ((rootOrigin instanceof WarpedSource)||(rootOrigin instanceof TransformedSource)) {
            if (rootOrigin instanceof WarpedSource) {
                rootOrigin = ((WarpedSource<?>) rootOrigin).getWrappedSource();
            } else { // rootOrigin instanceof TransformedSource
                rootOrigin = ((TransformedSource<?>) rootOrigin).getWrappedSource();
            }
        }

        for (int l=0;l<rootOrigin.getNumMipmapLevels();l++) {
            AffineTransform3D at3d = new AffineTransform3D();
            rootOrigin.getSourceTransform(0,l,at3d);
            double mid = SourceAndConverterHelper.getCharacteristicVoxelSize(at3d);
            originVoxSize.add(mid);
        }

    }

    private void computeMipMapsCorrespondance() {
        AffineTransform3D at3D = new AffineTransform3D();
        for (int l=0;l<resamplingModel.getNumMipmapLevels();l++) {
            if (reuseMipMaps) {
                resamplingModel.getSourceTransform(0,l, at3D);
                double middleDim = SourceAndConverterHelper.getCharacteristicVoxelSize(at3D);
                int match = bestMatch(middleDim);
                mipmapModelToOrigin.put(l, match);
            } else {
                mipmapModelToOrigin.put(l, 0); // Always taking the highest resolution
            }

            // For debugging resampling issues
            logger.debug("Model mipmap level "+l+" correspond to origin mipmap level "+mipmapModelToOrigin.get(l));
            logger.debug("Model mipmap level "+l+" has a characteristic voxel size of "+
                    SourceAndConverterHelper.getCharacteristicVoxelSize(resamplingModel,0,l));
            logger.debug("Origin level "+mipmapModelToOrigin.get(l)+" has a characteristic voxel size of "+
                    SourceAndConverterHelper.getCharacteristicVoxelSize(origin,0,mipmapModelToOrigin.get(l)));

        }
    }

    public int getModelToOriginMipMapLevel(int mipmapModel) {
        return mipmapModelToOrigin.get(mipmapModel);
    }

    public Source<?> getOriginalSource() {
        return origin;
    }

    public Source<?> getModelResamplerSource() {
        return resamplingModel;
    }

    @Override
    public boolean isPresent(int t) {
        return origin.isPresent(t)&&resamplingModel.isPresent(t);
    }

    public boolean areMipmapsReused() {
        return reuseMipMaps;
    }

    public boolean isCached() {
        return cache;
    }

    public Interpolation originInterpolation() {
        return originInterpolation;
    }

    @Override
    public RandomAccessibleInterval<T> getSource(int t, int level) {
        if (cache) {
            if (!cachedRAIs.containsKey(t)) {
                cachedRAIs.put(t, new ConcurrentHashMap<>());
            }

            if (!cachedRAIs.get(t).containsKey(level)) {
                if (cache) {
                    RandomAccessibleInterval<T> nonCached = buildSource(t, level);

                    int[] blockSize = {64, 64, 64};

                    if (nonCached.dimension(0) < 64) blockSize[0] = (int) nonCached.dimension(0);
                    if (nonCached.dimension(1) < 64) blockSize[1] = (int) nonCached.dimension(1);
                    if (nonCached.dimension(2) < 64) blockSize[2] = (int) nonCached.dimension(2);

                    cachedRAIs.get(t).put(level, RAIHelper.wrapAsVolatileCachedCellImg(nonCached, blockSize));
                } else {
                    cachedRAIs.get(t).put(level,buildSource(t, level));
                }
            }
            return cachedRAIs.get(t).get(level);
        } else {
            return buildSource(t,level);
        }

    }

    public RandomAccessibleInterval<T> buildSource(int t, int level) {
        // Get current model source transformation
        AffineTransform3D at = new AffineTransform3D();
        resamplingModel.getSourceTransform(t,level,at);

        //int mipmap = getModelToOriginMipMapLevel(level);

        // Get bounds of model source RAI
        // TODO check if -1 is necessary
        long sx = resamplingModel.getSource(t,level).dimension(0)-1;
        long sy = resamplingModel.getSource(t,level).dimension(1)-1;
        long sz = resamplingModel.getSource(t,level).dimension(2)-1;

        // Get field of origin source
        final RealRandomAccessible<T> ipimg = origin.getInterpolatedSource(t, getModelToOriginMipMapLevel(level), originInterpolation);

        // Gets randomAccessible... ( with appropriate transform )
        at = at.inverse();
        AffineTransform3D atOrigin = new AffineTransform3D();
        origin.getSourceTransform(t, getModelToOriginMipMapLevel(level), atOrigin);
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
        resamplingModel.getSourceTransform(t,level,transform);
    }

    @Override
    public T getType() {
        return origin.getType();
    }

    @Override
    public String getName() {
        return origin.getName()+"_ResampledAs_"+resamplingModel.getName();
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return resamplingModel.getVoxelDimensions();
    }

    @Override
    public int getNumMipmapLevels() {
        return resamplingModel.getNumMipmapLevels();
    }

}
