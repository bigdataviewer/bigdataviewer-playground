package sc.fiji.bdvpg.bdv.scijava;

import net.imagej.ImageJ;
import sc.iview.SciView;

public class SimpleIJLaunch {

    static public void main(String... args) {

        SciView sciview = null;
        try {
            sciview = SciView.create();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ImageJ imagej = new ImageJ(sciview.getScijavaContext());
        // Arrange
        // create the ImageJ application context with all available services
        //final ImageJ ij = new ImageJ();
        imagej.ui().showUI();

    }
}
