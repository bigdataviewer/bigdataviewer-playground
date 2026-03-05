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
package sc.fiji.bdvpg.demos.transform;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.util.VersionUtils;
import sc.fiji.bdvpg.viewers.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.DemoHelper;
import sc.fiji.bdvpg.scijava.services.SourceBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceService;
import sc.fiji.bdvpg.source.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.source.register.BigWarpLauncher;
import sc.fiji.bdvpg.dataset.importer.XMLToDatasetImporter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BigWarpDemo {

    static ImageJ ij;

    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        ij = new ImageJ();
        DemoHelper.startFiji(ij);//ij.ui().showUI();
        System.out.println("BigWarp version:"+VersionUtils.getVersion(BigWarp.class));
        demo2d(ij);
    }

    public static void demo3d(ImageJ ij) {
        SourceService sourceService = ij.get(SourceService.class);
        SourceBdvDisplayService displayService = ij.get(SourceBdvDisplayService.class);

        final String filePath = "src/test/resources/mri-stack.xml";
        // Import SpimData
        XMLToDatasetImporter importer = new XMLToDatasetImporter(filePath);

        AbstractSpimData<?> spimData = importer.get();

        SourceAndConverter<?> sourceFixed = sourceService
                .getSourcesFromDataset(spimData)
                .get(0);

        importer = new XMLToDatasetImporter(filePath);

        spimData = importer.get();

        SourceAndConverter<?> sourceMoving = sourceService
                .getSourcesFromDataset(spimData)
                .get(0);


        // Creates a BdvHandle
        BdvHandle bdvHandle = displayService.getActiveBdv();

        // Show the SourceAndConverter
        displayService.show(bdvHandle, sourceFixed);

        sourceService.getConverterSetup(sourceMoving)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        new BrightnessAutoAdjuster<>(sourceFixed, 0).run();

        new BrightnessAutoAdjuster<>(sourceMoving, 0).run();

        new ViewerTransformAdjuster(bdvHandle, sourceFixed).run();

        List<SourceAndConverter<?>> movingSources = new ArrayList<>();
        movingSources.add(sourceMoving);

        List<SourceAndConverter<?>> fixedSources = new ArrayList<>();
        fixedSources.add(sourceFixed);

        List<ConverterSetup> converterSetups = movingSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList());
        converterSetups.addAll(fixedSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList()));

        BigWarpLauncher bwl = new BigWarpLauncher(movingSources, fixedSources, "BigWarp Demo", converterSetups);
        bwl.run();

        bwl.getBigWarp().loadLandmarks( "src/test/resources/landmarks3d-demo.csv" );

        bwl.getBigWarp().toggleMovingImageDisplay();
        bwl.getBigWarp().matchActiveViewerPanelToOther();

        for (SourceAndConverter<?> source : bwl.getWarpedSources()) {
            sourceService.register(source);
        }
    }


    public static void demo2d(ImageJ ij) {
        SourceService sourceService = ij.get(SourceService.class);
        SourceBdvDisplayService displayService = ij.get(SourceBdvDisplayService.class);

        // Makes BDV Source

        final String filePath = "src/test/resources/demoSlice.xml";
        // Import SpimData
        XMLToDatasetImporter importer = new XMLToDatasetImporter(filePath);
        //importer.run();

        AbstractSpimData<?> spimData = importer.get();

        SourceAndConverter<?> sourceFixed = sourceService
                .getSourcesFromDataset(spimData)
                .get(0);

        importer = new XMLToDatasetImporter(filePath);
        //importer.run();

        spimData = importer.get();

        SourceAndConverter<?> sourceMoving = sourceService
                .getSourcesFromDataset(spimData)
                .get(0);


        // Creates a BdvHandle
        BdvHandle bdvHandle = displayService.getActiveBdv();

        // Show the SourceAndConverter
        displayService.show(bdvHandle, sourceFixed);

        sourceService.getConverterSetup(sourceMoving)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        new BrightnessAutoAdjuster<>(sourceFixed, 0).run();

        new BrightnessAutoAdjuster<>(sourceMoving, 0).run();

        new ViewerTransformAdjuster(bdvHandle, sourceFixed).run();

        List<SourceAndConverter<?>> movingSources = new ArrayList<>();
        movingSources.add(sourceMoving);

        List<SourceAndConverter<?>> fixedSources = new ArrayList<>();
        fixedSources.add(sourceFixed);

        List<ConverterSetup> converterSetups = movingSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList());
        converterSetups.addAll(fixedSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList()));

        BigWarpLauncher bwl = new BigWarpLauncher(movingSources, fixedSources, "BigWarp Demo", converterSetups);
        bwl.run();

        bwl.getBigWarp().loadLandmarks( "src/test/resources/landmarks2d-demoSlice.csv" );

        bwl.getBigWarp().toggleMovingImageDisplay();
        bwl.getBigWarp().matchActiveViewerPanelToOther();

        for (SourceAndConverter<?> source : bwl.getWarpedSources()) {
            sourceService.register(source);
        }

        BdvHandle bdvh = displayService.getNewBdv();

        displayService.show(bdvh, bwl.getWarpedSources()[0]);

        displayService.show(bdvh, fixedSources.get(0));

        bdvh.getViewerPanel().showDebugTileOverlay();
        bdvh.getViewerPanel().getDisplay().repaint();

        new ViewerTransformAdjuster(bdvh, bwl.getWarpedSources()[0]).run();
    }

}
