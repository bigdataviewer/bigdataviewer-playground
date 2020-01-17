package sc.fiji.bdvpg.services;

import net.imagej.ImageJ;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterDisplayService;

/**
 * Static methods to access BdvSourceAndConverterService and BdvSourceAndConverterDisplayService
 */

public class BdvService {
    public static IBdvSourceAndConverterService bdvSourceAndConverterService;
    public static BdvSourceAndConverterDisplayService bdvSourceAndConverterDisplayService;
    private static ImageJ ij;

    public static IBdvSourceAndConverterService getSourceAndConverterService() {
        return bdvSourceAndConverterService;
    }

    public static BdvSourceAndConverterDisplayService getSourceAndConverterDisplayService() {
        return bdvSourceAndConverterDisplayService;
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
