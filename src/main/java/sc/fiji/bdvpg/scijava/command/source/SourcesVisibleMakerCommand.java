package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterDisplayService;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService;


@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Make Sources Visible")
public class SourcesVisibleMakerCommand implements Command {

    @Parameter
    SourceAndConverter[] sacs;

    @Parameter
    BdvSourceAndConverterDisplayService bsds;

    @Override
    public void run() {
        for (SourceAndConverter sac:sacs) {
            bsds.makeVisible(sac);
        }
    }
}
