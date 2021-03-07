package sc.fiji.bdvpg;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

public class TestHelper {

    public static void closeFijiAndBdvs(ImageJ ij) {
        try {

            // Closes bdv windows
            SourceAndConverterBdvDisplayService sac_display_service =
                    ij.context().getService(SourceAndConverterBdvDisplayService.class);
            sac_display_service.getDisplays().forEach(BdvHandle::close);

            // Clears all sources
            SourceAndConverterService sac_service =
                    ij.context().getService(SourceAndConverterService.class);
            sac_service.remove(sac_service.getSourceAndConverters().toArray(new SourceAndConverter[0]));

            // Closes ij context
            ij.context().close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
