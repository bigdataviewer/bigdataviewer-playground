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

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import org.junit.After;
import org.junit.Test;
import org.scijava.util.VersionUtils;
import sc.fiji.bdvpg.viewer.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.services.BdvService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BigWarpDemo {

    static ImageJ ij;

    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        ij = new ImageJ();
        TestHelper.startFiji(ij);//ij.ui().showUI();
        System.out.println("BigWarp version:"+VersionUtils.getVersion(BigWarp.class));
        demo2d(ij);
    }

    public static void demo3d(ImageJ ij) {
        SourceAndConverterService sourceService = ij.get(SourceAndConverterService.class);
        BdvService displayService = ij.get(BdvService.class);

        final String filePath = "src/test/resources/mri-stack.xml";
        // Import SpimData
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(filePath);

        AbstractSpimData<?> spimData = importer.get();

        SourceAndConverter<?> sacFixed = sourceService
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);

        importer = new SpimDataFromXmlImporter(filePath);

        spimData = importer.get();

        SourceAndConverter<?> sacMoving = sourceService
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);


        // Creates a BdvHandle
        BdvHandle bdvHandle = displayService.getActiveViewer();

        // Show the SourceAndConverter
        displayService.show(bdvHandle, sacFixed);

        sourceService.getConverterSetup(sacMoving)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        new BrightnessAutoAdjuster<>(sacFixed, 0).run();

        new BrightnessAutoAdjuster<>(sacMoving, 0).run();

        new ViewerTransformAdjuster(bdvHandle.getViewerPanel(), sacFixed).run();

        List<SourceAndConverter<?>> movingSources = new ArrayList<>();
        movingSources.add(sacMoving);

        List<SourceAndConverter<?>> fixedSources = new ArrayList<>();
        fixedSources.add(sacFixed);

        List<ConverterSetup> converterSetups = movingSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList());
        converterSetups.addAll(fixedSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList()));

        BigWarpLauncher bwl = new BigWarpLauncher(movingSources, fixedSources, "BigWarp Demo", converterSetups);
        bwl.run();

        bwl.getBigWarp().loadLandmarks( "src/test/resources/landmarks3d-demo.csv" );

        bwl.getBigWarp().toggleMovingImageDisplay();
        bwl.getBigWarp().matchActiveViewerPanelToOther();

        for (SourceAndConverter<?> sac : bwl.getWarpedSources()) {
            sourceService.register(sac);
        }
    }


    public static void demo2d(ImageJ ij) {
        SourceAndConverterService sourceService = ij.get(SourceAndConverterService.class);
        BdvService displayService = ij.get(BdvService.class);

        // Makes BDV Source

        final String filePath = "src/test/resources/demoSlice.xml";
        // Import SpimData
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(filePath);
        //importer.run();

        AbstractSpimData<?> spimData = importer.get();

        SourceAndConverter<?> sacFixed = sourceService
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);

        importer = new SpimDataFromXmlImporter(filePath);
        //importer.run();

        spimData = importer.get();

        SourceAndConverter<?> sacMoving = sourceService
                .getSourceAndConverterFromSpimdata(spimData)
                .get(0);


        // Creates a BdvHandle
        BdvHandle bdvHandle = displayService.getActiveViewer();

        // Show the SourceAndConverter
        displayService.show(bdvHandle, sacFixed);

        sourceService.getConverterSetup(sacMoving)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        new BrightnessAutoAdjuster<>(sacFixed, 0).run();

        new BrightnessAutoAdjuster<>(sacMoving, 0).run();

        new ViewerTransformAdjuster(bdvHandle.getViewerPanel(), sacFixed).run();

        List<SourceAndConverter<?>> movingSources = new ArrayList<>();
        movingSources.add(sacMoving);

        List<SourceAndConverter<?>> fixedSources = new ArrayList<>();
        fixedSources.add(sacFixed);

        List<ConverterSetup> converterSetups = movingSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList());
        converterSetups.addAll(fixedSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList()));

        BigWarpLauncher bwl = new BigWarpLauncher(movingSources, fixedSources, "BigWarp Demo", converterSetups);
        bwl.run();

        bwl.getBigWarp().loadLandmarks( "src/test/resources/landmarks2d-demoSlice.csv" );

        bwl.getBigWarp().toggleMovingImageDisplay();
        bwl.getBigWarp().matchActiveViewerPanelToOther();

        for (SourceAndConverter<?> sac : bwl.getWarpedSources()) {
            sourceService.register(sac);
        }

        BdvHandle bdvh = displayService.getNewViewer();

        displayService.show(bdvh, bwl.getWarpedSources()[0]);

        displayService.show(bdvh, fixedSources.get(0));

        //bdvh.getViewerPanel().showDebugTileOverlay();
        //bdvh.getViewerPanel().getDisplay().repaint();

        new ViewerTransformAdjuster(bdvh.getViewerPanel(), bwl.getWarpedSources()[0]).run();
    }

    @Test
    public void demoRunOk() {
        main("");
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }
}
