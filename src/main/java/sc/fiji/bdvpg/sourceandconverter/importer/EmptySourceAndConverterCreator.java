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

import bdv.util.EmptySource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.function.Supplier;

/**
 * Action which generates a new Source which samples and spans a space region
 * defined by either :
 * - an affine transform and a number of voxels
 * - or a model source and voxel sizes
 *
 * Mipmap unsupported
 */

public class EmptySourceAndConverterCreator implements Runnable, Supplier<SourceAndConverter<?>> {

    private final AffineTransform3D at3D;

    private final long nx, ny, nz;

    private final String name;

    private final VoxelDimensions voxelDimensions;

    private static VoxelDimensions defaultVoxelDimensions = new FinalVoxelDimensions( "pixel", 1.0, 1.0, 1.0 );

    /**
     * Simple constructor
     * @param name name of the source
     * @param interval interval imaged by the source
     * @param sizeVoxX voxel size in x
     * @param sizeVoxY voxel size in y
     * @param sizeVoxZ voxel size in z
     */
    public EmptySourceAndConverterCreator(
            String name,
            RealInterval interval,
            double sizeVoxX, double sizeVoxY, double sizeVoxZ
    ) {
        this.name = name;

        AffineTransform3D at3D = new AffineTransform3D();
        this.nx = (long) ((interval.realMax(0)- interval.realMin(0))/sizeVoxX);
        this.ny = (long) ((interval.realMax(1)- interval.realMin(1))/sizeVoxY);
        this.nz = (long) ((interval.realMax(2)- interval.realMin(2))/sizeVoxZ);
        at3D.scale(sizeVoxX, sizeVoxY, sizeVoxZ);
        at3D.translate(interval.realMin(0), interval.realMin(1), interval.realMin(2));
        this.at3D = at3D;
        this.voxelDimensions = defaultVoxelDimensions;
    }

    /**
     * Simple constructor
     * @param name name of the source
     * @param interval interval imaged by the source
     * @param nx number of voxels in x
     * @param ny number of voxels in y
     * @param nz number of voxels in z
     */
    public EmptySourceAndConverterCreator(
            String name,
            RealInterval interval,
            long nx, long ny, long nz
    ) {
        this(name, interval, nx, ny, nz, defaultVoxelDimensions);
    }

    /**
     * Simple constructor
     * @param name name of the source
     * @param interval interval imaged by the source
     * @param nx number of voxels in x
     * @param ny number of voxels in y
     * @param nz number of voxels in z
     */
    public EmptySourceAndConverterCreator(
            String name,
            RealInterval interval,
            long nx, long ny, long nz,
            VoxelDimensions voxelDimensions
    ) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.name = name;

