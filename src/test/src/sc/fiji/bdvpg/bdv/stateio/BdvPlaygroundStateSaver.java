package sc.fiji.bdvpg.bdv.stateio;

import sc.fiji.bdvpg.bdv.ProjectionModeChangerDemo;
import sc.fiji.bdvpg.bdv.sourceandconverter.transform.AffineTransformSourceDemo;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;

import java.io.File;

public class BdvPlaygroundStateSaver {
    public static void main( String[] args )
    {

        //ProjectionModeChangerDemo.main(args);
        AffineTransformSourceDemo.main(args);

        new SourceAndConverterServiceSaver(
                new File("src/test/resources/bdvplaygroundstate.json")
        ).run();

        System.out.println("Saved!");

    }
}
