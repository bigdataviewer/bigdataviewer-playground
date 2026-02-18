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

## Wishlist of actions
Here we document actions we would like to have. If you know similar functionality in other repositories, feel free to contribute it here or send us a link where we can adopt code! Thanks :-)
* Log intensity in current source at mouse position
* 


