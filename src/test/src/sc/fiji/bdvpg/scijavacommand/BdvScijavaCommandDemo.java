package sc.fiji.bdvpg.scijavacommand;

import bdv.ui.BdvDefaultCards;
import bdv.util.BdvHandle;
import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.scijava.BdvScijavaHelper;
import sc.fiji.bdvpg.scijava.ScijavaSwingUI;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class BdvScijavaCommandDemo {
    static ImageJ ij;

    public static void main( String[] args )
    {
        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ij = new ImageJ();
        ij.ui().showUI();

        // Creates a BDV since none exists yet
        BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getActiveBdv();

        // Adds a scijava Command in the menu bar
        BdvScijavaHelper
                .addCommandToBdvHandleMenu(
                        bdvh,
                        ij.context(),
                        RenameBdv.class,
                        2, // Skips Plugins > BigDataViewer (two levels of menu hierarchy)
                        "bdvh", bdvh // Fill in scijava parameters : key1, v1, key2, v2
                        );

        // Adds a scijava Interactive Command as a card panel
        bdvh.getSplitPanel().setCollapsed(false);
        bdvh.getCardPanel().addCard("Zoom",
                ScijavaSwingUI.getPanel(ij.context(), BdvZoom.class, "bdvh", bdvh), true);

        bdvh.getCardPanel().setCardExpanded(BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD, false);
        bdvh.getCardPanel().setCardExpanded(BdvDefaultCards.DEFAULT_SOURCES_CARD, false);


    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }
}