        AffineTransform3D at3D = new AffineTransform3D();
        double sizePx = (interval.realMax(0)- interval.realMin(0))/(double) nx;
        double sizePy = (interval.realMax(1)- interval.realMin(1))/(double) ny;
        double sizePz = (interval.realMax(2)- interval.realMin(2))/(double) nz;
        at3D.scale(sizePx, sizePy, sizePz);
        at3D.translate(interval.realMin(0), interval.realMin(1), interval.realMin(2));
        this.at3D = at3D;
        this.voxelDimensions = voxelDimensions;
    }

    /**
     * Simple constructor
     * @param name name of the source
     * @param at3D affine transform of the source
     * @param nx number of voxels in x
     * @param ny number of voxels in y
     * @param nz number of voxels in z
     */
    public EmptySourceAndConverterCreator(
            String name,
            AffineTransform3D at3D,
            long nx, long ny, long nz
    ) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.at3D = at3D;
        this.name = name;
        this.voxelDimensions = defaultVoxelDimensions;
    }

    /**
     * Constructor where the region and sampling is defined by a model source
     * This constructor translates information from the model source into
     * an affine transform and a number of voxels
     * @param name name of the source
     * @param model model source and converter : defines the portion of space sampled
     * @param timePoint timepoint of the model chosen for the model
     * @param voxSizeX overrides the model voxel size for a new one  - in bdv current units (x)
     * @param voxSizeY overrides the model voxel size for a new one - in bdv current units (y)
     * @param voxSizeZ overrides the model voxel size for a new one - in bdv current units (z)
     */
    public EmptySourceAndConverterCreator(
            String name,
            SourceAndConverter<?> model,
            int timePoint,
            double voxSizeX, double voxSizeY, double voxSizeZ
    ) {
        this.voxelDimensions = defaultVoxelDimensions;

        // Gets model RAI
        RandomAccessibleInterval<?> rai = model.getSpimSource().getSource(timePoint,0);

        long nPixModelX = rai.dimension(0);
        long nPixModelY = rai.dimension(1);
        long nPixModelZ = rai.dimension(2);

        // Gets transform of model RAI
        AffineTransform3D at3Dorigin = new AffineTransform3D();

        model.getSpimSource().getSourceTransform(timePoint,0,at3Dorigin);

        // Computes Voxel Size of model source (x, y, z)
        // And how it should be resampled to match the specified voxsize into the constructor
        // Origin
        double[] x0 = new double[3];
        at3Dorigin.apply(new double[]{0,0,0}, x0);

        // xMax
        double[] pt = new double[3];
        double dist;

        at3Dorigin.apply(new double[]{1,0,0},pt);

        dist = (pt[0]-x0[0])*(pt[0]-x0[0]) +
                (pt[1]-x0[1])*(pt[1]-x0[1]) +
                (pt[2]-x0[2])*(pt[2]-x0[2]);

        double distx =  Math.sqrt(dist);

        dist = Math.sqrt(dist)*nPixModelX;

        double nPixX = dist/voxSizeX;

        at3Dorigin.apply(new double[]{0,1,0},pt);

        dist = (pt[0]-x0[0])*(pt[0]-x0[0]) +
                (pt[1]-x0[1])*(pt[1]-x0[1]) +
                (pt[2]-x0[2])*(pt[2]-x0[2]);

        double disty =  Math.sqrt(dist);

        dist = Math.sqrt(dist)*nPixModelY;
        double nPixY = dist/voxSizeY;

        at3Dorigin.apply(new double[]{0,0,1},pt);

        dist = (pt[0]-x0[0])*(pt[0]-x0[0]) +
                (pt[1]-x0[1])*(pt[1]-x0[1]) +
                (pt[2]-x0[2])*(pt[2]-x0[2]);

        double distz =  Math.sqrt(dist);

        dist = Math.sqrt(dist)*nPixModelZ;
        double nPixZ = dist/voxSizeZ;

        // Gets original affine transform and rescales it accordingly
        double[] m = at3Dorigin.getRowPackedCopy();

        m[0] = m[0]/distx * voxSizeX;
        m[4] = m[4]/distx * voxSizeX;
        m[8] = m[8]/distx * voxSizeX;

        m[1] = m[1]/disty * voxSizeY;
        m[5] = m[5]/disty * voxSizeY;
        m[9] = m[9]/disty * voxSizeY;

        m[2] = m[2]/distz * voxSizeZ;
        m[6] = m[6]/distz * voxSizeZ;
        m[10] = m[10]/distz * voxSizeZ;

        at3Dorigin.set(m);

        this.name = name;
        this.at3D = at3Dorigin;
        this.nx = (long) nPixX;
        this.ny = (long) nPixY;
        this.nz = (long) nPixZ;

    }

    @Override
    public void run() {

    }

    @Override
    public SourceAndConverter<?> get() {

        Source<?> src = new EmptySource(nx,ny,nz,at3D,name,voxelDimensions);

        SourceAndConverter<?> sac;

        sac = SourceAndConverterHelper.createSourceAndConverter(src);

        return sac;
    }
}
