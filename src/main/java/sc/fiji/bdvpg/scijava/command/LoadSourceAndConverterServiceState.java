package sc.fiji.bdvpg.scijava.command;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServiceLoader;

import java.io.File;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Load Bdv Playground State")
public class LoadSourceAndConverterServiceState implements Command {

    @Parameter
    File file;

    @Override
    public void run() {
        new SourceAndConverterServiceLoader(file.getAbsolutePath()).run();
    }
}
