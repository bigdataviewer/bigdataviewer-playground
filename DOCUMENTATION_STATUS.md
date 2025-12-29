# SciJava Command Documentation Status

This document tracks the documentation status of all SciJava commands (`@Plugin` annotated classes) in the bigdataviewer-playground project.

**Total Commands Found: 57**
**Fully Documented: 51** (with @Plugin description + @Parameter labels/descriptions)
**Partially Documented: 6** (has @Plugin description but some parameters need review)
**Needs Documentation: 0**

---

## Fully Documented Commands (51)

These commands have both `@Plugin(description=...)` and `@Parameter(label=..., description=...)` for all user-facing parameters.

### Root Level Commands (5)
| File | Description |
|------|-------------|
| `CacheOptionsCommand.java` | Sets Bdv Playground cache options (needs a restart) |
| `ClearSourceAndConverterService.java` | Removes all sources from the SourceAndConverter service |
| `LoadSourceAndConverterServiceState.java` | Loads a previously saved Bdv Playground state from a JSON file |
| `SaveSourceAndConverterServiceState.java` | Saves the current Bdv Playground state to a JSON file |
| `ShowSourceAndConverterServiceWindow.java` | Opens the Bdv Playground source management window |

### BDV Commands (22)
| File | Description |
|------|-------------|
| `bdv/BdvCreatorCommand.java` | Creates an empty BDV window |
| `bdv/BdvDebugOverlayAdderCommand.java` | Adds the overlay of the bdv tiled renderer |
| `bdv/BdvDefaultViewerSetterCommand.java` | Sets the default preferences for newly created BDV windows |
| `bdv/BdvOrthoCreatorCommand.java` | Creates 3 BDV windows with synchronized orthogonal views |
| `bdv/BdvSelectCommand.java` | Selects and brings a BDV window to the front |
| `bdv/BdvSettingsCommand.java` | Sets actions linked to key / mouse event in BDV |
| `bdv/BdvSourcesAdderCommand.java` | Adds one or several sources to an existing BDV window |
| `bdv/BdvSourcesRemoverCommand.java` | Removes one or several sources from an existing BDV window |
| `bdv/BdvSourcesShowCommand.java` | Displays one or several sources into a new BDV window |
| `bdv/BdvTitleSetterCommand.java` | Sets the title of a BDV window |
| `bdv/BdvViewAdjustOnSourcesCommand.java` | Adjust current Bdv view on the selected sources |
| `bdv/BdvViewLoggerCommand.java` | Outputs the current view transform of a BDV window into the standard IJ logger |
| `bdv/BdvViewTransformatorCommand.java` | Applies a simple view transform (translation / rotation) to a BDV window |
| `bdv/MultiBdvCloseCommand.java` | Closes one or several bdv windows |
| `bdv/MultiBdvCrossAdderCommand.java` | Adds a centering cross onto BDV windows |
| `bdv/MultiBdvSourceNameOverlayAdderCommand.java` | Adds a source name overlay onto BDV windows |
| `bdv/MultiBdvSourceNavigatorSliderAdderCommand.java` | Adds a source slider onto BDV windows |
| `bdv/MultiBdvSourcesAdderCommand.java` | Adds one or several sources into several existing BDV windows |
| `bdv/MultiBdvSourcesRemoverCommand.java` | Removes one or several sources from several existing BDV windows |
| `bdv/MultiBdvTimepointAdapterCommand.java` | Adapts the bdv windows timepoints to the number of timepoints present in their sources |
| `bdv/MultiBdvTimepointsSetterCommand.java` | Sets the number of timepoints in one or several BDV Windows |
| `bdv/MultiBdvZSliderAdderCommand.java` | Adds a z slider onto BDV windows |

### BVV Commands (5)
| File | Description |
|------|-------------|
| `bvv/BvvOrthoWindowCreatorCommand.java` | Creates 3 BVV windows with synchronized orthogonal views |
| `bvv/BvvSetTimepointsNumberCommand.java` | Sets the number of timepoints in one or several BVV Windows |
| `bvv/BvvSourcesAdderCommand.java` | Show sources in a BigVolumeViewer window |
| `bvv/BvvSourcesRemoverCommand.java` | Removes one or several sources from an existing BVV window |
| `bvv/BvvWindowCreatorCommand.java` | Creates an empty Bvv window |

