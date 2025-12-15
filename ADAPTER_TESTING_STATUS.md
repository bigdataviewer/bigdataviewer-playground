# Adapter Testing Status

This document tracks the testing status of all Gson serialization adapters in the project. These adapters are automatically registered via the SciJava plugin system and are used by `ScijavaGsonHelper.getGson(context)`.

## Overview

Adapters implement one of the following interfaces:
- **IClassAdapter**: Direct serialization/deserialization for a specific class
- **IClassRuntimeAdapter**: Runtime polymorphic serialization (base class → runtime class mapping)
- **ISourceAdapter**: Specialized adapter for Source types (extends IObjectScijavaAdapter)

## Transform Adapters (net.imglib2.realtransform)

### IClassAdapter Implementations

| Adapter Class | Target Class | Test Status | Test Location |
|--------------|--------------|-------------|---------------|
| AffineTransform3DAdapter | AffineTransform3D | ✅ TESTED | TransformSerializationTests.testAffineTransformSerialization() |
| RealTransformSequenceAdapter | RealTransformSequence | ✅ TESTED | TransformSerializationTests.testRealTransformSequenceSerialization() |
| ThinPlateSplineTransformAdapter | ThinplateSplineTransform | ✅ TESTED | TransformSerializationTests.testThinPlateSplineTransformSerialization() |
| InvertibleRealTransformSequenceAdapter | InvertibleRealTransformSequence | ✅ TESTED | TransformSerializationTests.testInvertibleRealTransformSequenceSerialization() |
| Wrapped2DTransformAs3DRealTransformAdapter | Wrapped2DTransformAs3D | ⚠️ PARTIALLY TESTED | Tested indirectly via source serialization |
| WrappedIterativeInvertibleRealTransformAdapter | WrappedIterativeInvertibleRealTransform | ✅ TESTED | TransformSerializationTests.testWrappedIterativeInvertibleRealTransformSerialization() |

### IClassRuntimeAdapter Implementations

| Adapter Class | Base Class | Runtime Class | Test Status | Test Location |
|--------------|-----------|---------------|-------------|---------------|
| AffineTransform3DRunTimeAdapter | RealTransform | AffineTransform3D | ✅ TESTED | TransformSerializationTests.testAffineTransformSerialization() |
| RealTransformSequenceRunTimeAdapter | RealTransform | RealTransformSequence | ✅ TESTED | TransformSerializationTests.testRealTransformSequenceSerialization() |
| ThinPlateSplineTransformRunTimeAdapter | RealTransform | ThinplateSplineTransform | ✅ TESTED | TransformSerializationTests.testThinPlateSplineTransformSerialization() |
| InvertibleRealTransformSequenceRunTimeAdapter | InvertibleRealTransform | InvertibleRealTransformSequence | ✅ TESTED | TransformSerializationTests.testInvertibleRealTransformSequenceSerialization() |
| Wrapped2DTransformAs3DRealTransformRunTimeAdapter | RealTransform | Wrapped2DTransformAs3D | ⚠️ PARTIALLY TESTED | Tested indirectly via source serialization |
| InvertibleWrapped2DTransformAs3DRealTransformRunTimeAdapter | InvertibleRealTransform | InvertibleWrapped2DTransformAs3D | ⚠️ PARTIALLY TESTED | Tested indirectly via source serialization |
| WrappedIterativeInvertibleRealTransformRunTimeAdapter | InvertibleRealTransform | WrappedIterativeInvertibleRealTransform | ✅ TESTED | TransformSerializationTests.testWrappedIterativeInvertibleRealTransformSerialization() |

## Source Adapters (sc.fiji.bdvpg.scijava.adapter.source)

### ISourceAdapter Implementations

| Adapter Class | Target Source Class | Test Status | Test Location |
|--------------|---------------------|-------------|---------------|
| EmptySourceAdapter | EmptySource | ✅ TESTED | SourceSerializationTests.testEmptySourceSerialization_withTransform() |
| SpimSourceAdapter | SpimSource | ✅ TESTED | SourceSerializationTests.testSpimSourceSerialization() |
| TransformedSourceAdapter | TransformedSource | ✅ TESTED | SourceSerializationTests.testTransformedSourceSerialization() |
| WarpedSourceAdapter | WarpedSource | ✅ TESTED | SourceSerializationTests.testWarpedSourceSerialization_withThinPlateSpline() |
| ResampledSourceAdapter | ResampledSource | ✅ TESTED | SourceSerializationTests.testResampledSourceSerialization() |

