package sc.fiji.bdvpg.services;

import net.imagej.ImageJ;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

/**
 * Static methods to access BdvSourceAndConverterService and BdvSourceAndConverterDisplayService
 */

public class SourceAndConverterServices
{
    private static ISourceAndConverterService sourceAndConverterService;

    private static SourceAndConverterBdvDisplayService sourceAndConverterBdvDisplayService;

    private static ImageJ ij;

    public static ISourceAndConverterService getSourceAndConverterService() {
        return sourceAndConverterService;
    }

    public static void setSourceAndConverterService(ISourceAndConverterService sourceAndConverterService) {
        SourceAndConverterServices.sourceAndConverterService = sourceAndConverterService;
    }

    public static SourceAndConverterBdvDisplayService getSourceAndConverterDisplayService() {
        return sourceAndConverterBdvDisplayService;
    }

    public static void setSourceAndConverterDisplayService(SourceAndConverterBdvDisplayService sourceAndConverterBdvDisplayService) {
        SourceAndConverterServices.sourceAndConverterBdvDisplayService = sourceAndConverterBdvDisplayService;
    }

    /**
     * Creates Services Within SciJava context
     */
    /*static public void InitScijavaServices() {

    }*/
}
