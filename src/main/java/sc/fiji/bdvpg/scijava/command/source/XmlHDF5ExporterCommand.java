package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.exporter.XmlHDF5SpimdataExporter;

import java.io.File;
import java.util.Arrays;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Export Sources to XML/HDF5 Spimdataset")
public class XmlHDF5ExporterCommand implements Command {

    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sacs;
    @Parameter
    int nThreads = 4;
    @Parameter
    int timePointBegin = 0;
    @Parameter
    int timePointEnd = 1;
    @Parameter
    int scaleFactor = 4;
    @Parameter
    int blockSizeX = 64;
    @Parameter
    int blockSizeY = 64;
    @Parameter
    int blockSizeZ = 64;
    @Parameter
    File xmlFile;

    @Override
    public void run() {
        new XmlHDF5SpimdataExporter(Arrays.asList(sacs),nThreads,timePointBegin,timePointEnd,scaleFactor,blockSizeX,blockSizeY,blockSizeZ,xmlFile).run();
    }


}
