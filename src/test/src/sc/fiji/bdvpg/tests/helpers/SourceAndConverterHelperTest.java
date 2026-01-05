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
package sc.fiji.bdvpg.tests.helpers;

import bdv.tools.transformation.TransformedSource;
import bdv.util.EmptySource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RealPoint;
import net.imglib2.converter.Converter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.junit.Test;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.List;

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

    @Test
    public void testRayIntersectPlane_atEdge() {
        // Ray hitting exactly at the edge of the bounded region
        RealPoint rayOrigin = new RealPoint(5.0, 5.0, -5.0);
        RealPoint rayDirection = new RealPoint(0.0, 0.0, 1.0);
        RealPoint planeOrigin = new RealPoint(0.0, 0.0, 0.0);
        RealPoint u = new RealPoint(1.0, 0.0, 0.0);
        RealPoint v = new RealPoint(0.0, 1.0, 0.0);

        boolean intersects = SourceAndConverterHelper.rayIntersectPlane(
            rayOrigin, rayDirection, planeOrigin, u, v, 10, 10);

        assertTrue("Ray should intersect within the bounded region", intersects);
    }

    @Test
    public void testRayIntersectPlane_diagonal() {
        // Ray at 45 degrees hitting the plane
        RealPoint rayOrigin = new RealPoint(0.0, 0.0, -5.0);
        RealPoint rayDirection = new RealPoint(1.0, 1.0, 1.0);
        SourceAndConverterHelper.normalize3(rayDirection);
        RealPoint planeOrigin = new RealPoint(0.0, 0.0, 0.0);
        RealPoint u = new RealPoint(1.0, 0.0, 0.0);
        RealPoint v = new RealPoint(0.0, 1.0, 0.0);

        boolean intersects = SourceAndConverterHelper.rayIntersectPlane(
            rayOrigin, rayDirection, planeOrigin, u, v, 20, 20);

        assertTrue("Diagonal ray should intersect the plane", intersects);
    }

    // ==================== Additional Vector Math Tests ====================

    @Test
    public void testNorm2_negativeValues() {
        RealPoint pt = new RealPoint(-3.0, -4.0, 0.0);
        double result = SourceAndConverterHelper.norm2(pt);
        assertEquals(25.0, result, EPSILON); // (-3)^2 + (-4)^2 + 0^2 = 25
    }

    @Test
    public void testNorm2_3dVector() {
        RealPoint pt = new RealPoint(1.0, 2.0, 2.0);
        double result = SourceAndConverterHelper.norm2(pt);
        assertEquals(9.0, result, EPSILON); // 1 + 4 + 4 = 9
    }

    @Test
    public void testProdScal3_selfDot() {
        // Dot product of a vector with itself is its squared norm
        RealPoint pt = new RealPoint(3.0, 4.0, 0.0);
        double result = SourceAndConverterHelper.prodScal3(pt, pt);
        assertEquals(25.0, result, EPSILON);
    }

    @Test
    public void testProdScal3_negativeResult() {
        RealPoint pt1 = new RealPoint(1.0, 0.0, 0.0);
        RealPoint pt2 = new RealPoint(-1.0, 0.0, 0.0);
        double result = SourceAndConverterHelper.prodScal3(pt1, pt2);
        assertEquals(-1.0, result, EPSILON);
    }

    @Test
    public void testProdVect_generalCase() {
        // General cross product
        RealPoint pt1 = new RealPoint(1.0, 2.0, 3.0);
        RealPoint pt2 = new RealPoint(4.0, 5.0, 6.0);

        RealPoint result = SourceAndConverterHelper.prodVect(pt1, pt2);

        // Expected: (2*6 - 3*5, 3*4 - 1*6, 1*5 - 2*4) = (-3, 6, -3)
        assertEquals(-3.0, result.getDoublePosition(0), EPSILON);
        assertEquals(6.0, result.getDoublePosition(1), EPSILON);
        assertEquals(-3.0, result.getDoublePosition(2), EPSILON);
    }

    // ==================== Additional Voxel Size Tests ====================

    @Test
    public void testGetCharacteristicVoxelSize_withRotation() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.scale(2.0);
        transform.rotate(2, Math.PI / 4); // Rotate around Z axis

        // Rotation shouldn't change the characteristic voxel size
        double voxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize(transform);
        assertEquals(2.0, voxelSize, EPSILON);
    }

    @Test
    public void testGetCharacteristicVoxelSize_allDifferent() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        // Set scaling: X=0.5, Y=1.5, Z=3.0
        transform.set(0.5, 0, 0);
        transform.set(1.5, 1, 1);
        transform.set(3.0, 2, 2);

        // The characteristic voxel size should be the median: 1.5
        double voxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize(transform);
        assertEquals(1.5, voxelSize, EPSILON);
    }

    @Test
    public void testGetCharacteristicVoxelSize_twoEqual() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        // Set scaling: X=2.0, Y=2.0, Z=5.0
        transform.set(2.0, 0, 0);
        transform.set(2.0, 1, 1);
        transform.set(5.0, 2, 2);

        // Median of (2, 2, 5) is 2
        double voxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize(transform);
        assertEquals(2.0, voxelSize, EPSILON);
    }

    // ==================== Source-Based Tests ====================

    /**
     * Creates a simple EmptySource for testing.
     */
    private EmptySource createTestSource(long nx, long ny, long nz, String name) {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        return new EmptySource(nx, ny, nz, transform, name, null);
    }

    /**
     * Creates an EmptySource with a custom transform.
     */
    private EmptySource createTestSource(long nx, long ny, long nz, String name,
                                         AffineTransform3D transform) {
        return new EmptySource(nx, ny, nz, transform, name, null);
    }

    @Test
    public void testHasAValidTimepoint_emptySource() {
        EmptySource source = createTestSource(100, 100, 50, "TestSource");

        // EmptySource.isPresent() always returns true
        boolean hasValid = SourceAndConverterHelper.hasAValidTimepoint(source);
        assertTrue("EmptySource should always have a valid timepoint", hasValid);
    }

    @Test
    public void testGetAValidTimepoint_emptySource() {
        EmptySource source = createTestSource(100, 100, 50, "TestSource");

        int validTimepoint = SourceAndConverterHelper.getAValidTimepoint(source);
        // EmptySource.isPresent(0) returns true, so it should return 0
        assertEquals(0, validTimepoint);
    }

    @Test
    public void testIsNotGenerative_emptySource() {
        EmptySource source = createTestSource(100, 100, 50, "TestSource");

        // EmptySource.isPresent(-1) returns true, so it IS generative
        boolean isNotGenerative = SourceAndConverterHelper.isNotGenerative(source);
        assertFalse("EmptySource is generative (present at t=-1)", isNotGenerative);
    }

    // ==================== getRootSource Tests ====================

    @Test
    public void testGetRootSource_simpleSource() {
        EmptySource source = createTestSource(100, 100, 50, "RootSource");
        AffineTransform3D chainedTransform = new AffineTransform3D();

        Source<?> root = SourceAndConverterHelper.getRootSource(source, chainedTransform);

        assertSame("Root of a simple source is itself", source, root);
        // Chained transform should be identity
        assertTrue("Chained transform should be identity", chainedTransform.isIdentity());
    }

    @Test
    public void testGetRootSource_transformedSource() {
        EmptySource baseSource = createTestSource(100, 100, 50, "BaseSource");
        TransformedSource<UnsignedShortType> transformedSource = new TransformedSource<>(baseSource);

        // Apply a fixed transform
        AffineTransform3D fixedTransform = new AffineTransform3D();
        fixedTransform.translate(10, 20, 30);
        transformedSource.setFixedTransform(fixedTransform);

        AffineTransform3D chainedTransform = new AffineTransform3D();
        Source<?> root = SourceAndConverterHelper.getRootSource(transformedSource, chainedTransform);

        assertSame("Root should be the base EmptySource", baseSource, root);
        // Chained transform should include the fixed transform
        assertEquals(10.0, chainedTransform.get(0, 3), EPSILON);
        assertEquals(20.0, chainedTransform.get(1, 3), EPSILON);
        assertEquals(30.0, chainedTransform.get(2, 3), EPSILON);
    }

    @Test
    public void testGetRootSource_nestedTransformedSource() {
        EmptySource baseSource = createTestSource(100, 100, 50, "BaseSource");

        // First transformation
        TransformedSource<UnsignedShortType> transformed1 = new TransformedSource<>(baseSource);
        AffineTransform3D transform1 = new AffineTransform3D();
        transform1.translate(10, 0, 0);
        transformed1.setFixedTransform(transform1);

        // Second transformation
        TransformedSource<UnsignedShortType> transformed2 = new TransformedSource<>(transformed1);
        AffineTransform3D transform2 = new AffineTransform3D();
        transform2.translate(0, 20, 0);
        transformed2.setFixedTransform(transform2);

        AffineTransform3D chainedTransform = new AffineTransform3D();
        Source<?> root = SourceAndConverterHelper.getRootSource(transformed2, chainedTransform);

        assertSame("Root should be the base EmptySource", baseSource, root);
        // Chained transform should include both transforms
        assertEquals(10.0, chainedTransform.get(0, 3), EPSILON);
        assertEquals(20.0, chainedTransform.get(1, 3), EPSILON);
        assertEquals(0.0, chainedTransform.get(2, 3), EPSILON);
    }

    // ==================== Voxel Position Tests ====================

    @Test
    public void testGetVoxelPositionInSource_origin() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);

        RealPoint globalPos = new RealPoint(0.0, 0.0, 0.0);
        long[] voxelPos = SourceAndConverterHelper.getVoxelPositionInSource(source, globalPos, 0, 0);

        assertEquals(0L, voxelPos[0]);
        assertEquals(0L, voxelPos[1]);
        assertEquals(0L, voxelPos[2]);
    }

    @Test
    public void testGetVoxelPositionInSource_translated() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.translate(10.0, 20.0, 30.0);
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);

        // Global position (10, 20, 30) should map to voxel (0, 0, 0) in source
        RealPoint globalPos = new RealPoint(10.0, 20.0, 30.0);
        long[] voxelPos = SourceAndConverterHelper.getVoxelPositionInSource(source, globalPos, 0, 0);

        assertEquals(0L, voxelPos[0]);
        assertEquals(0L, voxelPos[1]);
        assertEquals(0L, voxelPos[2]);
    }

    @Test
    public void testGetVoxelPositionInSource_scaled() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.scale(2.0); // Each voxel is 2 units
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);

        // Global position (10, 10, 10) should map to voxel (5, 5, 5)
        RealPoint globalPos = new RealPoint(10.0, 10.0, 10.0);
        long[] voxelPos = SourceAndConverterHelper.getVoxelPositionInSource(source, globalPos, 0, 0);

        assertEquals(5L, voxelPos[0]);
        assertEquals(5L, voxelPos[1]);
        assertEquals(5L, voxelPos[2]);
    }

    // ==================== Center Point Tests ====================

    @Test
    public void testGetSourceAndConverterCenterPoint_identity() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        RealPoint center = SourceAndConverterHelper.getSourceAndConverterCenterPoint(sac, 0);

        // Center of 100x100x50 volume at identity transform: (49.5, 49.5, 24.5)
        assertEquals(49.5, center.getDoublePosition(0), EPSILON);
        assertEquals(49.5, center.getDoublePosition(1), EPSILON);
        assertEquals(24.5, center.getDoublePosition(2), EPSILON);
    }

    @Test
    public void testGetSourceAndConverterCenterPoint_translated() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.translate(100.0, 200.0, 300.0);
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        RealPoint center = SourceAndConverterHelper.getSourceAndConverterCenterPoint(sac, 0);

        // Center should be translated
        assertEquals(149.5, center.getDoublePosition(0), EPSILON);
        assertEquals(249.5, center.getDoublePosition(1), EPSILON);
        assertEquals(324.5, center.getDoublePosition(2), EPSILON);
    }

    @Test
    public void testGetSourceAndConverterCenterPoint_scaled() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.scale(2.0);
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        RealPoint center = SourceAndConverterHelper.getSourceAndConverterCenterPoint(sac, 0);

        // Center in pixel space is (49.5, 49.5, 24.5), scaled by 2
        assertEquals(99.0, center.getDoublePosition(0), EPSILON);
        assertEquals(99.0, center.getDoublePosition(1), EPSILON);
        assertEquals(49.0, center.getDoublePosition(2), EPSILON);
    }

    // ==================== bestLevel Tests ====================

    @Test
    public void testBestLevel_singleLevel() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);

        // EmptySource has only 1 mipmap level
        int level = SourceAndConverterHelper.bestLevel(source, 0, 1.0);
        assertEquals("Single level source should return level 0", 0, level);
    }

    @Test
    public void testBestLevel_belowHighestRes() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);

        // Requesting voxel size smaller than the source has
        int level = SourceAndConverterHelper.bestLevel(source, 0, 0.5);
        assertEquals("Should return level 0 for sub-resolution request", 0, level);
    }

    // ==================== Ray Intersection with Source Tests ====================

    @Test
    public void testRayIntersect_nullSource() {
        SourceAndConverter<?> sac = new SourceAndConverter<>(null, null);
        RealPoint origin = new RealPoint(0.0, 0.0, 0.0);
        RealPoint direction = new RealPoint(0.0, 0.0, 1.0);

        List<Double> intersections = SourceAndConverterHelper.rayIntersect(sac, 0, origin, direction);

        assertTrue("Null source should return empty list", intersections.isEmpty());
    }

    @Test
    public void testRayIntersect_emptySource_alongZ() {
        // Ray along Z axis through the center of a 100x100x50 source
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        RealPoint origin = new RealPoint(50.0, 50.0, -10.0);
        RealPoint direction = new RealPoint(0.0, 0.0, 1.0);

        List<Double> intersections = SourceAndConverterHelper.rayIntersect(sac, 0, origin, direction);

        assertNotNull("Should return intersection list", intersections);
        assertEquals("Should have 50 Z planes", 50, intersections.size());
    }

    @Test
    public void testRayIntersect_emptySource_alongX() {
        // Ray along X axis
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        RealPoint origin = new RealPoint(-10.0, 50.0, 25.0);
        RealPoint direction = new RealPoint(1.0, 0.0, 0.0);

        List<Double> intersections = SourceAndConverterHelper.rayIntersect(sac, 0, origin, direction);

        assertNotNull("Should return intersection list", intersections);
        assertEquals("Should have 100 X planes", 100, intersections.size());
    }

    @Test
    public void testRayIntersect_emptySource_diagonal() {
        // Diagonal ray through the source
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 100, "CubicSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        RealPoint origin = new RealPoint(-10.0, -10.0, -10.0);
        RealPoint direction = new RealPoint(1.0, 1.0, 1.0);
        SourceAndConverterHelper.normalize3(direction);

        List<Double> intersections = SourceAndConverterHelper.rayIntersect(sac, 0, origin, direction);

        assertNotNull("Should return intersection list", intersections);
        assertFalse("Should have intersections", intersections.isEmpty());
    }

    @Test
    public void testRayIntersect_emptySource_misses() {
        // Ray that misses the source entirely (parallel to Z, but outside XY bounds)
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        // Ray parallel to Z axis but starting far outside the XY bounds
        RealPoint origin = new RealPoint(500.0, 500.0, -10.0);
        RealPoint direction = new RealPoint(0.0, 0.0, 1.0);

        List<Double> intersections = SourceAndConverterHelper.rayIntersect(sac, 0, origin, direction);

        assertTrue("Should have no intersections when ray misses", intersections.isEmpty());
    }

    @Test
    public void testRayIntersect_emptySource_scaled() {
        // Ray through a scaled source
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.scale(2.0);
        EmptySource source = createTestSource(100, 100, 50, "ScaledSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        // Ray along Z starting at center of scaled source (center is at 99, 99, 49)
        RealPoint origin = new RealPoint(100.0, 100.0, -10.0);
        RealPoint direction = new RealPoint(0.0, 0.0, 1.0);

        List<Double> intersections = SourceAndConverterHelper.rayIntersect(sac, 0, origin, direction);

        assertNotNull("Should return intersection list", intersections);
        assertEquals("Should have 50 Z planes", 50, intersections.size());
    }

    @Test
    public void testRayIntersect_emptySource_translated() {
        // Ray through a translated source
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.translate(100.0, 100.0, 100.0);
        EmptySource source = createTestSource(50, 50, 50, "TranslatedSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        // Ray along Z through the translated source center
        RealPoint origin = new RealPoint(125.0, 125.0, 50.0);
        RealPoint direction = new RealPoint(0.0, 0.0, 1.0);

        List<Double> intersections = SourceAndConverterHelper.rayIntersect(sac, 0, origin, direction);

        assertNotNull("Should return intersection list", intersections);
        assertEquals("Should have 50 Z planes", 50, intersections.size());
    }

    @Test
    public void testRayIntersect_transformedSource() {
        // Test with TransformedSource wrapping an EmptySource
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource baseSource = createTestSource(100, 100, 50, "BaseSource", transform);
        TransformedSource<UnsignedShortType> transformedSource = new TransformedSource<>(baseSource);

        // Apply a translation
        AffineTransform3D fixedTransform = new AffineTransform3D();
        fixedTransform.translate(50, 50, 0);
        transformedSource.setFixedTransform(fixedTransform);

        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(transformedSource,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        // Ray along Z through the transformed source
        RealPoint origin = new RealPoint(100.0, 100.0, -10.0);
        RealPoint direction = new RealPoint(0.0, 0.0, 1.0);

        List<Double> intersections = SourceAndConverterHelper.rayIntersect(sac, 0, origin, direction);

        assertNotNull("Should return intersection list", intersections);
        assertEquals("Should have 50 Z planes", 50, intersections.size());
    }

    // ==================== Converter Creation Tests ====================

    @Test
    public void testCreateConverterRealType() {
        UnsignedShortType type = new UnsignedShortType();

        Converter<UnsignedShortType, ARGBType> converter =
            SourceAndConverterHelper.createConverterRealType(type);

        assertNotNull("Converter should be created", converter);
        assertTrue("Converter should be RealARGBColorConverter",
            converter instanceof RealARGBColorConverter);
    }

    @Test
    public void testCreateConverterRealType_convertsValue() {
        UnsignedShortType type = new UnsignedShortType();
        type.set(32767);

        Converter<UnsignedShortType, ARGBType> converter =
            SourceAndConverterHelper.createConverterRealType(type);

        ARGBType output = new ARGBType();
        converter.convert(type, output);

        // The output should be a non-zero ARGB value (since input is mid-range)
        assertNotEquals("Converter should produce non-zero output", 0, output.get());
    }

    // ==================== Interval Position Tests ====================

    @Test
    public void testIsPositionWithinSourceInterval_inside() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        RealPoint position = new RealPoint(50.0, 50.0, 25.0);

        boolean inside = SourceAndConverterHelper.isPositionWithinSourceInterval(
            sac, position, 0, false);

        assertTrue("Position should be inside the source interval", inside);
    }

    @Test
    public void testIsPositionWithinSourceInterval_outside() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        RealPoint position = new RealPoint(150.0, 150.0, 75.0);

        boolean inside = SourceAndConverterHelper.isPositionWithinSourceInterval(
            sac, position, 0, false);

        assertFalse("Position should be outside the source interval", inside);
    }

    @Test
    public void testIsPositionWithinSourceInterval_2d() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 1, "2DSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        // Position inside XY but with arbitrary Z (should be ignored in 2D mode)
        RealPoint position = new RealPoint(50.0, 50.0, 1000.0);

        boolean inside = SourceAndConverterHelper.isPositionWithinSourceInterval(
            sac, position, 0, true);

        assertTrue("Position should be inside when checking 2D only", inside);
    }

    @Test
    public void testIsPositionWithinSourceInterval_atBoundary() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        // Position at the corner (0, 0, 0)
        RealPoint position = new RealPoint(0.0, 0.0, 0.0);

        boolean inside = SourceAndConverterHelper.isPositionWithinSourceInterval(
            sac, position, 0, false);

        assertTrue("Position at origin corner should be inside", inside);
    }

    // ==================== Characteristic Voxel Size with Source ====================

    @Test
    public void testGetCharacteristicVoxelSize_fromSource() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.scale(2.0);
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);

        double voxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize(source, 0, 0);

        assertEquals(2.0, voxelSize, EPSILON);
    }

    @Test
    public void testGetCharacteristicVoxelSize_fromSourceAndConverter() {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        transform.scale(3.0);
        EmptySource source = createTestSource(100, 100, 50, "TestSource", transform);
        SourceAndConverter<UnsignedShortType> sac = new SourceAndConverter<>(source,
            SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));

        double voxelSize = SourceAndConverterHelper.getCharacteristicVoxelSize(sac, 0, 0);

        assertEquals(3.0, voxelSize, EPSILON);
    }
}
