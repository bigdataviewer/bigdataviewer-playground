/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.util.VersionUtils;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BigWarpDemo2D {

    static ImageJ ij;

    static SourceAndConverterService sourceService;

    static final String filePath = "src/test/resources/demoSlice.xml";

    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        ij = new ImageJ();
        TestHelper.startFiji(ij);//ij.ui().showUI();
        System.out.println("BigWarp version:"+VersionUtils.getVersion(BigWarp.class));
        sourceService = ij.get(SourceAndConverterService.class);

        // Makes BDV Source
        // --------------------------- START BIGWARP
        bigwarp();
        bigwarpRot();
        bigwarpRotTranslate();
        bigwarpRotPostTranslate();

    }

    public static void bigwarp() {

        // --------------------------- MAKE SOURCES
        SourceAndConverter<?> fixedSource = takeFirstSource(filePath);
        SourceAndConverter<?> movingSource = takeFirstSource(filePath);

        // Chqnge moving source color
        sourceService.getConverterSetup(movingSource)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));


        startBigWarp("Rotate PI/2", fixedSource, movingSource, "src/test/resources/landmarks2d-demoSlice.csv");
    }

    public static void bigwarpRot() {

        // --------------------------- MAKE SOURCES
        SourceAndConverter<?> fixedSource = takeFirstSource(filePath);

        AffineTransform3D rot90 = new AffineTransform3D();

        rot90.rotate(2, Math.PI/2.0);

        SourceAndConverter<?> rotatedMovingSource = new SourceAffineTransformer<>(takeFirstSource(filePath), rot90).get();

        // Chqnge moving source color
        sourceService.getConverterSetup(rotatedMovingSource)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        startBigWarp("Rotate PI/2", fixedSource, rotatedMovingSource, "src/test/resources/landmarks2d-demoSlice.csv");
    }

    public static void bigwarpRotTranslate() {

        // --------------------------- MAKE SOURCES
        SourceAndConverter<?> fixedSource = takeFirstSource(filePath);

        AffineTransform3D rot90 = new AffineTransform3D();

        rot90.rotate(2, Math.PI/2.0);

        SourceAndConverter<?> rotatedMovingSource = new SourceAffineTransformer<>(takeFirstSource(filePath), rot90).get();


        AffineTransform3D translateRight = new AffineTransform3D();
        translateRight.translate(3,0,0);

        SourceAndConverter<?> rotatedTranslatedMovingSource = SourceTransformHelper.mutate(translateRight, new SourceAndConverterAndTimeRange<>(rotatedMovingSource,0,1));

        // Chqnge moving source color
        sourceService.getConverterSetup(rotatedMovingSource)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        startBigWarp("Rotate PI/2", fixedSource, rotatedTranslatedMovingSource, "src/test/resources/landmarks2d-demoSlice.csv");
    }

    public static void bigwarpRotPostTranslate() {

        // --------------------------- MAKE SOURCES
        SourceAndConverter<?> fixedSource = takeFirstSource(filePath);

        AffineTransform3D rot90 = new AffineTransform3D();

        rot90.rotate(2, Math.PI/2.0);

        SourceAndConverter<?> rotatedMovingSource = new SourceAffineTransformer<>(takeFirstSource(filePath), rot90).get();

        // Change moving source color
        sourceService.getConverterSetup(rotatedMovingSource)
                .setColor(new ARGBType(ARGBType.rgba(0, 255, 255,0)));

        startBigWarp("Rotate PI/2", fixedSource, rotatedMovingSource, "src/test/resources/landmarks2d-demoSlice.csv");


        AffineTransform3D translateRight = new AffineTransform3D();
        translateRight.translate(5,0,0);

        SourceTransformHelper.mutate(translateRight, new SourceAndConverterAndTimeRange<>(rotatedMovingSource,0,1));

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
