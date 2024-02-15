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
package sc.fiji.bdvpg.transform;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.junit.After;
import org.junit.Test;
import org.scijava.util.VersionUtils;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BigWarpDemo2D {

    static ImageJ ij;

    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        ij = new ImageJ();
        TestHelper.startFiji(ij);//ij.ui().showUI();
        System.out.println("BigWarp version:"+VersionUtils.getVersion(BigWarp.class));
        SourceAndConverterService sourceService = ij.get(SourceAndConverterService.class);

        // Makes BDV Source
        final String filePath = "src/test/resources/demoSlice.xml";

        // --------------------------- MAKE SOURCES
        SourceAndConverter<?> fixedSource = takeFirstSource(filePath);
        SourceAndConverter<?> movingSource = takeFirstSource(filePath);

        // Chqnge moving source color
        sourceService.getConverterSetup(movingSource)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        // --------------------------- START BIGWARP
        startBigWarp("Default", fixedSource, movingSource, "src/test/resources/landmarks2d-demoSlice.csv");

        // --------------------------- START BIGWARP - ROTATE 90 DEGREES

        AffineTransform3D at3d = new AffineTransform3D();

        at3d.rotate(2, Math.PI/2.0);

        SourceAndConverter<?> rotatedMovingSource = new SourceAffineTransformer<>(takeFirstSource(filePath), at3d).get();
        // Chqnge moving source color
        sourceService.getConverterSetup(rotatedMovingSource)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        startBigWarp("Rotate PI/2", fixedSource, rotatedMovingSource, "src/test/resources/landmarks2d-demoSlice.csv");


    }

    public static SourceAndConverter<?> takeFirstSource(String xmlPath) {
        SourceAndConverterService sourceService = ij.get(SourceAndConverterService.class);
        // Fixed SourceAndConverter
        AbstractSpimData<?> spimDataFixed = new SpimDataFromXmlImporter(xmlPath).get();

        SourceAndConverter<?> source = sourceService
                .getSourceAndConverterFromSpimdata(spimDataFixed)
                .get(0);

        new BrightnessAutoAdjuster<>(source, 0).run();

        return source;
    }


    public static void startBigWarp(String bwName, SourceAndConverter<?> fixedSource, SourceAndConverter<?> movingSource, String landmarkFilePath) {
        SourceAndConverterService sourceService = ij.get(SourceAndConverterService.class);

        List<SourceAndConverter<?>> movingSources = new ArrayList<>();
        movingSources.add(movingSource);

        List<SourceAndConverter<?>> fixedSources = new ArrayList<>();
        fixedSources.add(fixedSource);

        List<ConverterSetup> converterSetups = movingSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList());
        converterSetups.addAll(fixedSources.stream().map(sourceService::getConverterSetup).collect(Collectors.toList()));

        BigWarpLauncher bwl = new BigWarpLauncher(movingSources, fixedSources, bwName, converterSetups);
        bwl.run();

        bwl.getBigWarp().loadLandmarks( landmarkFilePath );
        bwl.getBigWarp().toggleMovingImageDisplay();
        bwl.getBigWarp().matchActiveViewerPanelToOther();
    }

}