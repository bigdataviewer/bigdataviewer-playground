package sc.fiji.bdvpg.scijava.command.spimdata;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.exporter.XmlFromSpimDataExporter;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.io.File;

@Plugin( type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDVDataset>Save BDVDataset" )
public class SpimDataExporterCommand implements Command {

    // To get associated spimdata
    @Parameter(label = "Select source(s)")
    SourceAndConverter sac;

    @Parameter(label = "Output File (XML)")
    public File xmlFilePath;

    public void run() {

        if (SourceAndConverterServices.getSourceAndConverterService()
                .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)==null) {
            System.err.println("No BDVDataset associated to the chosen source - Aborting save command");
            return;
        }

        AbstractSpimData asd =
                ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)).asd;

        new XmlFromSpimDataExporter(asd, xmlFilePath.getAbsolutePath()).run();
    }

}
