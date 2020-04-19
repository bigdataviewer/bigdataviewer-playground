package sc.fiji.bdvpg.scijava.command.sciview;

import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.iview.SciView;

import java.util.HashMap;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"SciView>Add Sources")
public class SciviewSpimSourceAdderCommand implements Command {

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

    static public void main(String... args) {
        SciView sciview = null;
        try {
            sciview = SciView.createSciView();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ImageJ imagej = new ImageJ(sciview.getScijavaContext());

        HashMap<String, Object> argmap = new HashMap<>();

        imagej.command().run(SciviewSpimSourceAdderCommand.class, true, argmap);
    }
}