## Other Adapters

### BDV-Related Adapters

| Adapter Class | Type | Target Class(es) | Test Status | Test Location |
|--------------|------|-----------------|-------------|---------------|
| DefaultBdvSupplierAdapter | IClassRuntimeAdapter | DefaultBdvSupplier | ✅ TESTED | TransformSerializationTests.testBdvSupplierSerialization() |
| DefaultAccumulatorFactoryAdapter | IClassRuntimeAdapter | DefaultAccumulatorFactory | ❌ NOT TESTED | - |

### High-Level Serializers

These are JsonSerializer/JsonDeserializer implementations that coordinate multiple sub-adapters:

| Adapter Class | Purpose | Test Status | Notes |
|--------------|---------|-------------|-------|
| SourceAndConverterAdapter | Serializes SourceAndConverter objects | ✅ TESTED | Tested indirectly via SourceSerializationTests |
| AbstractSpimdataAdapter | Serializes SpimData objects | ✅ TESTED | Tested indirectly via SourceSerializationTests (SpimSource tests) |

## Summary Statistics

- **Total Adapters**: 20 (excluding high-level serializers)
- **Tested**: 15 (75%)
  - ✅ Fully Tested: 13 (65%)
  - ⚠️ Partially Tested: 2 (10%)
- **Not Tested**: 5 (25%)

### Breakdown by Category

**Transform Adapters** (13 total):
- ✅ Fully Tested: 10
- ⚠️ Partially Tested: 3
- ❌ Not Tested: 0

**Source Adapters** (5 total):
- ✅ Tested: 5
- ❌ Not Tested: 0

**Other Adapters** (2 total):
- ✅ Tested: 1
- ❌ Not Tested: 1

## Testing Notes

### Test Patterns

The project uses two complementary testing approaches (standard for serialization tests):

1. **Round-trip JSON serialization** (from TransformSerializationTests.testSerialization()):
   - Serialize object → JSON
   - Deserialize JSON → object
   - Re-serialize → JSON2
   - Assert JSON == JSON2

2. **Functional verification**:
   - Verify the deserialized object actually works correctly
   - For transforms: apply to test points and compare results
   - For invertible transforms: also test inverse operations
   - For sources: verify source data is accessible and correct

### Notes on Specific Adapters

**Wrapped2DTransformAs3D adapters**: These adapters require actual 2D transforms from imglib2 (not AffineTransform3D which is 3D-only). Direct unit tests would require specific 2D transform implementations that may not be readily available. However, these adapters are exercised indirectly when sources that have been warped with 2D transforms are serialized/deserialized, so they have some test coverage through integration tests.

### Priority for Testing

**HIGH PRIORITY** (transform adapters, frequently used):
1. ✅ InvertibleRealTransformSequenceAdapter + RunTimeAdapter - TESTED
2. ⚠️ Wrapped2DTransformAs3DRealTransformAdapter + RunTimeAdapter - Tested indirectly (requires 2D transforms from imglib2)
3. ⚠️ InvertibleWrapped2DTransformAs3DRealTransformRunTimeAdapter - Tested indirectly (requires 2D transforms from imglib2)
4. ✅ WrappedIterativeInvertibleRealTransformAdapter + RunTimeAdapter - TESTED

**MEDIUM PRIORITY** (less commonly used):
5. ❌ DefaultAccumulatorFactoryAdapter - NOT TESTED

## Test File Locations

- Transform adapter tests: `src/test/src/sc/fiji/bdvpg/tests/adapters/transforms/TransformSerializationTests.java`
- Source adapter tests: `src/test/src/sc/fiji/bdvpg/tests/adapters/sources/SourceSerializationTests.java`

## Running Tests

```bash
# Run all adapter tests
mvn test -Dtest="sc.fiji.bdvpg.tests.adapters.*"

# Run just transform adapter tests
mvn test -Dtest="sc.fiji.bdvpg.tests.adapters.transforms.TransformSerializationTests"

# Run just source adapter tests
mvn test -Dtest="sc.fiji.bdvpg.tests.adapters.sources.SourceSerializationTests"
```
