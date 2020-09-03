package sc.fiji.bdvpg.bdv.stateio;

import net.imagej.ImageJ;
import sc.fiji.bdvpg.services.SourceAndConverterServiceLoader;

import java.io.File;

public class BdvPlaygroundStateLoader {
    public static void main( String[] args )
    {
        //ProjectionModeChangerDemo.main(args);

        /*new SourceAndConverterServiceSaver(
                new File("src/test/resources/bdvplaygroundstate.json")
        ).run();*/

        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        new SourceAndConverterServiceLoader(
                new File("src/test/resources/bdvplaygroundstate.json")
        ).run();

    }
}
