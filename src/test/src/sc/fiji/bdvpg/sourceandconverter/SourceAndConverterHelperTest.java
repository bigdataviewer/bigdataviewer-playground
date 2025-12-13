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
package sc.fiji.bdvpg.sourceandconverter;

import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SourceAndConverterHelper} utility methods.
 * These tests cover pure mathematical functions that don't require
 * any ImageJ context or GUI components.
 */
public class SourceAndConverterHelperTest {

    private static final double EPSILON = 1e-10;

    // ==================== Vector Math Tests ====================

    @Test
    public void testNorm2() {
        RealPoint pt = new RealPoint(3.0, 4.0, 0.0);
        double result = SourceAndConverterHelper.norm2(pt);
        assertEquals(25.0, result, EPSILON); // 3^2 + 4^2 + 0^2 = 25
    }

    @Test
    public void testNorm2_unitVector() {
        RealPoint pt = new RealPoint(1.0, 0.0, 0.0);
        double result = SourceAndConverterHelper.norm2(pt);
        assertEquals(1.0, result, EPSILON);
    }

    @Test
    public void testNorm2_zeroVector() {
        RealPoint pt = new RealPoint(0.0, 0.0, 0.0);
        double result = SourceAndConverterHelper.norm2(pt);
        assertEquals(0.0, result, EPSILON);
    }

    @Test
    public void testNormalize3() {
        RealPoint pt = new RealPoint(3.0, 4.0, 0.0);
        SourceAndConverterHelper.normalize3(pt);

        // After normalization, the vector should have length 1
        double length = Math.sqrt(SourceAndConverterHelper.norm2(pt));
        assertEquals(1.0, length, EPSILON);

        // Check individual components (3/5, 4/5, 0)
        assertEquals(0.6, pt.getDoublePosition(0), EPSILON);
        assertEquals(0.8, pt.getDoublePosition(1), EPSILON);
        assertEquals(0.0, pt.getDoublePosition(2), EPSILON);
    }

    @Test
    public void testProdScal3() {
        RealPoint pt1 = new RealPoint(1.0, 2.0, 3.0);
        RealPoint pt2 = new RealPoint(4.0, 5.0, 6.0);

        double result = SourceAndConverterHelper.prodScal3(pt1, pt2);
        // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(32.0, result, EPSILON);
    }

    @Test
    public void testProdScal3_orthogonal() {
        RealPoint pt1 = new RealPoint(1.0, 0.0, 0.0);
        RealPoint pt2 = new RealPoint(0.0, 1.0, 0.0);

        double result = SourceAndConverterHelper.prodScal3(pt1, pt2);
        assertEquals(0.0, result, EPSILON); // Orthogonal vectors have dot product 0
    }

    @Test
    public void testProdVect() {
        // Cross product of X and Y unit vectors should give Z
        RealPoint xAxis = new RealPoint(1.0, 0.0, 0.0);
        RealPoint yAxis = new RealPoint(0.0, 1.0, 0.0);

        RealPoint result = SourceAndConverterHelper.prodVect(xAxis, yAxis);

        assertEquals(0.0, result.getDoublePosition(0), EPSILON);
        assertEquals(0.0, result.getDoublePosition(1), EPSILON);
        assertEquals(1.0, result.getDoublePosition(2), EPSILON);
    }

    @Test
    public void testProdVect_reverseOrder() {
        // Y x X should give -Z
        RealPoint xAxis = new RealPoint(1.0, 0.0, 0.0);
        RealPoint yAxis = new RealPoint(0.0, 1.0, 0.0);

        RealPoint result = SourceAndConverterHelper.prodVect(yAxis, xAxis);

        assertEquals(0.0, result.getDoublePosition(0), EPSILON);
        assertEquals(0.0, result.getDoublePosition(1), EPSILON);
        assertEquals(-1.0, result.getDoublePosition(2), EPSILON);
    }

