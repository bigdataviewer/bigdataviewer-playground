package sc.fiji.bdvpg.bdv.stateio;

import net.imagej.ImageJ;
import sc.fiji.bdvpg.services.SourceAndConverterServiceLoader;

import java.io.File;

public class BdvPlaygroundStateLoader {
    public static void main( String[] args )
    {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        new SourceAndConverterServiceLoader(
                new File("src/test/resources/bdvplaygroundstate.json")
        ).run();

    }
}
