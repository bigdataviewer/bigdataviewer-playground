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
package sc.fiji.bdvpg.tests.adapters.sources;

import bdv.SpimSource;
import bdv.img.WarpedSource;
import bdv.util.EmptySource;
import bdv.util.ResampledSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import sc.fiji.bdvpg.scijava.services.SourceService;
import sc.fiji.bdvpg.services.SourceAndConverterServiceLoader;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;
import sc.fiji.persist.IObjectScijavaAdapterService;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for Source serialization/deserialization.
 * Tests individual source types in isolation to ensure their adapters work correctly.
 */
public class SourceSerializationTests {

    File tempFile;

    static Context ctx;

    @Before
    public void setUp() throws IOException {
        ctx = new Context(SourceService.class, IObjectScijavaAdapterService.class);
        // ctx.getService(UIService.class).setHeadless(true); too late
        tempFile = File.createTempFile("bdvpg_test_", ".json");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        ctx.close();
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    // ==================== EmptySource Tests ====================

    @Test
    public void testEmptySourceSerialization_withTransform() throws IOException {
        // Create an EmptySource with scale + rotation + translation
        EmptySource.EmptySourceParams params = new EmptySource.EmptySourceParams();
        params.nx = 256;
        params.ny = 256;
        params.nz = 128;
        params.name = "TestEmptySource";
        params.at3D = new AffineTransform3D();
        params.at3D.scale(0.5, 0.5, 1.0);
        params.at3D.rotate(1, Math.PI / 6); // 30 degrees around Y
        params.at3D.translate(50.0, 100.0, 25.0);
        params.setVoxelDimensions("um", 0.5, 0.5, 1.0);

        EmptySource originalSource = new EmptySource(params);
        SourceAndConverter<?> originalSrc = SourceAndConverterHelper.createSourceAndConverter(originalSource);

        // Register, save, clear, reload
        SourceAndConverterServices.getSourceAndConverterService().register(originalSrc);
        saveSource(originalSrc);
        clearAndReload();

        // Verify
        List<SourceAndConverter<?>> restoredSources = SourceAndConverterServices
                .getSourceAndConverterService().getSourceAndConverters();

        Assert.assertEquals("Should have one restored source", 1, restoredSources.size());

        EmptySource restoredSource = (EmptySource) restoredSources.get(0).getSpimSource();

        // Verify name and dimensions
        Assert.assertEquals("Name should match", "TestEmptySource", restoredSource.getName());
        EmptySource.EmptySourceParams restoredParams = restoredSource.getParameters();
        Assert.assertEquals("nx should match", 256, restoredParams.nx);
        Assert.assertEquals("ny should match", 256, restoredParams.ny);
        Assert.assertEquals("nz should match", 128, restoredParams.nz);

        // Verify transform by transforming a test point
        double[] testPoint = {10.0, 20.0, 30.0};
        double[] originalResult = new double[3];
        double[] restoredResult = new double[3];

        AffineTransform3D originalTransform = new AffineTransform3D();
        AffineTransform3D restoredTransform = new AffineTransform3D();
        originalSource.getSourceTransform(0, 0, originalTransform);
        restoredSource.getSourceTransform(0, 0, restoredTransform);

        originalTransform.apply(testPoint, originalResult);
        restoredTransform.apply(testPoint, restoredResult);

        Assert.assertArrayEquals("Transform should produce same results",
                originalResult, restoredResult, 1e-10);
    }

    // ==================== SpimSource Tests ====================

    @Test
    public void testSpimSourceSerialization() throws Exception {
        // Load SpimData from test resource
        String xmlPath = "src/test/resources/mri-stack.xml";
        new SpimDataFromXmlImporter(xmlPath).run();

        // Get the sources that were registered
        List<SourceAndConverter<?>> sources = SourceAndConverterServices
                .getSourceAndConverterService().getSourceAndConverters();

        Assert.assertFalse("Should have loaded sources from SpimData", sources.isEmpty());

        SourceAndConverter<?> originalSource = sources.get(0);
        String originalName = originalSource.getSpimSource().getName();

        // Sources are already registered by the importer
        saveSource(originalSource);
        clearAndReload();

        // Verify
        List<SourceAndConverter<?>> restoredSources = SourceAndConverterServices
                .getSourceAndConverterService().getSourceAndConverters();

        Assert.assertEquals("Should have one restored source", 1, restoredSources.size());

        SourceAndConverter<?> restoredSource = restoredSources.get(0);
        Assert.assertTrue("Should be a SpimSource", restoredSource.getSpimSource() instanceof SpimSource);
        Assert.assertEquals("Name should match", originalName, restoredSource.getSpimSource().getName());

        // Verify the source can provide data (basic functionality check)
        Assert.assertNotNull("Should be able to get source data",
                restoredSource.getSpimSource().getSource(0, 0));
    }

    // ==================== TransformedSource Tests ====================

    @Test
    public void testTransformedSourceSerialization() throws Exception {
        // Load a SpimData source and apply an affine transform
        String xmlPath = "src/test/resources/mri-stack.xml";
        new SpimDataFromXmlImporter(xmlPath).run();

        List<SourceAndConverter<?>> sources = SourceAndConverterServices
                .getSourceAndConverterService().getSourceAndConverters();

        SourceAndConverter<?> baseSource = sources.get(0);

        // Create an affine transform
        AffineTransform3D transform = new AffineTransform3D();
        transform.scale(2.0);
        transform.rotate(2, Math.PI / 4);
        transform.translate(100.0, 50.0, 25.0);

        // Apply transform to create a TransformedSource
        SourceAffineTransformer transformer = new SourceAffineTransformer(baseSource, transform);
        SourceAndConverter<?> transformedSource = transformer.get();

        SourceAndConverterServices.getSourceAndConverterService().register(transformedSource);

        // Save both the base and transformed source
        saveSources(Arrays.asList(baseSource, transformedSource));
        clearAndReload();

        // Verify
        List<SourceAndConverter<?>> restoredSources = SourceAndConverterServices
                .getSourceAndConverterService().getSourceAndConverters();

        Assert.assertEquals("Should have two restored sources", 2, restoredSources.size());

        // Find the TransformedSource (it wraps the SpimSource)
        SourceAndConverter<?> restoredTransformed = restoredSources.stream()
                .filter(source -> source.getSpimSource().getClass().getSimpleName().equals("TransformedSource"))
                .findFirst()
                .orElse(null);

        Assert.assertNotNull("Should have a TransformedSource", restoredTransformed);

        // Verify the transform is preserved by comparing transformed points
        double[] testPoint = {10.0, 20.0, 30.0};
        double[] originalResult = new double[3];
        double[] restoredResult = new double[3];

        AffineTransform3D originalTransform = new AffineTransform3D();
        AffineTransform3D restoredTransform = new AffineTransform3D();
        transformedSource.getSpimSource().getSourceTransform(0, 0, originalTransform);
        restoredTransformed.getSpimSource().getSourceTransform(0, 0, restoredTransform);

        originalTransform.apply(testPoint, originalResult);
        restoredTransform.apply(testPoint, restoredResult);

        Assert.assertArrayEquals("Transform should produce same results",
                originalResult, restoredResult, 1e-10);
    }

    // ==================== WarpedSource Tests ====================

    @Test
    public void testWarpedSourceSerialization_withThinPlateSpline() throws Exception {
        // Load a SpimData source
        String xmlPath = "src/test/resources/mri-stack.xml";
        new SpimDataFromXmlImporter(xmlPath).run();

        List<SourceAndConverter<?>> sources = SourceAndConverterServices
                .getSourceAndConverterService().getSourceAndConverters();

        SourceAndConverter<?> baseSource = sources.get(0);

        // Create a ThinPlateSpline transform with 4 landmarks
        double[][] srcPts = new double[][] {
            {0, 100, 0, 100},   // X coordinates
            {0, 0, 100, 100},   // Y coordinates
            {0, 0, 0, 0}        // Z coordinates (2D transform in 3D)
        };
        double[][] tgtPts = new double[][] {
            {10, 110, 10, 110}, // Shifted X
            {10, 10, 110, 110}, // Shifted Y
            {0, 0, 0, 0}        // Z unchanged
        };

        ThinplateSplineTransform tps = new ThinplateSplineTransform(srcPts, tgtPts);

        // Apply transform to create a WarpedSource
        SourceRealTransformer transformer = new SourceRealTransformer(baseSource, tps);
        SourceAndConverter<?> warpedSource = transformer.get();

        SourceAndConverterServices.getSourceAndConverterService().register(warpedSource);

        // Save both sources
        saveSources(Arrays.asList(baseSource, warpedSource));
        clearAndReload();

        // Verify
        List<SourceAndConverter<?>> restoredSources = SourceAndConverterServices
                .getSourceAndConverterService().getSourceAndConverters();

        Assert.assertEquals("Should have two restored sources", 2, restoredSources.size());

        // Find the WarpedSource
        SourceAndConverter<?> restoredWarped = restoredSources.stream()
                .filter(source -> source.getSpimSource() instanceof WarpedSource)
                .findFirst()
                .orElse(null);

        Assert.assertNotNull("Should have a WarpedSource", restoredWarped);

        // Verify the transform by applying to a test point
        WarpedSource<?> originalWarpedSource = (WarpedSource<?>) warpedSource.getSpimSource();
        WarpedSource<?> restoredWarpedSource = (WarpedSource<?>) restoredWarped.getSpimSource();

        RealTransform originalTransform = originalWarpedSource.getTransform();
        RealTransform restoredTransform = restoredWarpedSource.getTransform();

        double[] testPoint = {50.0, 50.0, 0.0};
        double[] originalResult = new double[3];
        double[] restoredResult = new double[3];

        originalTransform.apply(testPoint, originalResult);
        restoredTransform.apply(testPoint, restoredResult);

        Assert.assertArrayEquals("TPS transform should produce same results",
                originalResult, restoredResult, 1e-6);
    }

    // ==================== ResampledSource Tests ====================

    @Test
    public void testResampledSourceSerialization() throws Exception {
        // Load a SpimData source as the origin
        String xmlPath = "src/test/resources/mri-stack.xml";
        new SpimDataFromXmlImporter(xmlPath).run();

        List<SourceAndConverter<?>> sources = SourceAndConverterServices
                .getSourceAndConverterService().getSourceAndConverters();

        SourceAndConverter<?> originSource = sources.get(0);

        // Create an EmptySource as the model (defines the resampling grid)
        EmptySource.EmptySourceParams modelParams = new EmptySource.EmptySourceParams();
        modelParams.nx = 64;
        modelParams.ny = 64;
        modelParams.nz = 32;
        modelParams.name = "ResampleModel";
        modelParams.at3D = new AffineTransform3D();
        modelParams.at3D.scale(2.0); // Downsample by factor of 2

        EmptySource modelSource = new EmptySource(modelParams);
        SourceAndConverter<?> modelSrc = SourceAndConverterHelper.createSourceAndConverter(modelSource);
        SourceAndConverterServices.getSourceAndConverterService().register(modelSrc);

        // Create a ResampledSource
        SourceResampler resampler = new SourceResampler(
                originSource,
                modelSrc,
                "Resampled",
                false,  // reuseMipMaps
                true,   // cache
                false,  // interpolate (nearest neighbor)
                0       // defaultMipMapLevel
        );
        SourceAndConverter<?> resampledSource = resampler.get();

        SourceAndConverterServices.getSourceAndConverterService().register(resampledSource);

        // Save all three sources
        saveSources(Arrays.asList(originSource, modelSrc, resampledSource));
        clearAndReload();

        // Verify
        List<SourceAndConverter<?>> restoredSources = SourceAndConverterServices
                .getSourceAndConverterService().getSourceAndConverters();

        Assert.assertTrue("Should have at least three restored sources",
                restoredSources.size() >= 3);

        // Find the ResampledSource
        SourceAndConverter<?> restoredResampled = restoredSources.stream()
                .filter(source -> source.getSpimSource() instanceof ResampledSource)
                .findFirst()
                .orElse(null);

        Assert.assertNotNull("Should have a ResampledSource", restoredResampled);

        // Verify the resampled source name
        Assert.assertEquals("Name should match", "Resampled",
                restoredResampled.getSpimSource().getName());

        // Verify it can provide data
        Assert.assertNotNull("Should be able to get resampled source data",
                restoredResampled.getSpimSource().getSource(0, 0));
    }

    // ==================== Helper Methods ====================

    private void saveSource(SourceAndConverter<?> source) {
        saveSources(Collections.singletonList(source));
    }

    private void saveSources(List<SourceAndConverter<?>> sources) {
        new SourceAndConverterServiceSaver(
                tempFile,
                ctx,
                sources
        ).run();
    }

    private void clearAndReload() {
        // Clear existing sources
        SourceService sourceService = ctx.getService(SourceService.class);
        sourceService.remove(sourceService.getSourceAndConverters().toArray(new SourceAndConverter[0]));

        // Reload from file
        new SourceAndConverterServiceLoader(
                tempFile.getAbsolutePath(),
                tempFile.getParent(),
                ctx,
                false
        ).run();
    }
}