    @Test
    public void testProdVect_parallelVectors() {
        // Cross product of parallel vectors should be zero
        RealPoint pt1 = new RealPoint(1.0, 2.0, 3.0);
        RealPoint pt2 = new RealPoint(2.0, 4.0, 6.0); // pt2 = 2 * pt1

        RealPoint result = SourceAndConverterHelper.prodVect(pt1, pt2);

        assertEquals(0.0, result.getDoublePosition(0), EPSILON);
        assertEquals(0.0, result.getDoublePosition(1), EPSILON);
        assertEquals(0.0, result.getDoublePosition(2), EPSILON);
    }

    // ==================== Voxel Size Tests ====================

    @Test
    public void testGetCharacteristicVoxelSize_identity() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();

        double voxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize(transform);
        assertEquals(1.0, voxelSize, EPSILON);
    }

    @Test
    public void testGetCharacteristicVoxelSize_uniformScale() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.scale(2.0);

        double voxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize(transform);
        assertEquals(2.0, voxelSize, EPSILON);
    }

    @Test
    public void testGetCharacteristicVoxelSize_anisotropic() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        // Set anisotropic scaling: X=1, Y=2, Z=10
        transform.set(1.0, 0, 0);  // X scale
        transform.set(2.0, 1, 1);  // Y scale
        transform.set(10.0, 2, 2); // Z scale

        // The characteristic voxel size should be the median: 2.0
        double voxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize(transform);
        assertEquals(2.0, voxelSize, EPSILON);
    }

    @Test
    public void testGetCharacteristicVoxelSize_anisotropicReordered() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        // Set anisotropic scaling: X=10, Y=1, Z=2
        transform.set(10.0, 0, 0);  // X scale
        transform.set(1.0, 1, 1);   // Y scale
        transform.set(2.0, 2, 2);   // Z scale

        // The characteristic voxel size should be the median: 2.0
        double voxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize(transform);
        assertEquals(2.0, voxelSize, EPSILON);
    }

    // ==================== Ray-Plane Intersection Tests ====================

    @Test
    public void testRayIntersectPlane_perpendicular() {
        // Ray along Z axis hitting XY plane at origin
        RealPoint rayOrigin = new RealPoint(0.0, 0.0, -5.0);
        RealPoint rayDirection = new RealPoint(0.0, 0.0, 1.0);
        RealPoint planeOrigin = new RealPoint(0.0, 0.0, 0.0);
        RealPoint u = new RealPoint(1.0, 0.0, 0.0);
        RealPoint v = new RealPoint(0.0, 1.0, 0.0);

        boolean intersects = SourceAndConverterHelper.rayIntersectPlane(
            rayOrigin, rayDirection, planeOrigin, u, v, 10, 10);

        assertTrue("Ray should intersect the plane", intersects);
    }

    @Test
    public void testRayIntersectPlane_parallel() {
        // Ray parallel to XY plane (should not intersect)
        RealPoint rayOrigin = new RealPoint(0.0, 0.0, 5.0);
        RealPoint rayDirection = new RealPoint(1.0, 0.0, 0.0); // Along X
        RealPoint planeOrigin = new RealPoint(0.0, 0.0, 0.0);
        RealPoint u = new RealPoint(1.0, 0.0, 0.0);
        RealPoint v = new RealPoint(0.0, 1.0, 0.0);

        boolean intersects = SourceAndConverterHelper.rayIntersectPlane(
            rayOrigin, rayDirection, planeOrigin, u, v, 10, 10);

        assertFalse("Ray parallel to plane should not intersect", intersects);
    }

    @Test
    public void testRayIntersectPlane_outsideBounds() {
        // Ray hits the infinite plane but outside the bounded region
        RealPoint rayOrigin = new RealPoint(100.0, 100.0, -5.0);
        RealPoint rayDirection = new RealPoint(0.0, 0.0, 1.0);
        RealPoint planeOrigin = new RealPoint(0.0, 0.0, 0.0);
        RealPoint u = new RealPoint(1.0, 0.0, 0.0);
        RealPoint v = new RealPoint(0.0, 1.0, 0.0);

        boolean intersects = SourceAndConverterHelper.rayIntersectPlane(
            rayOrigin, rayDirection, planeOrigin, u, v, 10, 10);

        assertFalse("Ray should not intersect within bounded region", intersects);
    }
}
