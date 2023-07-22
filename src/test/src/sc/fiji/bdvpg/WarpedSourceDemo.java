/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import bdv.util.BigWarpHelper;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import org.junit.After;
import org.junit.Test;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WarpedSourceDemo {

    static ImageJ ij;

    static public void main(String... args) throws Exception {
        // Arrange
        // create the ImageJ application context with all available services
        ij = new ImageJ();
        TestHelper.startFiji(ij);//ij.ui().showUI();

        demo();
    }

    @Test
    public void demoRunOk() throws Exception {
        main("");
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }

    public static void demo() throws Exception {

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

        SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sac);
        new ViewerTransformAdjuster(bdvHandle.getViewerPanel(), sac).run();
        new BrightnessAutoAdjuster<>(sac, 0).run();

        List<RealTransform> transform_tested = new ArrayList<>();

        // Warp 3D
        RealTransform warp3d = BigWarpHelper.realTransformFromBigWarpFile(new File("src/test/resources/landmarks3d-demo.csv"), true);
        transform_tested.add(warp3d);

        // Warp 2D
        RealTransform warp2d = BigWarpHelper.realTransformFromBigWarpFile(new File("src/test/resources/landmarks2d-demoSlice.csv"), true);
        transform_tested.add(warp2d);

        // Affine transform
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.translate(-150,0,0);
        transform_tested.add(at3D);

        // Real transform sequence
        RealTransformSequence rts = new RealTransformSequence();
        rts.add(at3D);
        rts.add(warp3d);
        transform_tested.add(rts);

        // --- Invertible real transform sequence

        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();
        irts.add(at3D.inverse());
        irts.add(at3D.inverse());
        //irts.add(((InvertibleRealTransform) warp3d).inverse()); //unfortunately this doesn't work because of
        //estimatebounds function which returns somewhere a singular matrix
        irts.add((InvertibleRealTransform) warp3d);
        transform_tested.add(irts);

        // TODO : Send bug report for singular matrix in estimate bounds

        // Register sources and display source
        for (RealTransform rt : transform_tested) {
            SourceAndConverter<?> transformed_source = new SourceRealTransformer(rt).apply(sac);

            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .register(transformed_source);

            SourceAndConverterServices
                    .getBdvDisplayService()
                    .show(bdvHandle, transformed_source);

            bdvHandle.getViewerPanel().showDebugTileOverlay();
            bdvHandle.getViewerPanel().getDisplay().repaint();

        }

    }

}
