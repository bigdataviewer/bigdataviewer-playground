package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.Source;
import net.imagej.display.ColorTables;
import net.imagej.lut.LUTService;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorTable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.convert.ConvertService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Set Source LUT", initializer = "init")

public class SourceConverterCommand extends DynamicCommand {

    @Parameter
    LUTService lutService;

    @Parameter(label = "LUT name", persist = false, callback = "nameChanged")
    String choice = "Gray";

    @Parameter(required = false, label = "LUT", persist = false)
    ColorTable table = ColorTables.GRAYS;

    @Parameter
    ConvertService cs;

    // -- other fields --
    private Map<String, URL> luts = null;

    @Parameter
    Source source;

    @Parameter
    BdvSourceDisplayService bsds;

    @Override
    public void run() {
        Converter bdvLut = cs.convert(table, Converter.class);
        bsds.updateConverter(source, bdvLut);

    }

    // -- initializers --

    protected void init() {
        luts = lutService.findLUTs();
        final ArrayList<String> choices = new ArrayList<>();
        for (final Map.Entry<String, URL> entry : luts.entrySet()) {
            choices.add(entry.getKey());
        }
        Collections.sort(choices);
        final MutableModuleItem<String> input =
                getInfo().getMutableInput("choice", String.class);
        input.setChoices(choices);
        input.setValue(this, choices.get(0));
        nameChanged();
    }

    // -- callbacks --

    protected void nameChanged() {
        try {
            table = lutService.loadLUT(luts.get(choice));
        }
        catch (final Exception e) {
            // nada
        }
    }
}
