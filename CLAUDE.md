# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BigDataViewer-Playground (bdv-playground) is a Java library that extends BigDataViewer (BDV) with actions and GUI components for managing and displaying multi-dimensional image sources in Fiji/ImageJ2. It provides a centralized service architecture for handling `SourceAndConverter` objects and their display across multiple BDV windows.

## Build Commands

```bash
# Build the project
mvn clean install

# Build without running tests
mvn clean install -DskipTests

# Run a specific test class
mvn test -Dtest=sc.fiji.bdvpg.SimpleIJLaunch

# Run tests in a specific package
mvn test -Dtest="sc.fiji.bdvpg.tests.*"
```

The project uses Java 8 and inherits from `pom-scijava` parent POM (version 43.0.0).

## Architecture

### Core Services (SciJava Plugin Services)

The library is built around two main SciJava services that work together:

1. **SourceAndConverterService** (`sc.fiji.bdvpg.scijava.services.SourceAndConverterService`)
   - Central registry for all `SourceAndConverter` objects
   - Manages metadata associated with sources (stored in weak-key caches)
   - Handles SpimData registration and source creation from HDF5/N5 datasets (or other backends coming from other libraries)
   - Provides action registration system for contextual menus
   - Manages a global cache for image data across all datasets

2. **SourceAndConverterBdvDisplayService** (`sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService`)
   - Manages display of sources in BDV windows
   - Handles multiple simultaneous BdvHandle instances
   - Tracks source visibility across different viewers
   - Provides BdvHandle supplier mechanism for customizing window creation

### Static Access Pattern

`SourceAndConverterServices` provides static access to both services when SciJava context injection is not available.

### Key Design Patterns

- **SourceAndConverter wrapper**: All BDV sources are wrapped with their converters for unified handling
- **Metadata via weak-key caches**: Source metadata is stored using Guava caches with weak keys to allow garbage collection
- **Action registration**: SciJava commands implementing `BdvPlaygroundActionCommand` are auto-registered as contextual menu actions
- **EntityHandler extension**: Custom SpimData entity types can be supported via `EntityHandler` plugins

### Package Structure

- `sc.fiji.bdvpg.scijava.command.*` - SciJava commands exposed in Fiji menus
- `sc.fiji.bdvpg.scijava.services.*` - Core SciJava services
- `sc.fiji.bdvpg.sourceandconverter` - Source manipulation utilities
- `sc.fiji.bdvpg.bdv.*` - BDV-specific helpers and actions
- `sc.fiji.bdvpg.cache` - Global caching infrastructure
- `bdv.util.*` - BDV utility classes (ResampledSource, EmptySource, etc.)
- `net.imglib2.realtransform.*` - Transform adapters for serialization

### Source Hierarchy

Sources can be wrapped/transformed in chains. `SourceAndConverterHelper.getRootSource()` traverses:
- `WarpedSource` -> wrapped source
- `TransformedSource` -> wrapped source (with affine transform)
- `ResampledSource` -> model resampler source

### Serialization

The project uses Gson with custom adapters for serializing:
- Sources and converters
- Real transforms (affine, thin-plate spline, sequences)
- SpimData references
- BDV window options

Adapters are automatically registered via SciJava plugins and obtained via `ScijavaGsonHelper.getGson(context)`. See [ADAPTER_TESTING_STATUS.md](ADAPTER_TESTING_STATUS.md) for a complete list of all serialization adapters and their test coverage.

## Testing

Test files are in `src/test/src/sc/fiji/bdvpg/` (non-standard path from pom-scijava).

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelperTest

# Run adapter serialization tests
mvn test -Dtest="sc.fiji.bdvpg.tests.adapters.*"
```

See [ADAPTER_TESTING_STATUS.md](ADAPTER_TESTING_STATUS.md) for the complete list of serialization adapters and their test coverage.

### Test Categories

1. **Unit tests** (no GUI required):
   - `SourceAndConverterHelperTest` - Tests for math utilities (vectors, voxel sizes, ray intersection)
   - `TransformSerializationTests` - Tests for transform adapter serialization/deserialization
   - `SourceSerializationTests` - Tests for source adapter serialization/deserialization

2. **Integration tests** (require ImageJ context but no GUI):
   - Tests using `TestHelper.startFiji(ij)` without showing UI

3. **Interactive demos** (require GUI, typically run manually):
   - Most files ending in `Demo.java` launch ImageJ with UI

## Key Dependencies

- `bigdataviewer-core`, `bigdataviewer-vistools` - Core BDV libraries
- `bigvolumeviewer` - 3D volume rendering
- `bigwarp_fiji` - Landmark-based registration
- `imglib2`, `imglib2-realtransform` - Image processing
- `scijava-ui-swing`, `ui-behaviour` - GUI and interaction
- `bigdataviewer-selector` - Source selection widget
