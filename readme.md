## BDV playground / toolbox / actions

[![](https://github.com/bigdataviewer/bigdataviewer-playground/actions/workflows/build-main.yml/badge.svg)](https://github.com/bigdataviewer/bigdataviewer-playground/actions/workflows/build-main.yml)
[![Maven Scijava Version](https://img.shields.io/github/v/tag/bigdataviewer/bigdataviewer-playground?label=Version-[Maven%20Scijava])](https://maven.scijava.org/#browse/browse:releases:sc%2Ffiji%2Fbigdataviewer-playground)

In this repository, we collect useful additions for the [BigDataViewer](https://imagej.net/BigDataViewer) in [Fiji](https://fiji.sc). These functionalities are accessible by enabling the `PTBIOP` Fiji update site, accessible via `Help>Update>Manage Update Sites`.

User documentation is located can be found at [https://imagej.github.io/plugins/bdv/playground/bdv-playground](https://imagej.github.io/plugins/bdv/playground/bdv-playground)

## Coding guides
We tried to follow these general guidelines:
* Specific (atomic) functionality lives in a class implementing [Runnable](https://docs.oracle.com/javase/7/docs/api/java/lang/Runnable.html)
  * More mandatory parameters for the functionality are handed over as additional constructor parameters.
  * Optional parameters are set with setter methods.
  * The `run()` method executes the concrete action.
  * Results are retrieved using getter methods. The getter methods may internally run the `run()` method if necessary.

Pseudo-code example:
```
  Class Action {
    Action(BdvHandle)
    setParameter(Parameter)
    run()
    getResult()
  }
```

* Furthermore, [Demo code](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/test/src/sc/fiji/bdvpg) in the test sources directory should demonstrate the individual functionality, and how to be called programmatically.
  
* Higher level functions, which may execute several actions sequentially, are implemented as [SciJava Commands](https://javadoc.scijava.org/SciJava/org/scijava/command/Command.html).
This enables additional user access.

## List of actions
* Read a source from disc
* Add a source to a BDV
* [Log the current mouse position](https://github.com/haesleinhuepf/bigdataviewer-playground/blob/master/src/test/src/sc/fiji/bdv/navigate/LogMousePositionDemo.java#L33)
* [Log the current viewer transform](https://github.com/haesleinhuepf/bigdataviewer-playground/blob/master/src/test/src/sc/fiji/bdv/navigate/ViewTransformSetAndLogDemo.java#L35)
* [Change the current viewer transform](https://github.com/haesleinhuepf/bigdataviewer-playground/blob/master/src/test/src/sc/fiji/bdv/navigate/ViewTransformSetAndLogDemo.java#L37-L40)
* [Take a screenshot](https://github.com/haesleinhuepf/bigdataviewer-playground/blob/master/src/test/src/sc/fiji/bdv/screenshot/ScreenshotDemo.java)

## List of SciJava Commands 

<details>

### [CacheOptionsCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/CacheOptionsCommand.java) [Plugins>BigDataViewer-Playground>Set cache options]
Sets Bdv Playground cache options (needs a restart)
#### Input
* [Button] **button**:Reset to default
* [String] **cache_type**:Cache type
* [int] **log_ms**:Log cache (ms between log), negative to avoid logging
* [int] **mem_for_cache_mb**:Rule: set a size for cache (Mb)
* [int] **mem_for_everything_else_mb**:Rule: set a size for the rest of the application (Mb)
* [int] **mem_ratio_pc**:Rule: use a ratio of all memory available (%)
* [PrefService] **prefs**:


### [ClearSourceAndConverterService](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/ClearSourceAndConverterService.java) [Plugins>BigDataViewer-Playground>Clear Bdv Playground State]
#### Input
* [SourceAndConverterService] **sac_service**:


### [LoadSourceAndConverterServiceState](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/LoadSourceAndConverterServiceState.java) [Plugins>BigDataViewer-Playground>Load Bdv Playground State (experimental)]
#### Input
* [Context] **ctx**:
* [Boolean] **erasepreviousstate**:Erase current state
* [File] **file**:Open state file (json)


### [SaveSourceAndConverterServiceState](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/SaveSourceAndConverterServiceState.java) [Plugins>BigDataViewer-Playground>Save Bdv Playground State (experimental)]
#### Input
* [Context] **ctx**:
* [File] **file**:Save state file (json)


### [ShowSourceAndConverterServiceWindow](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/ShowSourceAndConverterServiceWindow.java) [Plugins>BigDataViewer-Playground>Show Bdv Playground Window]
#### Input
* [SourceAndConverterService] **sacs**:


### [BdvCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvCreatorCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Create empty BDV window]
Creates an empty BDV window
#### Input
* [SourceAndConverterBdvDisplayService] **sacDisplayService**:
#### Output
* [BdvHandle] **bdvh**:


### [BdvDebugOverlayAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvDebugOverlayAdderCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Add debug overlay]
Adds the overlay of the bdv tiled renderer
#### Input
* [BdvHandle] **bdvh**:


### [BdvDefaultViewerSetterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvDefaultViewerSetterCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Set BDV window (default)]
Set preferences of Bdv Window
#### Input
* [String] **frametitle**:
* [int] **height**:
* [boolean] **interpolate**:
* [boolean] **is2d**:
* [int] **numrenderingthreads**:
* [int] **numsourcegroups**:
* [int] **numtimepoints**:
* [boolean] **resetToDefault**:Click this checkbox to ignore all parameters and reset the default viewer
* [SourceAndConverterBdvDisplayService] **sacDisplayService**:
* [String] **screenscales**:
* [long] **targetrenderms**:
* [int] **width**:


### [BdvOrthoCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvOrthoCreatorCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Create Orthogonal Views]
Creates 3 BDV windows with synchronized orthogonal views
#### Input
* [boolean] **drawcrosses**:Add cross overlay to show view plane locations
* [boolean] **interpolate**:Interpolate
* [int] **locationx**:X Front Window location
* [int] **locationy**:Y Front Window location
* [int] **ntimepoints**:Number of timepoints (1 for a single timepoint)
* [SourceAndConverterBdvDisplayService] **sacDisplayService**:
* [int] **screen**:Display (0 if you have one screen)
* [int] **sizex**:Window Width
* [int] **sizey**:Window Height
* [boolean] **synchronize_sources**:
#### Output
* [BdvHandle] **bdvhx**:
* [BdvHandle] **bdvhy**:
* [BdvHandle] **bdvhz**:


### [BdvSelectCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSelectCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Select Window]
Select a BDV Windows
#### Input
* [BdvHandle] **bdvh**:Select BDV Window


### [BdvSettingsCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSettingsCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Preferences - Set (Key) Bindings]
Sets actions linked to key / mouse event in BDV
#### Input
* [Context] **context**:


### [BdvSourcesAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSourcesAdderCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Show Sources]
Adds one or several sources to an existing BDV window
#### Input
* [boolean] **adjustviewonsource**:Adjust View on Source
* [boolean] **autocontrast**:Auto Contrast
* [SourceAndConverterBdvDisplayService] **bdvDisplayService**:
* [BdvHandle] **bdvh**:Select BDV Window
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [BdvSourcesRemoverCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSourcesRemoverCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Remove Sources From BDV]
Removes one or several sources from an existing BDV window
#### Input
* [SourceAndConverterBdvDisplayService] **bdvDisplayService**:
* [BdvHandle] **bdvh**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [BdvSourcesShowCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSourcesShowCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Show Sources (new Bdv window)]
Displays one or several sources into a new BDV window
#### Input
* [boolean] **adjustviewonsource**:Adjust View on Source
* [boolean] **autocontrast**:Auto Contrast
* [SourceAndConverterBdvDisplayService] **bdvDisplayService**:
* [boolean] **interpolate**:Interpolate
* [SourceAndConverter[]] **sacs**:Select Source(s)
#### Output
* [BdvHandle] **bdvh**:


### [BdvTitleSetterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvTitleSetterCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Set Title]
Sets the title of a BDV Windows
#### Input
* [BdvHandle] **bdvh**:Select BDV Window
* [String] **title**:title


### [BdvViewAdjustOnSourcesCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvViewAdjustOnSourcesCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Adjust view on sources]
Adjust current Bdv view on the selected sources
#### Input
* [BdvHandle] **bdvh**:Select BDV Window
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [BdvViewLoggerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvViewLoggerCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Log view transform]
Outputs the current view transform of a BDV window into the standard IJ logger
#### Input
* [BdvHandle] **bdvh**:
* [LogService] **ls**:


### [BdvViewTransformatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvViewTransformatorCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Change view transform]
Applies a simple view transform (translation / rotation) to a BDV window
#### Input
* [BdvHandle] **bdvh**:Select BDV Windows
* [Double] **rotatearoundx**:Rotate around X
* [Double] **rotatearoundy**:Rotate around Y
* [Double] **rotatearoundz**:Rotate around Z
* [Double] **translatex**:Translate in X
* [Double] **translatey**:Translate in Y
* [Double] **translatez**:Translate in Z


### [MultiBdvCrossAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/MultiBdvCrossAdderCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Add center cross]
Adds a centering cross onto BDV windows
#### Input
* [BdvHandle[]] **bdvhs**:Select BDV Windows


### [MultiBdvSourceNameOverlayAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/MultiBdvSourceNameOverlayAdderCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Add Sources Name Overlay]
Adds a source name overlay onto BDV windows
#### Input
* [BdvHandle[]] **bdvhs**:Select BDV Windows
* [int] **fontSize**:Font Size
* [String] **fontString**:


### [MultiBdvSourceNavigatorSliderAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/MultiBdvSourceNavigatorSliderAdderCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Add Source Slider]
Adds a source slider onto BDV windows
#### Input
* [BdvHandle[]] **bdvhs**:Select BDV Windows


### [MultiBdvSourcesAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/MultiBdvSourcesAdderCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Show Sources In Multiple BDV Windows]
Adds one or several sources into several existing BDV windows
#### Input
* [SourceAndConverterBdvDisplayService] **bdvDisplayService**:
* [BdvHandle[]] **bdvhs**:Select BDV Windows
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [MultiBdvSourcesRemoverCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/MultiBdvSourcesRemoverCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Remove Sources In Multiple BDV Windows]
Removes one or several sources from several existing BDV windows
#### Input
* [SourceAndConverterBdvDisplayService] **bdvDisplayService**:
* [BdvHandle[]] **bdvhs**:Select BDV Windows
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [MultiBdvTimepointAdapterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/MultiBdvTimepointAdapterCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Adapt bdv number of timepoints to sources]
Adapts the bdv windows timepoints to the number of timepoints present in their sources.
#### Input
* [SourceAndConverterBdvDisplayService] **bdvDisplayService**:
* [BdvHandle[]] **bdvhs**:Select BDV Windows


### [MultiBdvTimepointsSetterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/MultiBdvTimepointsSetterCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Set Number Of Timepoints]
Sets the number of timepoints in one or several BDV Windows
#### Input
* [BdvHandle[]] **bdvhs**:Select BDV Windows
* [int] **numberoftimepoints**:Number of timepoints, min = 1


### [MultiBdvZSliderAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/MultiBdvZSliderAdderCommand.java) [Plugins>BigDataViewer-Playground>BDV>BDV - Add Z Slider]
Adds a z slider onto BDV windows
#### Input
* [BdvHandle[]] **bdvhs**:Select BDV Windows


### [BvvOrthoWindowCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bvv/BvvOrthoWindowCreatorCommand.java) [Plugins>BigDataViewer-Playground>BVV>BVV - Create Orthogonal Views]
Creates 3 BVV windows with synchronized orthogonal views
#### Input
* [boolean] **interpolate**:Interpolate
* [int] **locationx**:X Front Window location
* [int] **locationy**:Y Front Window location
* [int] **ntimepoints**:Number of timepoints (1 for a single timepoint)
* [int] **screen**:Display (0 if you have one screen)
* [int] **sizex**:Window Width
* [int] **sizey**:Window Height
* [boolean] **synchronize_sources**:
#### Output
* [BvvHandle] **bvvhx**:
* [BvvHandle] **bvvhy**:
* [BvvHandle] **bvvhz**:


### [BvvSetTimepointsNumberCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bvv/BvvSetTimepointsNumberCommand.java) [Plugins>BigDataViewer-Playground>BVV>BVV - Set Number Of Timepoints]
Sets the number of timepoints in one or several BVV Windows
#### Input
* [BvvHandle[]] **bvvhs**:Select BVV Windows
* [int] **numberoftimepoints**:Number of timepoints, min = 1


### [BvvSourcesAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bvv/BvvSourcesAdderCommand.java) [Plugins>BigDataViewer-Playground>BVV>BVV - Show Sources]
Show sources in a BigVolumeViewer window - limited to 16 bit images
#### Input
* [boolean] **adjustviewonsource**:Adjust View on Source
* [BvvHandle] **bvvh**:Select BVV Window(s)
* [SourceAndConverter[]] **sacs**:Select source(s)


### [BvvSourcesRemoverCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bvv/BvvSourcesRemoverCommand.java) [Plugins>BigDataViewer-Playground>BVV>BVV - Remove Sources From BVV]
Removes one or several sources from an existing BVV window
#### Input
* [BvvHandle] **bvvh**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [BvvWindowCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bvv/BvvWindowCreatorCommand.java) [Plugins>BigDataViewer-Playground>BVV>BVV - Create Empty BVV Frame]
Creates an empty Bvv window
#### Input
* [String] **windowtitle**:Title of the new BVV window
#### Output
* [BvvHandle] **bvvh**:


### [AddMetadataCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/AddMetadataCommand.java) [Plugins>BigDataViewer-Playground>Sources>Add Metadata To Sources]
Adds a metadata string to selected sources
#### Input
* [String] **key**:Key
* [SourceAndConverterService] **sac_service**:
* [SourceAndConverter[]] **sacs**:Select Source(s)
* [String] **value**:Value


### [BasicTransformerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/BasicTransformerCommand.java) [Plugins>BigDataViewer-Playground>Sources>Transform>Basic Transformation]
Performs basic transformation (rotate / flip) along X Y Z axis for several sources. If global is selected, the transformation is performed relative to the global origin (0,0,0). If global is not selected, the center of each source is unchanged.
#### Input
* [String] **axis**:
* [SourceAndConverterBdvDisplayService] **bdvDisplayService**:
* [boolean] **globalchange**:Global transform (relative to the origin of the world)
* [int] **initimepoint**:Initial timepoint (0 based)
* [int] **ntimepoints**:Number of timepoints (min 1)
* [SourceAndConverter[]] **sacs**:Select source(s)
* [String] **type**:


### [BigWarpLauncherCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/BigWarpLauncherCommand.java) [Plugins>BigDataViewer-Playground>Sources>Register>Launch BigWarp]
Starts BigWarp from existing sources
#### Input
* [String] **bigwarpname**:Window title for BigWarp
* [SourceAndConverterBdvDisplayService] **bsds**:
* [SourceAndConverter[]] **fixedsources**:Fixed Source(s)
* [SourceAndConverter[]] **movingsources**:Moving Source(s)
* [SourceAndConverterService] **sac_service**:
#### Output
* [BdvHandle] **bdvhp**:
* [BdvHandle] **bdvhq**:
* [SourceAndConverter] **gridsource**:
* [SourceAndConverter[]] **warpedsources**:
* [SourceAndConverter] **warpmagnitudesource**:


### [BrightnessAdjusterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/BrightnessAdjusterCommand.java) [Plugins>BigDataViewer-Playground>Sources>Display>Set Sources Brightness]
#### Input
* [double] **max**:
* [double] **min**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [ColorSourceCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/ColorSourceCreatorCommand.java) [Plugins>BigDataViewer-Playground>Sources>Display>Create New Source (Set Color)]
Duplicate one or several sources and sets a new color for these sources
#### Input
* [ColorRGB] **color**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [InteractiveBrightnessAdjusterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/InteractiveBrightnessAdjusterCommand.java) [Plugins>BigDataViewer-Playground>Sources>Display>Set Sources Brightness (Interactive)]
#### Input
* [String] **customsourcelabel**:Sources :
  Label the sources controlled by this window
* [double] **max**:
* [double] **maxslider**:relative Maximum
* [String] **message**:
* [double] **min**:
* [double] **minslider**:relative Minimum
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [LUTSourceCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/LUTSourceCreatorCommand.java) [Plugins>BigDataViewer-Playground>Sources>Display>Create New Source (Set LUT)]
Duplicate one or several sources and sets an (identical) Look Up Table for these duplicated sources
#### Input
* [String] **choice**:LUT name
* [ConvertService] **cs**:
* [LUTService] **lutservice**:
* [SourceAndConverter[]] **sacs**:Select Source(s)
* [ColorTable] **table**:LUT
#### Output
* [SourceAndConverter[]] **sacs_out**:


### [MakeGroupCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/MakeGroupCommand.java) [Plugins>BigDataViewer-Playground>Sources>Make Global Source Group]
Adds a node in the tree view which selects the sources specified in the command
#### Input
* [boolean] **displaysources**:Display Sources
* [String] **groupname**:Name of the group
* [SourceAndConverterService] **sac_service**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [MakeMetadataFilterNodeCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/MakeMetadataFilterNodeCommand.java) [Plugins>BigDataViewer-Playground>Sources>Make Metadata Filter Node]
Adds a node in the tree view which selects the sources which contain a certain key metadata and which matches a certain regular expression
#### Input
* [String] **groupname**:Name of the node
* [String] **key**:Select Metadata Key
* [SourceAndConverterService] **sac_service**:
* [String] **valueregex**:Regular expression for Metadata Value (".*" matches everything)


### [ManualTransformCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/ManualTransformCommand.java) [Plugins>BigDataViewer-Playground>Sources>Transform>Manual Sources Transformation]
Manual transformation of selected sources. Works only with a single bdv window (the active one).The sources that are not displayed but selected are transformed. During the registration, the user isplaced in the reference of the moving sources. That's why they are not moving during the registration.
#### Input
* [BdvHandle] **bdvh**:
* [String] **mode**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [NewSourceCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/NewSourceCommand.java) [Plugins>BigDataViewer-Playground>Sources>New Source Based on Model Source]
Defines an empty source which occupied the same volume as a model source but with a potentially different voxel size. Works with a single timepoint.
#### Input
* [SourceAndConverter] **model**:Model Source
  Defines the portion of space covered by the new source
* [String] **name**:Source name
* [int] **timepoint**:Timepoint (0 based index)
* [double] **voxsizex**:Voxel Size X
* [double] **voxsizey**:Voxel Size Y
* [double] **voxsizez**:Voxel Size Z
#### Output
* [SourceAndConverter] **sac**:


### [SampleSourceCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SampleSourceCreatorCommand.java) [Plugins>BigDataViewer-Playground>Sources>Create Sample Source]
#### Input
* [String] **samplename**:Sample name
#### Output
* [SourceAndConverter] **sac**:


### [SourceColorChangerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourceColorChangerCommand.java) [Plugins>BigDataViewer-Playground>Sources>Display>Set Sources Color]
#### Input
* [ColorRGB] **color**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [SourceTransformerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourceTransformerCommand.java) [Plugins>BigDataViewer-Playground>Sources>Transform>Sources Affine Transformation]
Applies an affine transformation on several sources.
#### Input
* [int] **initimepoint**:Initial timepoint (0 based)
* [double] **m00**:
* [double] **m01**:
* [double] **m02**:
* [double] **m10**:
* [double] **m11**:
* [double] **m12**:
* [double] **m20**:
* [double] **m21**:
* [double] **m22**:
* [String] **matrixCsv**:Matrix as comma separated numbers
* [int] **ntimepoints**:Number of timepoints (min 1)
* [SourceAndConverter[]] **sacs**:Select source(s)
* [double] **tx**:
* [double] **ty**:
* [double] **tz**:


### [SourcesDuplicatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesDuplicatorCommand.java) [Plugins>BigDataViewer-Playground>Sources>Duplicate Sources]
#### Input
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [SourcesInvisibleMakerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesInvisibleMakerCommand.java) [Plugins>BigDataViewer-Playground>Sources>Display>Make Sources Invisible]
#### Input
* [SourceAndConverterBdvDisplayService] **bsds**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [SourcesRemoverCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesRemoverCommand.java) [Plugins>BigDataViewer-Playground>Sources>Delete Sources]
#### Input
* [SourceAndConverterService] **bss**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [SourcesResamplerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesResamplerCommand.java) [Plugins>BigDataViewer-Playground>Sources>Resample Source Based on Model Source]
#### Input
* [boolean] **cache**:
* [int] **defaultmipmaplevel**:MipMap level if not re-used (0 = max resolution)
* [boolean] **interpolate**:
* [SourceAndConverter] **model**:
* [String] **name**:Name(s) of the resampled source(s)
* [boolean] **reusemipmaps**:Re-use MipMaps
* [SourceAndConverter[]] **sacs**:Select Source(s)
#### Output
* [SourceAndConverter[]] **sacs_out**:


### [SourcesVisibleMakerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesVisibleMakerCommand.java) [Plugins>BigDataViewer-Playground>Sources>Display>Make Sources Visible]
#### Input
* [SourceAndConverterBdvDisplayService] **bsds**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [TransformedSourceWrapperCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/TransformedSourceWrapperCommand.java) [Plugins>BigDataViewer-Playground>Sources>Transform>Wrap as Transformed Source]
#### Input
* [SourceAndConverter[]] **sacs**:Select Source(s)
#### Output
* [SourceAndConverter[]] **sacs_out**:


### [XmlHDF5ExporterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/XmlHDF5ExporterCommand.java) [Plugins>BigDataViewer-Playground>Sources>Export Sources to XML/HDF5 Spimdataset]
#### Input
* [int] **blocksizex**:
* [int] **blocksizey**:
* [int] **blocksizez**:
* [String] **entitytype**:Each source is an independent
* [int] **nthreads**:# of Threads
* [int] **numberoftimepointtoexport**:Number of timepoint to export (minimum 1)
* [SourceAndConverter[]] **sacs**:Select Source(s)
* [int] **scalefactor**:Scale factor between pyramid levels
* [int] **thresholdformipmap**:Dimensions in pixel above which a new resolution level should be created
* [int] **timepointbegin**:Timepoint start (0 = first timepoint)
* [File] **xmlfile**:Output file (XML)


### [BigDataBrowserPlugInCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/spimdata/BigDataBrowserPlugInCommand.java) [Plugins>BigDataViewer-Playground>BDVDataset>List BigDataServer Datasets]
#### Input
* [CommandService] **cs**:
* [LogService] **ls**:
* [String] **serverurl**:


### [MultipleSpimDataImporterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/spimdata/MultipleSpimDataImporterCommand.java) [Plugins>BigDataViewer-Playground>BDVDataset>Open XML BDV Datasets]
#### Input
* [File[]] **files**:
* [String] **message**:


### [SpimDataExporterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/spimdata/SpimDataExporterCommand.java) [Plugins>BigDataViewer-Playground>BDVDataset>Save BDVDataset]
#### Input
* [Context] **context**:
* [SourceAndConverter[]] **sacs**:Select source(s)
* [File] **xmlfilepath**:Output File (XML)


### [SpimdataBigDataServerImportCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/spimdata/SpimdataBigDataServerImportCommand.java) [Plugins>BigDataViewer-Playground>BDVDataset>BDVDataset [BigDataServer]]
Command that opens a BDV dataset from a BigDataServer. Click on Show to display it.
#### Input
* [String] **datasetname**:Dataset Name
* [String] **urlserver**:Big Data Server URL


### [StateSynchronizerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/viewer/StateSynchronizerCommand.java) [Plugins>BigDataViewer-Playground>Synchronize States]
Synchronizes the state of a set of BDV or BVV windows. A window popup should be closed to stop the synchronization
#### Input
* [BdvHandle[]] **bdvhs**:Select Bdv Windows to synchronize
* [BvvHandle[]] **bvvhs**:Select Bvv Windows to synchronize


### [ViewSynchronizerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/viewer/ViewSynchronizerCommand.java) [Plugins>BigDataViewer-Playground>Synchronize Views]
Synchronizes the view of a set of BDV or BVV windows. A window popup should be closed to stop the synchronization
#### Input
* [BdvHandle[]] **bdvhs**:Select Bdv Windows to synchronize
* [BvvHandle[]] **bvvhs**:Select Bvv Windows to synchronize
* [boolean] **synchronizetime**:Synchronize timepoints


### [BdvZoom](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijavacommand/BdvZoom.java) [Plugins>BigDataViewer>Playground>Zoom Controls]
#### Input
* [BdvHandle] **bdvh**:
* [Button] **button_in**:
* [Button] **button_out**:
* [double] **zoom_factor**:


### [RenameBdv](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijavacommand/RenameBdv.java) [Plugins>BigDataViewer-Playground>Another sub menu>Rename Bdv Window]
#### Input
* [BdvHandle] **bdvh**:
* [String] **title**:New Title


### [TestInteractiveCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijavacommand/TestInteractiveCommand.java) [Test>Test Interactive Command]
#### Input
* [String] **a_string**:


### [TestWidgetDemoCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijavacommand/TestWidgetDemoCommand.java) [Test>Sorted Sources]
#### Input
* [SourceAndConverter[]] **non_sorted_sources**:
* [SourceAndConverter[]] **sorted_sources**:



Process finished with exit code 0

 


</details>

## Wishlist of actions
Here we document actions we would like to have. If you know similar functionality in other repositories, feel free to contribute it here or send us a link where we can adopt code! Thanks :-)
* Log intensity in current source at mouse position
* 


