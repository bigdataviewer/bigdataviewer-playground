package sc.fiji.bdvpg.services;

import net.imagej.ImageJ;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;
import sc.fiji.bdvpg.scijava.services.BdvSourceService;

/**
 * Static methods to access BdvSourceService and BdvSourceDisplayService
 */

public class BdvService {
    public static IBdvSourceService iss;
    public static IBdvSourceDisplayService isds;

    public static IBdvSourceService getSourceService() {
        return iss;
    }

    public static IBdvSourceDisplayService getSourceDisplayService() {
        return isds;
    }

    /**
     * Creates Services Within SciJava context
     */
    static public void InitScijavaServices() {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        iss = ij.get(BdvSourceService.class);
        isds = ij.get(BdvSourceDisplayService.class);
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
