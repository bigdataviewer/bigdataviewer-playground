package sc.fiji.bdvpg.scijava;

import loci.common.DebugTools;
import net.imagej.ImageJ;
import org.scijava.command.CommandService;
import sc.fiji.bdvpg.scijava.command.spimdata.MultipleSpimDataImporterCommand;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

/**
 * Almost like an integration test for scijava commands
 */

public class ScijavaShowDemo {

    public static void main(String... args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        DebugTools.setRootLevel("INFO");

        CommandService cs = ij.command();


        cs.run(MultipleSpimDataImporterCommand.class, true,
                "files", new String[] {"src/test/resources/mri-stack.xml","src/test/resources/mri-stack-shiftedX.xml"}
                ).get();

        // Import SpimData
        //new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").run();
        //new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();

    }
}
