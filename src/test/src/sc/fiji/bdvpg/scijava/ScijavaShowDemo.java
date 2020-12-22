package sc.fiji.bdvpg.scijava;

import bdv.viewer.SourceAndConverter;
import loci.common.DebugTools;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import org.scijava.command.CommandService;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
import sc.fiji.bdvpg.scijava.command.spimdata.MultipleSpimDataImporterCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Almost like an integration test for scijava commands
 */

public class ScijavaShowDemo {

    public static void main(String... args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        DebugTools.setRootLevel("INFO");

        CommandService cs = ij.command();

        AbstractSpimData[] bdvDatasetArray = (AbstractSpimData[]) cs.run(MultipleSpimDataImporterCommand.class, true,
                "files", new String[] {"src/test/resources/mri-stack.xml","src/test/resources/mri-stack-shiftedX.xml"}
                ).get().getOutput("spimDataArray");

        List<SourceAndConverter<?>> sources = new ArrayList<>();

        for (AbstractSpimData dataset : bdvDatasetArray) {
            sources.addAll(SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getSourceAndConverterFromSpimdata(dataset));
        }

        for (SourceAndConverter<?> sac : sources) {
            System.out.println("------------------"+sac.getSpimSource().getName());
        }

        // First solution : gives sourceandconverter array : work
        //cs.run(BdvSourcesAdderCommand.class, true,
        //        "sacs", sources.toArray(new SourceAndConverter[0])).get();

        // Second solution : gives path to sources in the ui manager : fails
        cs.run(BdvSourcesAdderCommand.class, true,
                "sacs", "mri-stack.xml").get(); // this fails!! the converter to String array is called!

    }
}
