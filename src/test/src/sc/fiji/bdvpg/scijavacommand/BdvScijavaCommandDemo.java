/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
