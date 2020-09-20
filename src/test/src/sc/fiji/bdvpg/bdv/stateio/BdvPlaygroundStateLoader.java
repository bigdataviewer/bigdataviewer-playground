package sc.fiji.bdvpg.bdv.stateio;

import net.imagej.ImageJ;
import org.junit.Test;
import sc.fiji.bdvpg.services.SourceAndConverterServiceLoader;

public class BdvPlaygroundStateLoader {
    public static void main( String[] args )
    {
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        new SourceAndConverterServiceLoader("src/test/resources/bdvplaygroundstate.json").run();
    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }
}
