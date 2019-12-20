package sc.fiji.bdvpg.command;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.source.append.SourceBdvAdder;
import sc.fiji.bdvpg.bdv.source.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.log.SystemLogger;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.source.importer.samples.MandelbrotSourceGetter;
import sc.fiji.bdvpg.source.importer.samples.VoronoiSourceGetter;
import sc.fiji.bdvpg.source.importer.samples.Wave3DSourceGetter;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Append Sample Source to Bdv Window")
public class BdvAppendSampleCommand implements Command {

    @Parameter(type = ItemIO.BOTH)
    public BdvHandle bdvh;

    @Parameter(choices = {"Mandelbrot", "Wave3D", "Voronoi", "Big Voronoi"})
    public String sampleName;

    @Override
    public void run() {
        Source src;
        switch(sampleName) {

            case "Mandelbrot":
                src = (new MandelbrotSourceGetter()).get();
                break;

            case "Wave3D":
                src = (new Wave3DSourceGetter()).get();
                break;

            case "Voronoi":
                src = (new VoronoiSourceGetter(new long[]{512,512,1}, 256, true).get());
                break;

            case "Big Voronoi":
                src = (new VoronoiSourceGetter(new long[]{2048,2048,2048}, 65536, false).get());
                break;

            default:
                new SystemLogger().err("Invalid sample name");
                return;
        }
        new SourceBdvAdder(bdvh, src).run();

        new BrightnessAutoAdjuster(bdvh, src, 0.01, 0.99).run();
    }
}
