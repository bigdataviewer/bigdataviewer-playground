package sc.fiji.bdvpg.bdv.scijava;

import bdv.util.BdvHandle;
import net.imagej.ImageJ;

public class SimpleIJLaunch {

    static public void main(String... args) {
        // Arrange
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        BdvHandle bdvh;

    }
}
