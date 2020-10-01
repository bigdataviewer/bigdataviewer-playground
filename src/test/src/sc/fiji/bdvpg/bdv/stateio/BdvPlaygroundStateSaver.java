package sc.fiji.bdvpg.bdv.stateio;

import org.junit.Test;
import sc.fiji.bdvpg.bdv.ProjectionModeChangerDemo;
import sc.fiji.bdvpg.bdv.sourceandconverter.bigwarp.BigWarpDemo;
import sc.fiji.bdvpg.bdv.sourceandconverter.transform.AffineTransformSourceDemo;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;

import java.io.File;

public class BdvPlaygroundStateSaver {

    public static void main( String[] args )
    {

        //ProjectionModeChangerDemo.main(args); // Test SpimData Saving
        //AffineTransformSourceDemo.main(args); // Test Transformed Source Saving
        BigWarpDemo.main(args); // Test Warped source saving

        new SourceAndConverterServiceSaver(
                new File("src/test/resources/bdvplaygroundstate.json")
        ).run();

        System.out.println("Saved!");

    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }
}
