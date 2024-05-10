/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2024 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.tests;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.script.ScriptService;
import org.scijava.ui.UIService;
import org.scijava.ui.swing.SwingUI;
import sc.fiji.bdvpg.scijava.command.bdv.BdvCreatorCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import java.util.concurrent.ExecutionException;

public class TestBdvCommands {
    Context ctx;

    SourceAndConverterService sourceService;
    SourceAndConverterBdvDisplayService sourceDisplayService;
    CommandService commandService;
    @Before
    public void startFiji() {
        // Initializes static SourceService
        ctx = new Context();
        // Show UI
        ctx.service(UIService.class).showUI(SwingUI.NAME);

        sourceDisplayService = ctx.getService(SourceAndConverterBdvDisplayService.class);

        sourceService = ctx.getService(SourceAndConverterService.class);

        commandService = ctx.getService(CommandService.class);
    }

    @Test
    public void testBdvCreatorCommand() throws ExecutionException, InterruptedException {
        commandService.run(BdvCreatorCommand.class,true).get();
        Assert.assertEquals("Error - there should be one bdv created", 1, sourceDisplayService.getDisplays().size() );
    }

    @Test
    public void testBdvCreatorIJ1Macro() throws ExecutionException, InterruptedException{
        ctx.getService(ScriptService.class).run("dummy.ijm",
                "run(\"BDV - Create empty BDV window\");", true).get();
        Assert.assertEquals("Error - there should be one bdv created", 1, sourceDisplayService.getDisplays().size() );
    }


    @After
    public void closeFiji() {

        // Closes bdv windows
        sourceDisplayService.getDisplays().forEach(BdvHandle::close);

        // Clears all sources
        sourceService.remove(sourceService.getSourceAndConverters().toArray(new SourceAndConverter[0]));

        // Closes context
        ctx.close();
    }

}
