/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import net.imglib2.RandomAccessibleInterval;
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
 * TimePoint 0 supported only TODO : improve timepoint support
 */

public class EmptySourceAndConverterCreator implements Runnable, Supplier<SourceAndConverter> {

    AffineTransform3D at3D;

    long nx, ny, nz;

    String name;

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
            SourceAndConverter model,
            int timePoint,
            double voxSizeX, double voxSizeY, double voxSizeZ
    ) {
        // Gets model RAI
        RandomAccessibleInterval rai = model.getSpimSource().getSource(timePoint,0);

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
    public SourceAndConverter get() {

        Source src = new EmptySource(nx,ny,nz,at3D,name);

        SourceAndConverter sac;

        sac = SourceAndConverterHelper.createSourceAndConverter(src);

        return sac;
    }
}
