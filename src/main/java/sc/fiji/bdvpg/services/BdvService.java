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
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        iss = ij.get(BdvSourceAndConverterService.class);
        isds = ij.get(BdvSourceAndConverterDisplayService.class);
    }

    /**
     * Creates Default Services -> No SciJava
     */
    static public void InitDefaultServices() {
        // TODO
        // iss = ...
        // isds = ...
    }
}
