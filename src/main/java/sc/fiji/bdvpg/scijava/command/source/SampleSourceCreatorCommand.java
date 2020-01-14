package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.log.SystemLogger;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.importer.MandelbrotSourceGetter;
import sc.fiji.bdvpg.sourceandconverter.importer.VoronoiSourceGetter;
import sc.fiji.bdvpg.sourceandconverter.importer.Wave3DSourceGetter;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Create Sample Source")
public class SampleSourceCreatorCommand implements Command {

    @Parameter(choices = {"Mandelbrot", "Wave3D", "Voronoi", "Big Voronoi"})
    String sampleName;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter sampleSource;

    @Override
    public void run() {
        switch(sampleName) {

            case "Mandelbrot":
                sampleSource = (new MandelbrotSourceGetter()).get();
                break;

            case "Wave3D":
                sampleSource = (new Wave3DSourceGetter()).get();
                break;

            case "Voronoi":
                sampleSource = (new VoronoiSourceGetter(new long[]{512, 512, 1}, 256, true).get());
                break;

            case "Big Voronoi":
                sampleSource = (new VoronoiSourceGetter(new long[]{2048, 2048, 2048}, 65536, false).get());
                break;

            default:
                new SystemLogger().err("Invalid sample name");
                return;
        }
    }
}
