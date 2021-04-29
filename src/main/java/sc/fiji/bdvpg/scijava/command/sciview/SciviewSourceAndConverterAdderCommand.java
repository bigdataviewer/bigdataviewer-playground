package sc.fiji.bdvpg.scijava.command.sciview;

import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.iview.SciView;

import java.util.HashMap;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"SciView>Add SourceAndConverters")
public class SciviewSourceAndConverterAdderCommand implements Command {

    @Parameter
    SciView sciview;

    @Parameter
    SourceAndConverter[] sacs;

    @Parameter
    int numtimepoints;

    @Override
    public void run() {
        for (SourceAndConverter sac : sacs) {
            sciview.addVolume(sac, numtimepoints,sac.getSpimSource().getName());
        }
    }

    static public void main(String... args) {
        SciView sciview = null;
        try {
            sciview = SciView.create();//.createSciView();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ImageJ imagej = new ImageJ(sciview.getScijavaContext());

        HashMap<String, Object> argmap = new HashMap<>();

        imagej.command().run(SciviewSourceAndConverterAdderCommand.class, true, argmap);
    }
}
