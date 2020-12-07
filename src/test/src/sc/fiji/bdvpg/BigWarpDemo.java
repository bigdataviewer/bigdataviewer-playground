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
package sc.fiji.bdvpg;

import bdv.tools.brightness.ConverterSetup;
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
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.junit.Ignore;
import org.junit.Test;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.importer.MandelbrotSourceGetter;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BigWarpDemo {

    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        demo2d();
    }

    public static void demo3d() {
        // load and convert an image
        //ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        //RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because BDV needs 3D
        //rai = Views.addDimension( rai, 0, 0 );

        // Makes BDV Source
        // Source blobs = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");

        final String filePath = "src/test/resources/mri-stack.xml";
        // Import SpimData
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(filePath);
        //importer.run();

        AbstractSpimData spimData = importer.get();

        SourceAndConverter sacFixed = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);

        importer = new SpimDataFromXmlImporter(filePath);
        //importer.run();

        spimData = importer.get();

        SourceAndConverter sacMoving = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);


        // Creates a BdvHandle
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        // Creates SourceAndConverter Reference
        //SourceAndConverter sacBlobs = SourceAndConverterUtils.createSourceAndConverter(blobs);

        // Show the sourceandconverter
        SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sacFixed);

        SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(sacMoving)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        new BrightnessAutoAdjuster(sacFixed, 0).run();

        new BrightnessAutoAdjuster(sacMoving, 0).run();

        new ViewerTransformAdjuster(bdvHandle, sacFixed).run();

        List<SourceAndConverter> movingSources = new ArrayList<>();
        movingSources.add(sacMoving);

        List<SourceAndConverter> fixedSources = new ArrayList<>();
        fixedSources.add(sacFixed);

        List<ConverterSetup> converterSetups = movingSources.stream().map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList());
        converterSetups.addAll(fixedSources.stream().map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList()));

        BigWarpLauncher bwl = new BigWarpLauncher(movingSources, fixedSources, "BigWarp Demo", converterSetups);
        bwl.run();

        bwl.getBigWarp().loadLandmarks( "src/test/resources/landmarks3d-demo.csv" );

        bwl.getBigWarp().toggleMovingImageDisplay();
        bwl.getBigWarp().matchActiveViewerPanelToOther();

        for (SourceAndConverter sac : bwl.getWarpedSources()) {
            SourceAndConverterServices.getSourceAndConverterService()
                    .register(sac);
        }
    }


    public static void demo2d() {
        // load and convert an image
        //ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        //RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because BDV needs 3D
        //rai = Views.addDimension( rai, 0, 0 );

        // Makes BDV Source
        // Source blobs = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");

        final String filePath = "src/test/resources/demoSlice.xml";
        // Import SpimData
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(filePath);
        //importer.run();

        AbstractSpimData spimData = importer.get();

        SourceAndConverter sacFixed = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);

        importer = new SpimDataFromXmlImporter(filePath);
        //importer.run();

        spimData = importer.get();

        SourceAndConverter sacMoving = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);


        // Creates a BdvHandle
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        // Creates SourceAndConverter Reference
        //SourceAndConverter sacBlobs = SourceAndConverterUtils.createSourceAndConverter(blobs);

        // Show the sourceandconverter
        SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sacFixed);

        SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(sacMoving)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        new BrightnessAutoAdjuster(sacFixed, 0).run();

        new BrightnessAutoAdjuster(sacMoving, 0).run();

        new ViewerTransformAdjuster(bdvHandle, sacFixed).run();

        List<SourceAndConverter> movingSources = new ArrayList<>();
        movingSources.add(sacMoving);

        List<SourceAndConverter> fixedSources = new ArrayList<>();
        fixedSources.add(sacFixed);

        List<ConverterSetup> converterSetups = movingSources.stream().map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList());
        converterSetups.addAll(fixedSources.stream().map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList()));

        BigWarpLauncher bwl = new BigWarpLauncher(movingSources, fixedSources, "BigWarp Demo", converterSetups);
        bwl.run();

        bwl.getBigWarp().loadLandmarks( "src/test/resources/landmarks2d-demoSlice.csv" );

        bwl.getBigWarp().toggleMovingImageDisplay();
        bwl.getBigWarp().matchActiveViewerPanelToOther();

        for (SourceAndConverter sac : bwl.getWarpedSources()) {
            SourceAndConverterServices.getSourceAndConverterService()
                    .register(sac);
        }
    }

    @Ignore // Needs BigWarp Fiji v > 6.0.3, but as of today, it is only 6.0.3 - SNAPSHOT - not ready for release
    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }
}
