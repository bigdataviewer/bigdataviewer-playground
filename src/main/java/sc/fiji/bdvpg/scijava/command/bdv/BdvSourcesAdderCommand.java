package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterDisplayService;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Append Sources To Bdv")
public class BdvSourcesAdderCommand implements Command {

    @Parameter
    BdvHandle bdvh;

    @Parameter
    SourceAndConverter[] srcs_in;

    @Parameter
    BdvSourceAndConverterDisplayService bsds;

    @Parameter
    boolean autoContrast;

    @Parameter
    boolean adjustViewOnSource;

    @Override
    public void run() {
        for (SourceAndConverter src:srcs_in) {
            bsds.show(bdvh, src);
            int timepoint = bdvh.getViewerPanel().getState().getCurrentTimepoint();
            if (autoContrast) {
                new BrightnessAutoAdjuster(src, timepoint).run();
            }
        }
        if ((adjustViewOnSource)&& (srcs_in.length>0)) {
            new ViewerTransformAdjuster(bdvh, srcs_in[0]).run();
        }
    }
}
