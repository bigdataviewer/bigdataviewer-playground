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
package sc.fiji.bdvpg.tests.adapters.transforms;

import com.google.gson.Gson;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import org.junit.*;
import org.scijava.Context;
import net.imglib2.realtransform.AffineTransform3DRunTimeAdapter;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplierAdapter;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;
import sc.fiji.persist.IObjectScijavaAdapterService;
import sc.fiji.persist.ScijavaGsonHelper;

public class TransformSerializationTests {

    static Gson gson;
    static Context ctx;

    @Before
    public void buildGson() {
        ctx = new Context(IObjectScijavaAdapterService.class);
        gson = ScijavaGsonHelper.getGson(ctx);
    }

    @After
    public void closeCtx() {
        ctx.close();
    }

    /**
     * Test:
     * {@link DefaultBdvSupplierAdapter}
     */
    @Test
    public void testBdvSupplierSerialization() {
        DefaultBdvSupplier bdvSupplier = new DefaultBdvSupplier(new SerializableBdvOptions());
        testSerialization(gson, bdvSupplier, DefaultBdvSupplier.class);
    }

    /**
     * Test:
     * {@link AffineTransform3DRunTimeAdapter}
     */
    @Test
    public void testAffineTransformSerialization() {
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.scale(2);
        testSerialization(gson, at3D, AffineTransform3D.class); // This needs to work
        testSerialization(gson, at3D, RealTransform.class); // This needs to work
    }

    /**
     * Test: {@link net.imglib2.realtransform.RealTransformSequenceAdapter}
     */
    @Test
    public void testRealTransformSequenceSerialization() {
        AffineTransform3D t1 = new AffineTransform3D();
        t1.translate(10, 20, 30);

        AffineTransform3D t2 = new AffineTransform3D();
        t2.scale(2.0);

        RealTransformSequence sequence = new RealTransformSequence();
        sequence.add(t1);
        sequence.add(t2);

        testSerialization(gson, sequence, RealTransformSequence.class);
        testSerialization(gson, sequence, RealTransform.class);
    }

    /**
     * Test: {@link net.imglib2.realtransform.ThinPlateSplineTransformAdapter}
     */
    @Ignore // stupid rounding error in strings
    @Test
    public void testThinPlateSplineTransformSerialization() {
        // Create a simple TPS with 4 landmarks in 2D
        double[][] srcPts = new double[][] {
            {0, 10, 0, 10},  // X coordinates
            {0, 0, 10, 10}   // Y coordinates
        };
        double[][] tgtPts = new double[][] {
            {1, 11, 1, 11},  // Shifted X coordinates
            {1, 1, 11, 11}   // Shifted Y coordinates
        };

        ThinplateSplineTransform tps = new ThinplateSplineTransform(srcPts, tgtPts);

        testSerialization(gson, tps, ThinplateSplineTransform.class);
        testSerialization(gson, tps, RealTransform.class);
    }

    /**
     * Test that an AffineTransform3D actually works correctly after deserialization
     * (not just that the JSON is equal)
     */
    @Test
    public void testAffineTransformFunctionality() {
        AffineTransform3D original = new AffineTransform3D();
        original.translate(5, 10, 15);
        original.scale(2.0);
        original.rotate(2, Math.PI / 4); // Rotate around Z axis

        String json = gson.toJson(original, AffineTransform3D.class);
        AffineTransform3D restored = gson.fromJson(json, AffineTransform3D.class);

        // Test that the transform produces the same results
        double[] testPoint = {1.0, 2.0, 3.0};
        double[] originalResult = new double[3];
        double[] restoredResult = new double[3];

        original.apply(testPoint, originalResult);
        restored.apply(testPoint, restoredResult);

        Assert.assertArrayEquals("Transformed points should match",
            originalResult, restoredResult, 1e-10);
    }

    /**
     * Test that a ThinPlateSplineTransform actually works correctly after deserialization
     */
    @Test
    public void testThinPlateSplineTransformFunctionality() {
        double[][] srcPts = new double[][] {
            {0, 100, 0, 100},
            {0, 0, 100, 100}
        };
        double[][] tgtPts = new double[][] {
            {10, 110, 10, 110},
            {10, 10, 110, 110}
        };

        ThinplateSplineTransform original = new ThinplateSplineTransform(srcPts, tgtPts);

        String json = gson.toJson(original, RealTransform.class);
        RealTransform restored = gson.fromJson(json, RealTransform.class);

        // Test that the transform produces similar results at a test point
        RealPoint testPoint = new RealPoint(50.0, 50.0);
        RealPoint originalResult = new RealPoint(2);
        RealPoint restoredResult = new RealPoint(2);

        original.apply(testPoint, originalResult);
        restored.apply(testPoint, restoredResult);

        Assert.assertEquals("X coordinate should match",
            originalResult.getDoublePosition(0), restoredResult.getDoublePosition(0), 1e-6);
        Assert.assertEquals("Y coordinate should match",
            originalResult.getDoublePosition(1), restoredResult.getDoublePosition(1), 1e-6);
    }

