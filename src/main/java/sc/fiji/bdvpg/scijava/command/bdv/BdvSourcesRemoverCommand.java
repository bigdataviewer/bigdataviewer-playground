package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Remove Sources From Bdv")
public class BdvSourcesRemoverCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter
    SourceAndConverter[] srcs_in;

    @Parameter
	SourceAndConverterBdvDisplayService bsds;

    @Override
    public void run() {
        for (SourceAndConverter src:srcs_in) {
            bsds.remove(bdvh, src);
        }
    }
}
