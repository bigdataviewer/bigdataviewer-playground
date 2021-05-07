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

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.scijava.adapter.source.EmptySourceAdapter;

import java.io.Serializable;
import java.util.function.BiConsumer;

/**
 * An EmptySource is a source that is empty, but located in space thanks to
 * its 3D affine transform and also with a defined voxel size.
 *
 * This source can be thus used to define a template in order to Resample another source
 * with new bounds and voxel size, for instance as a model in {@link ResampledSource}
 *
 * Also this source can be serialized with the gson adapter {@link EmptySourceAdapter}
 * which is helpful to save such source easily.
 *
 */
public class EmptySource implements Source<UnsignedShortType>, Serializable {

    transient final RandomAccessibleInterval<UnsignedShortType> rai;

    EmptySourceParams params;

    transient protected final DefaultInterpolators< UnsignedShortType > interpolators = new DefaultInterpolators<>();

    public EmptySourceParams getParameters() {
        return new EmptySourceParams(params);
    }

    public EmptySource(EmptySourceParams p) {
       this(p.nx,p.ny,p.nz, p.at3D, p.name);
    }

    public EmptySource(long nx, long ny, long nz, AffineTransform3D at3D, String name) {

        params = new EmptySourceParams();
        params.nx = nx;
        params.ny = ny;
        params.nz = nz;

        BiConsumer<Localizable, UnsignedShortType > fun = (l,t) -> t.set(0);

        RandomAccessible<UnsignedShortType> ra = new FunctionRandomAccessible<>(3,
                fun, UnsignedShortType::new);

        this.rai = Views.interval(ra, new FinalInterval(nx,ny,nz));
        params.at3D = at3D;
        params.name = name;
    }

    @Override
    public boolean isPresent(int t) {
        return true;
    }

    @Override
    public RandomAccessibleInterval<UnsignedShortType> getSource(int t, int level) {
        return rai;
    }

    @Override
    public RealRandomAccessible<UnsignedShortType> getInterpolatedSource(int t, int level, Interpolation method) {
        ExtendedRandomAccessibleInterval<UnsignedShortType, RandomAccessibleInterval< UnsignedShortType >>
                eView = Views.extendZero(getSource( t, level ));
        return Views.interpolate( eView, interpolators.get(method) );
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        transform.set(params.at3D);
    }

    @Override
    public UnsignedShortType getType() {
        return new UnsignedShortType();
    }

    @Override
    public String getName() {
        return params.name;
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return new VoxelDimensions() {
            @Override
            public String unit() {
                return "undefined";
            }

            @Override
            public void dimensions(double[] dimensions) {
                dimensions[0] = 1;
                dimensions[1] = 1;
                dimensions[2] = 1;
            }

            @Override
            public double dimension(int d) {
                return 1;
            }

            @Override
            public int numDimensions() {
                return 3;
            }
        };
    }

    @Override
    public int getNumMipmapLevels() {
        return 1; // 0 or 1 ?
    }

    static public class EmptySourceParams implements Cloneable, Serializable {
        public long nx,ny,nz;
        public AffineTransform3D at3D;
        public String name;

        public EmptySourceParams() {
            nx = 1;
            ny = 1;
            nz = 1;
            at3D = new AffineTransform3D();
            name = "";
        }

        public EmptySourceParams(EmptySourceParams p) {
            nx = p.nx;
            ny = p.ny;
            nz = p.nz;
            at3D = new AffineTransform3D();
            at3D.set(p.at3D);
            name = p.name;
        }
    }
}
