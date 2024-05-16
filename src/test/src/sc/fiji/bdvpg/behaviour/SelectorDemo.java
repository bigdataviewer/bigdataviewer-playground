package sc.fiji.bdvpg.behaviour;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import org.scijava.Context;
import org.scijava.ui.UIService;
import org.scijava.ui.swing.SwingUI;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAdjuster;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;


/**
 * Demo of bigdataviewer-selector. Press E to enter into the selector mode.
 */
public class SelectorDemo {

    public static void main(String... args) {
        // Initializes static SourceService
        Context ctx = new Context();
        // Show UI
        ctx.service(UIService.class).showUI(SwingUI.NAME);

        SourceAndConverterBdvDisplayService sourceDisplayService = ctx.getService(SourceAndConverterBdvDisplayService.class);
        SourceAndConverterService sourceService = ctx.getService(SourceAndConverterService.class);

        // Open two example sources
        new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").get();
        new SpimDataFromXmlImporter("src/test/resources/demoSlice.xml").get();

        BdvHandle bdvh = sourceDisplayService.getNewBdv();
        sourceDisplayService.show(bdvh, sourceService.getSourceAndConverters().toArray(new SourceAndConverter[0]));

        new ViewerTransformAdjuster(bdvh, sourceService.getSourceAndConverters().toArray(new SourceAndConverter[0])).run();
        new BrightnessAdjuster(sourceService.getSourceAndConverters().get(0),0,255).run();
        new BrightnessAdjuster(sourceService.getSourceAndConverters().get(1),0,255).run();

        SourceSelectorBehaviour ssb = new SourceSelectorBehaviour(bdvh, "E");

        // Stores the associated selector to the display
        SourceAndConverterServices.getBdvDisplayService().setDisplayMetadata(bdvh,
                SourceSelectorBehaviour.class.getSimpleName(), ssb);

        new EditorBehaviourInstaller(ssb, "").run();

    }
}
