package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterDisplayService;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Append Source To Bdv")
public class BdvSourceAdderCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter
    SourceAndConverter src;

    @Parameter
    BdvSourceAndConverterDisplayService bsds;

    @Override
    public void run() {
        bsds.show(bdvh, src);
    }
}
