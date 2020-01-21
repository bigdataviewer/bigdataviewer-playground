## BDV playground / toolbox / actions
In this repository, we collect useful additions for the [BigDataViewer](https://imagej.net/BigDataViewer) in [Fiji](https://fiji.sc).

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
* 
* 

## Coding guides
We tried to follow these general guide lines:
* Specific (atomic) functionality lives in a class implementing [Runnable](https://docs.oracle.com/javase/7/docs/api/java/lang/Runnable.html)
  * The constructor takes a [BdvHandle](https://github.com/bigdataviewer/bigdataviewer-vistools/blob/master/src/main/java/bdvh/util/BdvHandle.java) as parameter.
  * More mandatory parameters for the functionality are handed over as additional constructor parameters.
  * Optional parameters are set with setter methods.
  * The `run()` method executes the concreate action.
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

* Furthermore, [Demo code](https://github.com/haesleinhuepf/bigdataviewer-playground/tree/master/src/test/src/sc/fiji/bdv) in the test sources directory should demonstrate
  * the individual functionality, 
  * how to add it as key listeners to the BigDataViewer,
  * how to add it to the right-click menu of the BigDataViewer 
  * how to add it to the BigDataViewer menu
  * and how to be called programmatically.

* To enable additional user access, some functionality in this repository could also be a ImageJ/Fiji. Technically these menu entries are implemented as [SciJava Command](https://javadoc.scijava.org/SciJava/org/scijava/command/Command.html).
  
