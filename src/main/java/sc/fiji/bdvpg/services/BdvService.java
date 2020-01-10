package sc.fiji.bdvpg.services;

import net.imagej.ImageJ;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterDisplayService;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService;

/**
 * Static methods to access BdvSourceAndConverterService and BdvSourceAndConverterDisplayService
 */

public class BdvService {
    public static IBdvSourceAndConverterService iss;
    public static IBdvSourceAndConverterDisplayService isds;
    private static ImageJ ij;

    public static IBdvSourceAndConverterService getSourceService() {
        return iss;
    }

    public static IBdvSourceAndConverterDisplayService getSourceDisplayService() {
        return isds;
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
