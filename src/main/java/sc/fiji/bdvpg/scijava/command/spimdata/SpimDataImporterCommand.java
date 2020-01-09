package sc.fiji.bdvpg.scijava.command.spimdata;

import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.spimdata.importer.SpimDataImporterXML;

import java.io.File;

@Plugin( type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"SpimDataset>Open XML/HDF5 File" )
public class SpimDataImporterCommand implements Command {

    @Parameter
    File f;

    @Parameter(type = ItemIO.OUTPUT)
    AbstractSpimData spimData;

    public void run() {
        spimData = new SpimDataImporterXML(f).get();
    }

}