    /**
     * Test serialization of a complex transform sequence
     */
    @Test
    public void testComplexTransformSequenceFunctionality() {
        AffineTransform3D translate = new AffineTransform3D();
        translate.translate(100, 200, 0);

        AffineTransform3D scale = new AffineTransform3D();
        scale.scale(0.5);

        AffineTransform3D rotate = new AffineTransform3D();
        rotate.rotate(2, Math.PI / 2); // 90 degrees around Z

        RealTransformSequence sequence = new RealTransformSequence();
        sequence.add(translate);
        sequence.add(scale);
        sequence.add(rotate);

        String json = gson.toJson(sequence, RealTransform.class);
        RealTransform restored = gson.fromJson(json, RealTransform.class);

        // Test at multiple points
        double[][] testPoints = {
            {0, 0, 0},
            {10, 20, 30},
            {-5, 15, -25}
        };

        for (double[] testPoint : testPoints) {
            double[] originalResult = new double[3];
            double[] restoredResult = new double[3];

            sequence.apply(testPoint, originalResult);
            restored.apply(testPoint, restoredResult);

            Assert.assertArrayEquals("Transform results should match for point " +
                java.util.Arrays.toString(testPoint),
                originalResult, restoredResult, 1e-10);
        }
    }

    /**
     * Test: {@link net.imglib2.realtransform.InvertibleRealTransformSequenceAdapter}
     * {@link net.imglib2.realtransform.InvertibleRealTransformSequenceRunTimeAdapter}
     */
    @Test
    public void testInvertibleRealTransformSequenceSerialization() {
        AffineTransform3D t1 = new AffineTransform3D();
        t1.translate(10, 20, 30);

        AffineTransform3D t2 = new AffineTransform3D();
        t2.scale(2.0);

        InvertibleRealTransformSequence sequence = new InvertibleRealTransformSequence();
        sequence.add(t1);
        sequence.add(t2);

        testSerialization(gson, sequence, InvertibleRealTransformSequence.class);
        testSerialization(gson, sequence, RealTransform.class);
    }

    /**
     * Test that InvertibleRealTransformSequence works correctly after deserialization,
     * including both forward and inverse transforms
     */
    @Test
    public void testInvertibleRealTransformSequenceFunctionality() {
        AffineTransform3D translate = new AffineTransform3D();
        translate.translate(100, 200, 50);

        AffineTransform3D scale = new AffineTransform3D();
        scale.scale(2.0);

        AffineTransform3D rotate = new AffineTransform3D();
        rotate.rotate(2, Math.PI / 4); // 45 degrees around Z

        InvertibleRealTransformSequence original = new InvertibleRealTransformSequence();
        original.add(translate);
        original.add(scale);
        original.add(rotate);

        String json = gson.toJson(original, RealTransform.class);
        RealTransform restored = gson.fromJson(json, RealTransform.class);

        Assert.assertTrue("Restored transform should be InvertibleRealTransform",
            restored instanceof InvertibleRealTransform);
        InvertibleRealTransform restoredInvertible = (InvertibleRealTransform) restored;

        // Test forward transform
        double[] testPoint = {10.0, 20.0, 30.0};
        double[] originalResult = new double[3];
        double[] restoredResult = new double[3];

        original.apply(testPoint, originalResult);
        restoredInvertible.apply(testPoint, restoredResult);

        Assert.assertArrayEquals("Forward transform should match",
            originalResult, restoredResult, 1e-10);

        // Test inverse transform
        double[] originalInverse = new double[3];
        double[] restoredInverse = new double[3];

        original.applyInverse(originalInverse, originalResult);
        restoredInvertible.applyInverse(restoredInverse, restoredResult);

        Assert.assertArrayEquals("Inverse transform should match",
            originalInverse, restoredInverse, 1e-10);

        // Verify that inverse brings us back to original point
        Assert.assertArrayEquals("Inverse should recover original point",
            testPoint, originalInverse, 1e-10);
    }

