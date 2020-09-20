package sc.fiji.bdvpg;

import net.imagej.ImageJ;

public class SimpleIJLaunch {

    static public void main(String... args) {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
    }
}
