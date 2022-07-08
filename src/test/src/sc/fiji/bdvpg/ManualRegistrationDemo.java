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
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.junit.After;
import org.junit.Test;
import sc.fiji.bdvpg.bdv.ManualRegistrationStarter;
import sc.fiji.bdvpg.bdv.ManualRegistrationStopper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

/**
 * Manual Registration Demo
 *
 * Press Ctrl+M to enable / disable the movement of a source
 *
 * Author: Nicolas Chiaruttini
 * 01 2020
 */
public class ManualRegistrationDemo {

    static ImageJ ij;

    final public static int CreateNewTransformedSourceAndConverter = 0;
    final public static int MutateTransformedSourceAndConverter = 1;

    final public static int AppendNewSpimdataTransformation = 2;
    final public static int MutateLastSpimdataTransformation = 3;

    // Change to test the different options
    static int demoMode = AppendNewSpimdataTransformation;

    public static boolean isTransforming = false;

    public static void main(String[] args) {

        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ij = new ImageJ();
        ij.ui().showUI();

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because BDV needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes BDV Source
        Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");

        // Creates a BdvHandle
        BdvHandle bdvHandle = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        // Creates SourceAndConverter Reference
        SourceAndConverter sacReference = SourceAndConverterHelper.createSourceAndConverter(source);

        //int demoMode = CreateNewTransformedSourceAndConverter;
        //int demoMode = MutateTransformedSourceAndConverter;


        if (demoMode == CreateNewTransformedSourceAndConverter) {

            SourceAndConverter sacToTransform;
            sacToTransform = SourceAndConverterHelper.createSourceAndConverter(source);
            new ColorChanger(sacToTransform, new ARGBType(ARGBType.rgba(255, 0, 0, 255))).run();

            SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sacReference);
            SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sacToTransform);

            // Adjust view on sourceandconverter
            new ViewerTransformAdjuster(bdvHandle, sacReference).run();

            ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacToTransform);
            ManualRegistrationStopper manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    SourceTransformHelper::createNewTransformedSourceAndConverter
            );

            manualRegistrationStarter.run();

            new ClickBehaviourInstaller(bdvHandle, (x, y) -> {
                manualRegistrationStopper.run();
            }).install("Stop Transformation", "ctrl M");

        } else if (demoMode == MutateTransformedSourceAndConverter) {

            SourceAndConverter sacToTransform;
            sacToTransform = SourceAndConverterHelper.createSourceAndConverter(source);
            sacToTransform = new SourceAffineTransformer(sacToTransform, new AffineTransform3D()).getSourceOut();
            new ColorChanger(sacToTransform, new ARGBType(ARGBType.rgba(255, 0, 0, 255))).run();

            SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sacReference);
            SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sacToTransform);

            // Adjust view on sourceandconverter
            new ViewerTransformAdjuster(bdvHandle, sacReference).run();

            ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacToTransform);
            ManualRegistrationStopper manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    SourceTransformHelper::mutateTransformedSourceAndConverter
            );

            isTransforming = false;

            new ClickBehaviourInstaller(bdvHandle, (x,y) -> {
                if (isTransforming) {
                    manualRegistrationStopper.run();
                } else {
                    manualRegistrationStarter.run();
                }
                isTransforming = !isTransforming;
            }).install("Toggle Transformation", "ctrl M");
        } else if (demoMode == MutateLastSpimdataTransformation) {
            AbstractSpimData asd =  new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").get();

            // Show all SourceAndConverter associated with above SpimData
            SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters().forEach( sac -> {
                SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sac);
                new BrightnessAutoAdjuster(sac, 0).run();
            });

            SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sacReference);
            new ViewerTransformAdjuster(bdvHandle, sacReference).run();

            List<SourceAndConverter<?>> sacList = SourceAndConverterServices.getSourceAndConverterService()
                    .getSourceAndConverterFromSpimdata(asd);

            ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacList.toArray(new SourceAndConverter[sacList.size()]));
            ManualRegistrationStopper manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    SourceTransformHelper::mutateLastSpimdataTransformation
            );

            isTransforming = false;

            new ClickBehaviourInstaller(bdvHandle, (x,y) -> {
                if (isTransforming) {
                    manualRegistrationStopper.run();
                } else {
                    manualRegistrationStarter.run();
                }
                isTransforming = !isTransforming;
            }).install("Toggle Transformation", "ctrl M");

        }else if (demoMode == AppendNewSpimdataTransformation) {
            // TO complete
            // Import SpimData
            AbstractSpimData asd =  new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").get();

            // Show all SourceAndConverter associated with above SpimData
            SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters().forEach( sac -> {
                SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sac);
                new BrightnessAutoAdjuster(sac, 0).run();
            });

            SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sacReference);
            new ViewerTransformAdjuster(bdvHandle, sacReference).run();

            List<SourceAndConverter<?>> sacList = SourceAndConverterServices.getSourceAndConverterService()
                    .getSourceAndConverterFromSpimdata(asd);


            ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacList.toArray(new SourceAndConverter[sacList.size()]));
            ManualRegistrationStopper manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    SourceTransformHelper::appendNewSpimdataTransformation
            );

            isTransforming = false;

            new ClickBehaviourInstaller(bdvHandle, (x,y) -> {
                if (isTransforming) {
                    manualRegistrationStopper.run();
                } else {
                    manualRegistrationStarter.run();
                }
                isTransforming = !isTransforming;
            }).install("Toggle Transformation", "ctrl M");

        }

    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }

}
