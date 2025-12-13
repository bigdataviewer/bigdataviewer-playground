/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.demos.behaviour;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import org.scijava.Context;
import org.scijava.ui.UIService;
import org.scijava.ui.swing.SwingUI;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.behaviour.EditorBehaviourInstaller;
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
