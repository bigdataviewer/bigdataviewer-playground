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

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.junit.After;
import org.junit.Test;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.importer.MandelbrotSourceGetter;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

public class ResamplingDemo {

    static ImageJ ij;

    static public void main(String... args) {
        // Arrange
        // create the ImageJ application context with all available services
        ij = new ImageJ();
        ij.ui().showUI();

        demo();
    }

    @Test
    public void demoRunOk() {
        main("");
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }

    public static void demo() {

        // --------------------- USE CASE 1:
        // --- Resample a functional source (or a warped / big warped source)
        // --- like a standard source backed by a RandomAccessibleInterval

        // Get Model Source
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter("src/test/resources/mri-stack-multilevel.xml");
        AbstractSpimData<?> asd = importer.get();

        SourceAndConverter<?> sac = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(asd)
                .get(0);

        SourceAndConverterServices
                .getBdvDisplayService()
                .show(sac);

        // Gets active BdvHandle instance
        BdvHandle bdvHandle = SourceAndConverterServices.getBdvDisplayService().getActiveBdv();

        SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, sac );
        new ViewerTransformAdjuster( bdvHandle, sac ).run();
        new BrightnessAutoAdjuster<>( sac, 0 ).run();

        // Get generative source (works with warped source as well)
        SourceAndConverter<?> mandelbrot = new MandelbrotSourceGetter().get();
        AffineTransform3D at3d = new AffineTransform3D();
        at3d.scale(600);
        at3d.translate(-100,-100,0);
        SourceAndConverter<?> bigMandelbrot = new SourceAffineTransformer<>(mandelbrot, at3d).getSourceOut();

        SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, bigMandelbrot );

        new BrightnessAdjuster(bigMandelbrot,0,800).run();

        // Resample generative source as model source
        SourceResampler sr = new SourceResampler(bigMandelbrot, sac, "resampled", false,false, false,0);
        SourceAndConverter<?> resampledMandelbrot = sr.get();

        SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, resampledMandelbrot );

        new ColorChanger(resampledMandelbrot, new ARGBType(ARGBType.rgba(255, 0,0,255))).run();

        // ------------------- USE CASE 2 :
        // ---- Downsample a source
        // ---- Upsample a source

        bdvHandle = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        //SourceAndConverterServices.getSourceAndConverterDisplayService().show( bdvHandle, sac );
        new ViewerTransformAdjuster( bdvHandle, sac ).run();

        // Make edge display on demand
        final int[] cellDimensions = new int[] { 32, 32, 32 };

        // Cached Image Factory Options
        final DiskCachedCellImgOptions factoryOptions = options()
                .cellDimensions( cellDimensions )
                .cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
                .maxCacheSize( 1 );

        // Creates cached image factory of Type UnsignedShort
        // final DiskCachedCellImgFactory<UnsignedShortType> factory = new DiskCachedCellImgFactory<>( new UnsignedShortType(), factoryOptions );

        // DOWNSAMPLING
        EmptySourceAndConverterCreator downSampledModel = new EmptySourceAndConverterCreator("DownSampled",sac,0,4,4,4);//, factory);

        sr = new SourceResampler(sac, downSampledModel.get(), "downsampled", false,false, true,0);
        SourceAndConverter<?> downsampledSource = sr.get();

        SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, downsampledSource );
        new ColorChanger( downsampledSource, new ARGBType(ARGBType.rgba(255, 120,0,255))).run();

        // DOWNSAMPLING With Mipmap Reuse

        sr = new SourceResampler(sac, downSampledModel.get(), "downsampled-with-mipmap", true,false, true,0);
        SourceAndConverter<?> downsampledSourceWithMipmaps = sr.get();

        SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, downsampledSourceWithMipmaps );
        new ColorChanger( downsampledSourceWithMipmaps, new ARGBType(ARGBType.rgba(120, 120,0,255))).run();

        // UPSAMPLING
        EmptySourceAndConverterCreator upSampledModel = new EmptySourceAndConverterCreator("UpSampled",sac,0,0.2,0.2,0.2);//, factory);

        sr = new SourceResampler(sac, upSampledModel.get(), "upsampled", false,false, true,0);
        SourceAndConverter<?> upsampledSource = sr.get();

        SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, upsampledSource );

        new ColorChanger( upsampledSource, new ARGBType(ARGBType.rgba(0, 0,255,255))).run();
    }
}
