package sc.fiji.bdvpg.scijava.command.sciview;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.iview.SciView;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"SciView>Add Sources")
public class SciviewSourcesAdderCommand implements Command {

    @Parameter
    SciView sciView;

    @Parameter
    SourceAndConverter[] sacs;

    @Parameter
    int timePoint;

    @Parameter
    int mipmapLevel;

    @Override
    public void run() {
        for (SourceAndConverter sac : sacs) {
            sciView.addVolume(sac.getSpimSource().getSource(timePoint, mipmapLevel), sac.getSpimSource().getName(), "");
        }
    }
}
