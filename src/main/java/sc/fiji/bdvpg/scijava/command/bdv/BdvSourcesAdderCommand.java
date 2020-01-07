package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterDisplayService;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Append Sources To Bdv")
public class BdvSourcesAdderCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter
    SourceAndConverter[] srcs_in;

    @Parameter
    BdvSourceAndConverterDisplayService bsds;

    @Override
    public void run() {
        for (SourceAndConverter src:srcs_in) {
            bsds.show(bdvh, src);
        }
    }
}
