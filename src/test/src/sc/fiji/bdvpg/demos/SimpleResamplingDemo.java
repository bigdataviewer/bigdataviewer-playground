/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.demos;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imagej.ImageJ;
import net.imglib2.FinalRealInterval;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import sc.fiji.bdvpg.DemoHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

public class SimpleResamplingDemo {

    static ImageJ ij;

    static public void main(String... args) {
        // Arrange
        // create the ImageJ application context with all available services
        ij = new ImageJ();
        DemoHelper.startFiji(ij);//ij.ui().showUI();

        demo();

        DemoHelper.shot("SimpleResamplingDemo");
    }

    public static void demo() {

        // Get Model Source
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter("src/test/resources/mri-stack-multilevel.xml");
        AbstractSpimData<?> asd = importer.get();

        SourceAndConverter<UnsignedShortType> sac = (SourceAndConverter<UnsignedShortType>) SourceAndConverterServices
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

        final VoxelDimensions voxelDimensions = new FinalVoxelDimensions("micrometer", 0.5, 0.5, 3.0 );

        SourceAndConverter<?> model = new EmptySourceAndConverterCreator("Model",
                new FinalRealInterval(new double[]{50,50,50}, new double[]{150,150,150}),
                100,100,100, voxelDimensions).get();

        // Resample generative source as model source
        SourceAndConverter<UnsignedShortType> box =
                new SourceResampler<>(sac, model, "crop", false,false, false,0).get();

        SourceAndConverterServices.getBdvDisplayService().show( bdvHandle, box );

        new ColorChanger(box, new ARGBType(ARGBType.rgba(255, 0,0,255))).run();

    }
}
