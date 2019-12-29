package sc.fiji.bdvpg.command;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;


@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Append Source To Bdv")
public class BdvSourceAdderCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter
    Source src;

    @Parameter
    BdvSourceDisplayService bsds;

    @Override
    public void run() {
        bsds.show(bdvh, src);
    }
}
