package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Remove Sources From BDV",
        description = "Removes one or several sources from an existing BDV window")
public class BdvSourcesRemoverCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter(label="Select Source(s)")
    SourceAndConverter[] srcs_in;

    @Override
    public void run() {
        for (SourceAndConverter src:srcs_in) {
            SourceAndConverterServices.getSourceAndConverterDisplayService().remove(bdvh, src);
        }
    }
}