    // NOTE: Wrapped2DTransformAs3D tests are not included because they require
    // actual 2D transforms from imglib2, and using AffineTransform3D causes issues
    // with dimension checking. The adapters (Wrapped2DTransformAs3DRealTransformAdapter
    // and Wrapped2DTransformAs3DRealTransformRunTimeAdapter) are used when deserializing
    // sources that have been warped with 2D transforms, so they are tested indirectly
    // through source serialization tests.

    /**
     * Test: {@link net.imglib2.realtransform.WrappedIterativeInvertibleRealTransformAdapter}
     * {@link net.imglib2.realtransform.WrappedIterativeInvertibleRealTransformRunTimeAdapter}
     */
    @Test
    public void testWrappedIterativeInvertibleRealTransformSerialization() {
        // Create a TPS transform (which is not analytically invertible)
        double[][] srcPts = new double[][] {
            {0, 100, 0, 100},
            {0, 0, 100, 100}
        };
        double[][] tgtPts = new double[][] {
            {10, 110, 10, 110},
            {10, 10, 110, 110}
        };

        ThinplateSplineTransform tps = new ThinplateSplineTransform(srcPts, tgtPts);

        // Wrap it to make it iteratively invertible
        WrappedIterativeInvertibleRealTransform<ThinplateSplineTransform> wrapped =
            new WrappedIterativeInvertibleRealTransform<>(tps);

        testSerialization(gson, wrapped, WrappedIterativeInvertibleRealTransform.class);
        testSerialization(gson, wrapped, RealTransform.class);
    }

    /**
     * Test that WrappedIterativeInvertibleRealTransform works correctly after deserialization
     */
    @Test
    public void testWrappedIterativeInvertibleRealTransformFunctionality() {
        // Create a simple non-invertible transform (TPS)
        double[][] srcPts = new double[][] {
            {0, 50, 0, 50},
            {0, 0, 50, 50}
        };
        double[][] tgtPts = new double[][] {
            {5, 55, 5, 55},
            {5, 5, 55, 55}
        };

        ThinplateSplineTransform tps = new ThinplateSplineTransform(srcPts, tgtPts);
        WrappedIterativeInvertibleRealTransform<ThinplateSplineTransform> original =
            new WrappedIterativeInvertibleRealTransform<>(tps);

        String json = gson.toJson(original, RealTransform.class);
        RealTransform restored = gson.fromJson(json, RealTransform.class);

        Assert.assertTrue("Restored transform should be InvertibleRealTransform",
            restored instanceof InvertibleRealTransform);
        InvertibleRealTransform restoredInvertible = (InvertibleRealTransform) restored;

        // Test forward transform
        RealPoint testPoint = new RealPoint(25.0, 25.0);
        RealPoint originalResult = new RealPoint(2);
        RealPoint restoredResult = new RealPoint(2);

        original.apply(testPoint, originalResult);
        restoredInvertible.apply(testPoint, restoredResult);

        Assert.assertEquals("Forward X should match",
            originalResult.getDoublePosition(0), restoredResult.getDoublePosition(0), 1e-6);
        Assert.assertEquals("Forward Y should match",
            originalResult.getDoublePosition(1), restoredResult.getDoublePosition(1), 1e-6);

        // Test inverse transform (iterative approximation)
        RealPoint originalInverse = new RealPoint(2);
        RealPoint restoredInverse = new RealPoint(2);

        original.applyInverse(originalInverse, originalResult);
        restoredInvertible.applyInverse(restoredInverse, restoredResult);

        // Inverse is approximate, so use larger tolerance
        Assert.assertEquals("Inverse X should be close",
            originalInverse.getDoublePosition(0), restoredInverse.getDoublePosition(0), 0.1);
        Assert.assertEquals("Inverse Y should be close",
            originalInverse.getDoublePosition(1), restoredInverse.getDoublePosition(1), 0.1);
    }

    /**
     * Just makes a loop serialize / deserialize / re-serialize and checks
     * whether the string representation is identical
     *
     * @param gson serializer/deserializer
     * @param o object to serialize and deserialize
     * @param c class of the object
     */
    public static void testSerialization(Gson gson, Object o, Class<?> c) {
        String json = gson.toJson(o, c);
        System.out.println(json);
        Object oRestored = gson.fromJson(json, c);
        String json2 = gson.toJson(oRestored, c);
        System.out.println(json2);
        Assert.assertEquals(json, json2);
    }

}
