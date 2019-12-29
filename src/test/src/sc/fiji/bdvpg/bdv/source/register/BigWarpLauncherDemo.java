package sc.fiji.bdvpg.bdv.source.register;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.source.append.SourceBdvAdder;
import sc.fiji.bdvpg.source.importer.samples.MandelbrotSourceGetter;
import sc.fiji.bdvpg.source.importer.samples.VoronoiSourceGetter;
import sc.fiji.bdvpg.source.register.BigWarpLauncher;

public class BigWarpLauncherDemo {
    public static void main(String... args) {
        // Makes a BDV window (holding sample sources)
        BdvHandle bdvh = new BdvCreator(false).get();

        // Builds the source adder
        SourceBdvAdder adder = new SourceBdvAdder(bdvh, null);

        // Gets sample sources
        Source voronoi = new VoronoiSourceGetter(new long[]{512,512,1}, 256, true).get();
        Source mandelbrot = new MandelbrotSourceGetter().get();

        // Puts voronoi source to bdv window and gets bdvstacksource and adjust display settings
        BdvStackSource bdvs_voronoi = adder.apply(voronoi);
        bdvs_voronoi.setDisplayRangeBounds(0,255);
        bdvs_voronoi.setColor(new ARGBType(ARGBType.rgba(255,0,0,255)));

        // Puts mandelbrot source to bdv window and gets bdvstacksource and adjust display settings
        BdvStackSource bdvs_mandelbrot = adder.apply(mandelbrot);
        bdvs_mandelbrot.setDisplayRangeBounds(0,255);
        bdvs_mandelbrot.setColor(new ARGBType(ARGBType.rgba(0,255,0,255)));

        // Initializes BigWarp with SourceAndConverters retrieved from the BdvStackSources
        // With SourceAndConverter

        ConverterSetup cs;

        BigWarpLauncher bwl = new BigWarpLauncher(
                (SourceAndConverter) bdvs_mandelbrot.getSources().get(0),
                (SourceAndConverter) bdvs_mandelbrot.getSources().get(0),
                "BigWarpDemo"
                );

        // With Source
        /*
        BigWarpLauncher bwl = new BigWarpLauncher(
                ((SourceAndConverter) bdvs_mandelbrot.getSources().get(0)).getSpimSource(),
                ((SourceAndConverter) bdvs_voronoi.getSources().get(0)).getSpimSource(),
                "BigWarpDemo"
        );*/

        // Launches BigWarp
        bwl.run();

    }
}
