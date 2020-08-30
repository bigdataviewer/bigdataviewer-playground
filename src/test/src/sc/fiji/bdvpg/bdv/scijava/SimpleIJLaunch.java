package sc.fiji.bdvpg.bdv.scijava;

import net.imagej.ImageJ;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;
import sc.iview.SciView;

public class SimpleIJLaunch {

    static public void main(String... args) {

        SciView sciview = null;
        try {
            sciview = SciView.create();
        } catch (Exception e) {
            e.printStackTrace();
        }

        final ImageJ imagej = new ImageJ(sciview.getScijavaContext());
        // Arrange
        // create the ImageJ application context with all available services
        //final ImageJ ij = new ImageJ();
        imagej.ui().showUI();

        new SpimDataFromXmlImporter( "src/test/resources/mri-stack.xml" ).run();



    }
}
