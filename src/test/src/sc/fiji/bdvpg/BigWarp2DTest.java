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
package sc.fiji.bdvpg;

import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bigwarp.BigWarp;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.BigWarpInit;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

/*
 * See
 * https://github.com/bigdataviewer/bigdataviewer-playground/issues/249
 */
public class BigWarp2DTest {

    public static void main(String[] args) throws SpimDataException {
        run();
    }

    public static <T extends NativeType<T> & RealType<T>> void run() throws SpimDataException
    {
        Source<T> mvg = loadSource("https://imagej.nih.gov/ij/images/boats.gif",  20);
        Source<T> tgt = loadSource("https://imagej.nih.gov/ij/images/boats.gif", -20);

        BigWarpData<T> bwdata = BigWarpInit.initData();
        BigWarpInit.add(bwdata, mvg, 0, 0, true);
        BigWarpInit.add(bwdata, tgt, 1, 0, false);
        bwdata.wrapUp();

        BigWarp<?> bw = new BigWarp<>(bwdata, "bw test", null);
    }

    public static <T extends NativeType<T> & RealType<T>> Source<T> loadSource( String path, double zOffset )
    {
        ImagePlus imp = IJ.openImage(path);
        RandomAccessibleInterval<T> img = ImageJFunctions.wrap(imp);
        if( img.numDimensions() == 2 )
            img = Views.addDimension(img, 0, 0);

        AffineTransform3D xfm = new AffineTransform3D();
        xfm.translate(0, 0, zOffset);

        return new RandomAccessibleIntervalSource<>(img, Util.getTypeFromInterval(img), xfm, imp.getTitle());
    }

}
