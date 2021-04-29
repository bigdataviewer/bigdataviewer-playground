[![](https://travis-ci.com/bigdataviewer/bigdataviewer-playground.svg?branch=master)](https://travis-ci.com/bigdataviewer/bigdataviewer-playground)
[![Gitter](https://badges.gitter.im/bigdataviewer-playground/community.svg)](https://gitter.im/bigdataviewer-playground/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)


## BDV playground / toolbox / actions

In this repository, we collect useful additions for the [BigDataViewer](https://imagej.net/BigDataViewer) in [Fiji](https://fiji.sc). These functionalities are accessible by enabling the `BigDataViewer-Playground` Fiji update site, accessible via `Help>Update>Manage Update Sites`.

User documentation is located can be found at [https://imagej.github.io/plugins/bdv/playground](https://imagej.github.io/plugins/bdv/playground)

## Coding guides
We tried to follow these general guide lines:
* Specific (atomic) functionality lives in a class implementing [Runnable](https://docs.oracle.com/javase/7/docs/api/java/lang/Runnable.html)
  * More mandatory parameters for the functionality are handed over as additional constructor parameters.
  * Optional parameters are set with setter methods.
  * The `run()` method executes the concrete action.
  * Results are retrieved using getter methods. The getter methods may internally run the `run()` method if necessary.

Pseudo code example:
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

## List of Scijava Commands 

<details>
 <summary>Source Service State</summary>
 
### [LoadSourceAndConverterServiceState](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/LoadSourceAndConverterServiceState.java) [BigDataViewer>Load Bdv Playground State (experimental)]
#### Input
* [Context] **ctx**:
* [File] **file**:


### [SaveSourceAndConverterServiceState](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/SaveSourceAndConverterServiceState.java) [BigDataViewer>Save Bdv Playground State (experimental)]
#### Input
* [Context] **ctx**:
* [File] **file**:


### [ShowSourceAndConverterServiceWindow](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/ShowSourceAndConverterServiceWindow.java) [BigDataViewer>Show Bdv Playground Window]
#### Input
* [SourceAndConverterService] **sacs**:

</details>

<details>
 <summary>BigDataViewer</summary>

### [BdvOrthoWindowCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvOrthoWindowCreatorCommand.java) [BigDataViewer>BDV>BDV - Create Orthogonal Views]
Creates 3 BDV windows with synchronized orthogonal views
#### Input
* [boolean] **drawCrosses**:Add cross overlay to show view plane locations
* [boolean] **interpolate**:Interpolate
* [int] **locationX**:X Front Window location
* [int] **locationY**:Y Front Window location
* [int] **nTimepoints**:Number of timepoints (1 for a single timepoint)
* [String] **projector**:Source Projection Mode
* [int] **screen**:Display (0 if you have one screen)
* [int] **sizeX**:Window Width
* [int] **sizeY**:Window Height
* [String] **windowTitle**:Title of BDV windows
#### Output
* [BdvHandle] **bdvhX**:
* [BdvHandle] **bdvhY**:
* [BdvHandle] **bdvhZ**:


### [BdvSetTimepointsNumberCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSetTimepointsNumberCommand.java) [BigDataViewer>BDV>BDV - Set Number Of Timepoints]
Sets the number of timepoints in one or several BDV Windows
#### Input
* [BdvHandle[]] **bdvhs**:Select BDV Windows
* [int] **numberOfTimePoints**:Number of timepoints, min = 1


### [BdvSettingsCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSettingsCommand.java) [BigDataViewer>BDV>BDV - Preferences - Set (Key) Bindings]
Sets actions linked to key / mouse event in BDV (WIP, currently not working)


### [BdvSourcesAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSourcesAdderCommand.java) [BigDataViewer>BDV>BDV - Show Sources]
Adds one or several sources to an existing BDV window
#### Input
* [boolean] **adjustViewOnSource**:Adjust View on Source
* [boolean] **autoContrast**:Auto Contrast
* [BdvHandle] **bdvh**:Select BDV Window
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [BdvSourcesRemoverCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSourcesRemoverCommand.java) [BigDataViewer>BDV>BDV - Remove Sources From BDV]
Removes one or several sources from an existing BDV window
#### Input
* [BdvHandle] **bdvh**:
* [SourceAndConverter[]] **srcs_in**:Select Source(s)


### [BdvSourcesShowCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvSourcesShowCommand.java) [BigDataViewer>BDV>BDV - Show Sources (new Bdv window)]
Displays one or several sources into a new BDV window
#### Input
* [boolean] **adjustViewOnSource**:Adjust View on Source
* [boolean] **autoContrast**:Auto Contrast
* [boolean] **interpolate**:Interpolate
* [boolean] **is2D**:Create a 2D BDV window
* [int] **nTimepoints**:Number of timepoints (1 for a single timepoint)
* [String] **projector**:
* [SourceAndConverter[]] **sacs**:Select Source(s)
* [String] **windowTitle**:Title of the new BDV window
#### Output
* [BdvHandle] **bdvh**:


### [BdvWindowCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/BdvWindowCreatorCommand.java) [BigDataViewer>BDV>BDV - Create empty BDV window]
Creates an empty BDV window
#### Input
* [boolean] **interpolate**:Interpolate
* [boolean] **is2D**:Create a 2D BDV window
* [int] **nTimepoints**:Number of timepoints (1 for a single timepoint)
* [String] **projector**:
* [String] **windowTitle**:Title of the new BDV window
#### Output
* [BdvHandle] **bdvh**:


### [MultiBdvSourcesAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/MultiBdvSourcesAdderCommand.java) [BigDataViewer>BDV>BDV - Show Sources In Multiple BDV Windows]
Adds one or several sources into several existing BDV windows
#### Input
* [BdvHandle[]] **bdvhs**:Select BDV Windows
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [ScreenShotMakerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/ScreenShotMakerCommand.java) [BigDataViewer>BDV>BDV - Screenshot]
Creates a screenshot of a BDV view, the resolution can be chosen to upscale or downscale the image compared to the original window. A single RGB image resulting from the projection of all sources is displayed. Raw image data can also be exported in grayscale.
#### Input
* [BdvHandle] **bdvh**:
* [boolean] **showRawData**:Show Raw Data
* [double] **targetPixelSizeInXY**:Target Pixel Size (in XY)
* [String] **targetPixelUnit**:Pixel Size Unit


### [ViewSynchronizerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/ViewSynchronizerCommand.java) [BigDataViewer>BDV>BDV - Synchronize Views]
Synchronizes the view of a set of BDV windows. A window popup should be closed to stop the synchronization
#### Input
* [BdvHandle[]] **bdvhs**:Select Windows to synchronize
* [boolean] **synchronizeTime**:Synchronize timepoints


### [ViewTransformLoggerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/ViewTransformLoggerCommand.java) [BigDataViewer>BDV>BDV - Log view transform]
Outputs the current view transfrom of a BDV window into the standard IJ logger
#### Input
* [BdvHandle] **bdvh**:
* [LogService] **ls**:


### [ViewTransformatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bdv/ViewTransformatorCommand.java) [BigDataViewer>BDV>BDV - Change view transform]
Applies a simple view transform (translation / rotation) to a BDV window
#### Input
* [BdvHandle] **bdvh**:Select BDV Windows
* [Double] **rotateAroundX**:Rotate around X
* [Double] **rotateAroundY**:Rotate around Y
* [Double] **rotateAroundZ**:Rotate around Z
* [Double] **translateX**:Translate in X
* [Double] **translateY**:Translate in Y
* [Double] **translateZ**:Translate in Z

</details>

<details>
 <summary>BigVolumeViewer</summary>

### [BvvSourcesAdderCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bvv/BvvSourcesAdderCommand.java) [BigDataViewer>BVV>Show Sources in BVV]
Show sources in a BigVolumeViewer window - limited to 16 bit images
#### Input
* [boolean] **adjustViewOnSource**:Adjust View on Source
* [BvvHandle] **bvvh**:Select BVV Window(s)
* [SourceAndConverter[]] **sacs**:Select source(s)


### [BvvWindowCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/bvv/BvvWindowCreatorCommand.java) [BigDataViewer>BVV>Create Empty BVV Frame]
Creates an empty Bvv window
#### Input
* [int] **nTimepoints**:Number of timepoints (1 for a single timepoint)
* [String] **windowTitle**:Title of the new BVV window
#### Output
* [BvvHandle] **bvvh**:


</details>

<details>
 <summary>Sources</summary>

### [BasicTransformerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/BasicTransformerCommand.java) [BigDataViewer>Sources>Transform>Basic Transformation]
Performs basic transformation (rotate / flip) along X Y Z axis for several sources. If global is selected, the transformation is performed relative to the global origin (0,0,0). If global is not selected, the center of each source is unchanged.
#### Input
* [String] **axis**:
* [boolean] **globalChange**:
* [SourceAndConverter[]] **sources_in**:Select source(s)
* [int] **timepoint**:
* [String] **type**:


### [BigWarpLauncherCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/BigWarpLauncherCommand.java) [BigDataViewer>Sources>Register>Launch BigWarp]
Starts BigWarp from existing sources
#### Input
* [String] **bigWarpName**:Window title for BigWarp
* [SourceAndConverterBdvDisplayService] **bsds**:
* [SourceAndConverter[]] **fixedSources**:Fixed Source(s)
* [SourceAndConverter[]] **movingSources**:Moving Source(s)
#### Output
* [BdvHandle] **bdvhP**:
* [BdvHandle] **bdvhQ**:
* [SourceAndConverter] **gridSource**:
* [SourceAndConverter] **warpMagnitudeSource**:
* [SourceAndConverter[]] **warpedSources**:


### [BrightnessAdjusterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/BrightnessAdjusterCommand.java) [BigDataViewer>Sources>Display>Set Sources Brightness]
#### Input
* [double] **max**:
* [double] **maxSlider**:relative Maximum
* [String] **message**:
* [double] **min**:
* [double] **minSlider**:relative Minimum
* [SourceAndConverter[]] **sources**:Select Source(s)


### [ColorSourceCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/ColorSourceCreatorCommand.java) [BigDataViewer>Sources>Display>Create New Source (Set Color)]
Duplicate one or several sources and sets a new color for these sources
#### Input
* [ColorRGB] **color**:
* [SourceAndConverter[]] **sources_in**:Select Source(s)


### [LUTSourceCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/LUTSourceCreatorCommand.java) [BigDataViewer>Sources>Display>Create New Source (Set LUT)]
Duplicate one or several sources and sets an (identical) Look Up Table for these duplicated sources
#### Input
* [String] **choice**:LUT name
* [ConvertService] **cs**:
* [LUTService] **lutService**:
* [SourceAndConverter[]] **sources_in**:Select Source(s)
* [ColorTable] **table**:LUT


### [MakeGroupCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/MakeGroupCommand.java) [BigDataViewer>Sources>Make Global Source Group]
Adds a node in the tree view which selects the sources specified in the command
#### Input
* [boolean] **displaySources**:Display Sources
* [String] **groupName**:Name of the group
* [SourceAndConverterService] **sac_service**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [ManualTransformCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/ManualTransformCommand.java) [BigDataViewer>Sources>Transform>Manual Sources Transformation]
Manual transformation of selected sources. Works only with a single bdv window (the active one).The sources that are not displayed but selected are transformed. During the registration, the user isplaced in the reference of the moving sources. That's why they are not moving during the registration.
#### Input
* [BdvHandle] **bdvHandle**:
* [String] **mode**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [NewSourceCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/NewSourceCommand.java) [BigDataViewer>Sources>New Source Based on Model Source]
Defines an empty source which occupied the same volume as a model source but with a potentially different voxel size. Works with a single timepoint.
#### Input
* [SourceAndConverter] **model**:Model Source
Defines the portion of space covered by the new source
* [String] **name**:Source name
* [int] **timePoint**:Timepoint (0 based index)
* [double] **voxSizeX**:Voxel Size X
* [double] **voxSizeY**:Voxel Size Y
* [double] **voxSizeZ**:Voxel Size Z
#### Output
* [SourceAndConverter] **newsource**:


### [SampleSourceCreatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SampleSourceCreatorCommand.java) [BigDataViewer>Sources>Create Sample Source]
#### Input
* [String] **sampleName**:Sample name
#### Output
* [SourceAndConverter] **sampleSource**:


### [SourceAndConverterProjectionModeChangerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourceAndConverterProjectionModeChangerCommand.java) [BigDataViewer>Sources>Display>Set Sources Projection Mode]
#### Input
* [boolean] **addToOccludingLayer**:Add Source(s) to occluding layer
* [String] **projectionMode**:Projection Mode
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [SourceColorChangerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourceColorChangerCommand.java) [BigDataViewer>Sources>Display>Set Sources Color]
#### Input
* [ColorRGB] **color**:
* [SourceAndConverter[]] **sources_in**:Select Source(s)


### [SourcesDuplicatorCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesDuplicatorCommand.java) [BigDataViewer>Sources>Duplicate Sources]
#### Input
* [SourceAndConverter[]] **sources_in**:Select Source(s)


### [SourcesInvisibleMakerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesInvisibleMakerCommand.java) [BigDataViewer>Sources>Display>Make Sources Invisible]
#### Input
* [SourceAndConverterBdvDisplayService] **bsds**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [SourcesRemoverCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesRemoverCommand.java) [BigDataViewer>Sources>Delete Sources]
#### Input
* [SourceAndConverterService] **bss**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [SourcesResamplerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesResamplerCommand.java) [BigDataViewer>Sources>Resample Source Based on Model Source]
#### Input
* [boolean] **cache**:
* [boolean] **interpolate**:
* [SourceAndConverter] **model**:
* [boolean] **reuseMipMaps**:Re-use MipMaps
* [SourceAndConverter[]] **sourcesToResample**:Select Source(s)


### [SourcesVisibleMakerCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/SourcesVisibleMakerCommand.java) [BigDataViewer>Sources>Display>Make Sources Visible]
#### Input
* [SourceAndConverterBdvDisplayService] **bsds**:
* [SourceAndConverter[]] **sacs**:Select Source(s)


### [TransformedSourceWrapperCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/TransformedSourceWrapperCommand.java) [BigDataViewer>Sources>Transform>Wrap as Transformed Source]
#### Input
* [SourceAndConverter[]] **sources_in**:Select Source(s)


</details>

<details>
 <summary>BDV Datasets</summary>

### [XmlHDF5ExporterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/source/XmlHDF5ExporterCommand.java) [BigDataViewer>Sources>Export Sources to XML/HDF5 Spimdataset]
#### Input
* [int] **blockSizeX**:
* [int] **blockSizeY**:
* [int] **blockSizeZ**:
* [int] **nThreads**:# of Threads
* [SourceAndConverter[]] **sacs**:Select Source(s)
* [int] **scaleFactor**:
* [int] **timePointBegin**:Timepoint (Beginning)
* [int] **timePointEnd**:Timepoint (End)
* [File] **xmlFile**:Output file (XML)


### [BigDataBrowserPlugInCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/spimdata/BigDataBrowserPlugInCommand.java) [BigDataViewer>BDVDataset>List BigDataServer Datasets]
#### Input
* [CommandService] **cs**:
* [LogService] **ls**:
* [String] **serverUrl**:


### [MultipleSpimDataImporterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/spimdata/MultipleSpimDataImporterCommand.java) [BigDataViewer>BDVDataset>Open XML/HDF5 Files]
#### Input
* [File[]] **files**:
* [String] **message**:


### [SpimDataExporterCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/spimdata/SpimDataExporterCommand.java) [BigDataViewer>BDVDataset>Save BDVDataset]
#### Input
* [SourceAndConverter] **sac**:Select source(s)
* [File] **xmlFilePath**:Output File (XML)


### [SpimdataBigDataServerImportCommand](https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/sc/fiji/bdvpg/scijava/command/spimdata/SpimdataBigDataServerImportCommand.java) [BigDataViewer>BDVDataset>BDVDataset [BigDataServer]]
Command that opens a BDV dataset from a BigDataServer. Click on Show to display it.
#### Input
* [String] **datasetName**:Dataset Name
* [String] **urlServer**:Big Data Server URL


</details>

## Wishlist of actions
Here we document actions we would like to have. If you know similar functionality in other repositories, feel free to contribute it here or send us a link where we can adopt code! Thanks :-)
* Log intensity in current source at mouse position
* 


