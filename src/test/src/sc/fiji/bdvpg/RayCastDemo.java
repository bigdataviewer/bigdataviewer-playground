package sc.fiji.bdvpg;

import bdv.util.BdvHandle;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.RayCastPositionerSliderAdder;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class RayCastDemo {

    static ImageJ ij;

    public static void main(String... args) {
        // Initializes static SourceService and Display Service

        ij = new ImageJ();
        ij.ui().showUI();

        AffineTransformSourceDemo.demo(3);

        BdvHandle bdvh = SourceAndConverterServices
                .getBdvDisplayService()
                .getActiveBdv();

        BdvHandleHelper.addCenterCross(bdvh);

        new RayCastPositionerSliderAdder(bdvh).run();
    }
}
