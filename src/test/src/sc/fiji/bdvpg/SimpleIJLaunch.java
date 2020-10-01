package sc.fiji.bdvpg;

import loci.common.DebugTools;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;
import sc.iview.SciView;
import org.junit.Test;

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


        // create the ImageJ application context with all available services
        //final ImageJ ij = new ImageJ();
        //ij.ui().showUI();
        DebugTools.setRootLevel("INFO");
    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }

}
