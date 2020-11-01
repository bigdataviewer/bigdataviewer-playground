package sc.fiji.bdvpg.scijava.command;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Show Bdv Playground Window")
public class ShowSourceAndConverterServiceWindow implements Command {

    @Parameter
    SourceAndConverterService sacs;

    public void run() {
        sacs.getUI().show();
    }

}
