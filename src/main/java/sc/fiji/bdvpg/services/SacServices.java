package sc.fiji.bdvpg.services;

import net.imagej.ImageJ;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

/**
 * Static methods to access BdvSourceAndConverterService and BdvSourceAndConverterDisplayService
 */

public class SacServices
{
    public static ISourceAndConverterService sourceAndConverterService;
    public static SourceAndConverterBdvDisplayService sourceAndConverterBdvDisplayService;
    private static ImageJ ij;

    public static ISourceAndConverterService getSacService() {
        return sourceAndConverterService;
    }

    public static SourceAndConverterBdvDisplayService getSacDisplayService() {
        return sourceAndConverterBdvDisplayService;
    }

    /**
     * Creates Services Within SciJava context
     */
    static public void InitScijavaServices() {
        // create the ImageJ application context with all available services
        ij = new ImageJ();
        ij.ui().showUI();
    }

    /**
     * Creates Default Services -> No SciJava
     */
    static public void InitDefaultServices() {
        // TODO
        // iss = ...
        // isds = ...
    }

    public static ImageJ getIJ()
    {
        return ij;
    }
}
