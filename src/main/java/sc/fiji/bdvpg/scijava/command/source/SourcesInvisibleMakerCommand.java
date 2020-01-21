package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;


@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Make Sources Invisible")
public class SourcesInvisibleMakerCommand implements Command {

    @Parameter
    SourceAndConverter[] sacs;

    @Parameter
    SourceAndConverterBdvDisplayService bsds;

    @Override
    public void run() {
        for (SourceAndConverter sac:sacs) {
            bsds.makeInvisible(sac);
        }
    }
}