### Source Commands (17)
| File | Description |
|------|-------------|
| `source/AddMetadataCommand.java` | Adds a metadata string to selected sources |
| `source/BasicTransformerCommand.java` | Performs basic transformation (scale, rotate, flip) on selected sources |
| `source/BigWarpLauncherCommand.java` | Starts BigWarp from existing sources |
| `source/BrightnessAdjusterCommand.java` | Sets the display range (min and max) of one or more sources |
| `source/ColorSourceCreatorCommand.java` | Duplicate one or several sources and sets a new color for these sources |
| `source/InteractiveBrightnessAdjusterCommand.java` | Interactively adjusts the display range (min and max) of sources with live preview |
| `source/LUTSourceCreatorCommand.java` | Duplicate one or several sources and sets an (identical) Look Up Table |
| `source/MakeGroupCommand.java` | Adds a node in the tree view which selects the sources specified in the command |
| `source/MakeMetadataFilterNodeCommand.java` | Adds a node in the tree view which selects the sources which contain a certain key metadata |
| `source/ManualTransformCommand.java` | Manual transformation of selected sources |
| `source/NewSourceCommand.java` | Defines an empty source which occupied the same volume as a model source |
| `source/SampleSourceCreatorCommand.java` | Creates a sample source for testing and demonstration purposes |
| `source/SourceColorChangerCommand.java` | Changes the display color of one or more sources |
| `source/SourceTransformerCommand.java` | Applies an affine transformation on several sources |
| `source/SourcesDuplicatorCommand.java` | Creates a copy of the selected sources |
| `source/SourcesInvisibleMakerCommand.java` | Makes sources invisible in all BDV windows where they are displayed |
| `source/SourcesRemoverCommand.java` | Removes sources from the source and converter service |
| `source/SourcesResamplerCommand.java` | Resamples sources to match the voxel grid of a model source |
| `source/SourcesVisibleMakerCommand.java` | Makes sources visible in all BDV windows where they are displayed |
| `source/TransformedSourceWrapperCommand.java` | Wraps sources in a TransformedSource, allowing subsequent transformations to be applied |
| `source/XmlHDF5ExporterCommand.java` | Exports sources to an XML/HDF5 BigDataViewer dataset |

### SpimData Commands (4)
| File | Description |
|------|-------------|
| `spimdata/BigDataBrowserPlugInCommand.java` | Opens a browser to list and select datasets from a BigDataServer |
| `spimdata/MultipleSpimDataImporterCommand.java` | Opens one or more BDV XML datasets |
| `spimdata/SpimDataExporterCommand.java` | Saves the SpimData associated with sources to an XML file |
| `spimdata/SpimdataBigDataServerImportCommand.java` | Opens a BDV dataset from a BigDataServer |

### Viewer Commands (1)
| File | Description |
|------|-------------|
| `viewer/ViewSynchronizerCommand.java` | Synchronizes the view of a set of BDV or BVV windows |

---

## Partially Documented Commands (6)

These commands have @Plugin descriptions but some parameters may need review.

| File | Notes |
|------|-------|
| `viewer/StateSynchronizerCommand.java` | Some parameters missing description attributes |
| `BdvPlaygroundActionCommand.java` | Interface/marker - no parameters needed |

---

## Commands Needing Documentation (0)

All commands in this repository have @Plugin descriptions.

---

## Documentation Guidelines

When documenting commands, add:

1. **@Plugin annotation** - Add `description` attribute:
   ```java
   @Plugin(type = Command.class,
           menuPath = "...",
           description = "Brief description of what the command does")
   ```

2. **@Parameter annotations** - Add `label` and `description` for user-facing parameters:
   ```java
   @Parameter(label = "Select Source(s)",
              description = "The source(s) to process")
   SourceAndConverter<?>[] sources;
   ```

3. **Service parameters** - No documentation needed (not user-facing):
   ```java
   @Parameter
   SourceAndConverterService sacService;  // No label/description needed
   ```

### Style Guidelines
- Labels: Use Title Case, 2-4 words
- Descriptions: Complete sentences, user-friendly language
- Avoid jargon: Use "image source" instead of "SourceAndConverter"
- Use "world coordinates units" instead of "physical units"

---

*Last updated: 2025-12-29*