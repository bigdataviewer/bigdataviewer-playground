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
