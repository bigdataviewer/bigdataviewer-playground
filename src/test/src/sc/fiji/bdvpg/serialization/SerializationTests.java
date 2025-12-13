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
package sc.fiji.bdvpg.serialization;

import com.google.gson.Gson;
import net.imagej.ImageJ;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.ThinplateSplineTransform;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import net.imglib2.realtransform.AffineTransform3DRunTimeAdapter;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplierAdapter;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;
import sc.fiji.persist.ScijavaGsonHelper;

public class SerializationTests {

    static ImageJ ij;
    static Gson gson;

    @Before
    public void openFiji() {
        // Initializes static SourceService and Display Service and plugins for serialization
        ij = new ImageJ();
        TestHelper.startFiji(ij);//ij.ui().showUI();
        gson = ScijavaGsonHelper.getGson(ij.context());
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
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
