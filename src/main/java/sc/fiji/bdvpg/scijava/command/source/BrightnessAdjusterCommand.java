package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SacServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAdjuster;

import java.text.DecimalFormat;

import static org.scijava.ItemVisibility.MESSAGE;

@Plugin(type = Command.class, initializer = "init",  menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Set Sources Brightness")
public class BrightnessAdjusterCommand extends InteractiveCommand {

    @Parameter
    SourceAndConverter[] sources;

    @Parameter(visibility=MESSAGE, required=false, style = "text field")
    String message = "Display Range [ NaN - NaN ]";

    @Parameter(callback = "updateMessage")
    double min;

    @Parameter(callback = "updateMessage")
    double max;

    @Parameter(style = "slider", min = "0", max = "1000", callback = "updateMessage")
    double minSlider;

    @Parameter(style = "slider", min = "0", max = "1000", callback = "updateMessage")
    double maxSlider;

    boolean firstTimeCalled = true;
    boolean secondTimeCalled = true;

    public void run() {
        if ((!firstTimeCalled)&&(!secondTimeCalled)) {
            double minValue = min + minSlider/1000.0*(max-min);
            double maxValue = min + maxSlider/1000.0*(max-min);
            for (SourceAndConverter source:sources) {
                new BrightnessAdjuster(source, minValue, maxValue).run();
            }
        } else {
            init();
            if (firstTimeCalled) {
                firstTimeCalled = false;
            } else if (secondTimeCalled) {
                secondTimeCalled = false;
            }
        }
    }

    DecimalFormat formatter = new DecimalFormat("#.###");

    public void updateMessage() {
        formatter.setMinimumFractionDigits(3);
        double minValue = min + minSlider/1000.0*(max-min);
        double maxValue = min + maxSlider/1000.0*(max-min);
        message = "Display Range ["+ formatter.format(minValue) +" - "+ formatter.format(maxValue) +"]";
    }


    public void init() {
        if (sources.length>0) {
            double minSource = SacServices.getSacDisplayService().getConverterSetup(sources[0]).getDisplayRangeMin();
            double maxSource = SacServices.getSacDisplayService().getConverterSetup(sources[0]).getDisplayRangeMax();

            if (minSource>=0) {
                min = 0;
            } else {
                min = minSource;
            }
            if (maxSource>65535) {
                max = maxSource;
            } else if (maxSource>255) {
                max = 65535;
            } else if (maxSource>1){
                max = 255;
            } else {
                max = 1;
            }
            minSlider = (minSource-min)/(max-min)*1000;
            maxSlider = (maxSource-min)/(max-min)*1000;
            message = "Display Range [ NaN - NaN ]";
        }
    }
}
