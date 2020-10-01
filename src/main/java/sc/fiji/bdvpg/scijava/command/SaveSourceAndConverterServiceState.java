package sc.fiji.bdvpg.scijava.command;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;

import java.io.File;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Save Bdv Playground State")
public class SaveSourceAndConverterServiceState implements Command {

    @Parameter
    File file;

    @Override
    public void run() {
        new SourceAndConverterServiceSaver(file).run();
    }
}
