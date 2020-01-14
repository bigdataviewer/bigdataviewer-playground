package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService;


@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Delete Sources")
public class SourcesRemoverCommand implements Command {

    @Parameter
    SourceAndConverter[] sacs;

    @Parameter
    BdvSourceAndConverterService bss;

    @Override
    public void run() {
        for (SourceAndConverter sac:sacs) {
            bss.remove(sac);
        }
    }
}
