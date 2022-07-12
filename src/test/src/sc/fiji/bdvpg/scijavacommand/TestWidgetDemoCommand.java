package sc.fiji.bdvpg.scijavacommand;

import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.WarpedSourceDemo;


@Plugin(type = Command.class, menuPath = "Test>Sorted Sources")
public class TestWidgetDemoCommand implements Command {

    @Parameter
    SourceAndConverter[] non_sorted_sources;


    @Parameter(style = "sorted")
    SourceAndConverter[] sorted_sources;

    @Override
    public void run() {
        IJ.log("--- Non Sorted");
        for(SourceAndConverter source: non_sorted_sources) {
            IJ.log(source.getSpimSource().getName());
        }

        IJ.log("--- Sorted");
        for(SourceAndConverter source: sorted_sources) {
            IJ.log(source.getSpimSource().getName());
        }
    }


    public static void main(String... args) throws Exception {
        // Initializes static SourceService and Display Service

        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        WarpedSourceDemo.demo();
        ij.command().run(TestWidgetDemoCommand.class, true);

    }
}
