package sc.fiji.bdvpg.command;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import com.google.common.collect.Lists;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.source.display.BrightnessAdjuster;
import sc.fiji.bdvpg.bdv.source.get.GetSourceByIndexFromBdv;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

import java.util.List;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Set Sources Min Max Display")
public class BdvBrightnessAdjusterCommand implements Command{

    @Parameter
    BdvHandle bdvh;

    @Parameter(label="Source Index, comma separated, range allowed '0:10'")
    public String sourceIndexString = "0";

    @Parameter
    double min;

    @Parameter
    double max;

    public void run() {
        List<Integer> indexes = CommandHelper.commaSeparatedListToArray(sourceIndexString);
        //GetSourceByIndexFromBdv getter = new GetSourceByIndexFromBdv(bdvh, -1); // Index not relevant when using Functional Interface
        //List<Source> sources = Lists.transform(indexes, getter::apply);

        BrightnessAdjuster ba = new BrightnessAdjuster(bdvh,null,min,max);
        //sources.forEach(ba::accept);
    }

}
