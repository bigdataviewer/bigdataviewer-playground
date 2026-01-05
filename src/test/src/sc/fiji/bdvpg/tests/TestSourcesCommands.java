/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import net.imagej.legacy.LegacyService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.script.ScriptService;
import org.scijava.ui.UIService;
import org.scijava.ui.swing.SwingUI;
import sc.fiji.bdvpg.scijava.command.source.NewSourceCommand;
import sc.fiji.bdvpg.scijava.command.source.SourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;
import sc.fiji.persist.IObjectScijavaAdapterService;

import java.util.concurrent.ExecutionException;

public class TestSourcesCommands {
    Context ctx;

    SourceAndConverterService sourceService;
    SourceAndConverterBdvDisplayService sourceDisplayService;
    CommandService commandService;
    @Before
    public void startFiji() {
        // Initializes static SourceService
        ctx = new Context(UIService.class,
                SourceAndConverterService.class,
                SourceAndConverterBdvDisplayService.class,
                IObjectScijavaAdapterService.class,
                LegacyService.class); // for ij1 macro testing

        sourceDisplayService = ctx.getService(SourceAndConverterBdvDisplayService.class);
        sourceService = ctx.getService(SourceAndConverterService.class);
        commandService = ctx.getService(CommandService.class);

        // Open two example sources
        new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").get();
        new SpimDataFromXmlImporter("src/test/resources/demoSlice.xml").get();

    }

    @Test(timeout=5000)
    public void testSourceDeleteCommand() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        commandService.run(SourcesRemoverCommand.class,true, "sacs", "mri-stack.xml").get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }

    @Test(timeout=5000)
    public void testSourceDeleteIJ1Macro() throws ExecutionException, InterruptedException{
        ctx.getService(ScriptService.class).run("dummy.ijm",
                "run(\"Delete Sources\", \"sacs=[mri-stack.xml]\");", true).get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size() );
    }

    @Test(timeout=5000)
    public void testSourceDeleteNestedPathCommand() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        commandService.run(SourcesRemoverCommand.class,true, "sacs", "mri-stack.xml>Channel>1").get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }

    @Test(timeout=5000)
    public void testSourceDeleteNestedPathIJ1Macro() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        ctx.getService(ScriptService.class).run("dummy.ijm",
                "run(\"Delete Sources\", \"sacs=[mri-stack.xml>Channel>1]\");", true).get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }

    /*
    // Splitting by comma is not supported and could be a bad idea. Let's refrain from it until it becomes absolutely necessary
    The workaround is to run several times the deletion on the different paths manually
    @Test(timeout=5000)
    public void testMultipleSourcesDeleteCommand() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        commandService.run(SourcesRemoverCommand.class,true, "sacs", "mri-stack.xml, demoSlice.xml").get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }

    @Test(timeout=5000)
    public void testMultipleSourcesDeleteIJ1Macro() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size() );
        ctx.getService(ScriptService.class).run("dummy.ijm",
                "run(\"Delete Sources\", \"sacs=[mri-stack.xml, demoSlice.xml]\");", true).get();
        Assert.assertEquals("Error - there should be one source left", 1, sourceService.getSourceAndConverters().size());
    }*/

    //NewSourceCommand
    @Test(timeout=5000)
    public void testNewSourceCommand() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size());
        commandService.run(NewSourceCommand.class,true,
                "model", "mri-stack.xml>Channel>1",
                "name", "model",
                "voxsizex",1,
                "voxsizey",1,
                "voxsizez",1,
                "timepoint",0).get();
        Assert.assertEquals("Error - there should three sources after one is created", 3, sourceService.getSourceAndConverters().size());
    }

    @Test(timeout=5000)
    public void testNewSourceIJ1Macro() throws ExecutionException, InterruptedException {
        Assert.assertEquals("Error - there should be two sources at the beginning of the test", 2, sourceService.getSourceAndConverters().size());
        //New Source Based on Model Source
        ctx.getService(ScriptService.class).run("dummy.ijm",
                "run(\"New Source Based on Model Source\", \""+
                        "model=[mri-stack.xml>Channel>1] " +
                        "name=model " +
                        "voxsizex=1 " +
                        "voxsizey=1 " +
                        "voxsizez=1 " +
                        "timepoint=0 " +
                        "\");", true).get();
        Assert.assertEquals("Error - there should three sources after one is created", 3, sourceService.getSourceAndConverters().size());
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
