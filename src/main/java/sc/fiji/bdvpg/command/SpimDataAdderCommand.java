package sc.fiji.bdvpg.command;

import bdv.util.BdvHandle;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.source.append.AddSpimdataToBdv;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

@Plugin( type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"SpimData>Add SpimData to Bdv" )
public class SpimDataAdderCommand implements Command {

    @Parameter
    AbstractSpimData spimData;

    @Parameter(type = ItemIO.BOTH)
    BdvHandle bdvh;

    public void run() {
        (new AddSpimdataToBdv(bdvh,spimData)).run();
    }

}
