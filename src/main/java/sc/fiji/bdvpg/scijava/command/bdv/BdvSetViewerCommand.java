package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.AxisOrder;
import bdv.viewer.render.AccumulateProjectorFactory;
import com.google.gson.Gson;
import ij.Prefs;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.projector.DefaultAccumulatorFactory;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.serializers.ScijavaGsonHelper;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.stream.Collectors;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Set BDV window (default)",
        description = "Creates an empty BDV window")
public class BdvSetViewerCommand implements BdvPlaygroundActionCommand{

    @Parameter(label = "Click this checkbox to ignore all parameters and reset the default viewer", persist = false)
    boolean resetToDefault = false;

    @Parameter
    int width = 640;

    @Parameter
    int height = 480;

    @Parameter
    String screenscales = "1, 0.75, 0.5, 0.25, 0.125";

    @Parameter
    long targetrenderms = 30;// * 1000000l;

    @Parameter
    int numrenderingthreads = 3;

    @Parameter
    int numsourcegroups = 10;

    @Parameter
    String frametitle = "BigDataViewer";

    @Parameter
    boolean is2d = false;

    @Parameter
    boolean interpolate = false;

    @Parameter
    int numtimepoints = 1;

    //@Parameter
    //AxisOrder axisOrder = AxisOrder.DEFAULT;

    //AccumulateProjectorFactory<ARGBType> accumulateProjectorFactory = new DefaultAccumulatorFactory();

    @Parameter
    Context ctx;

    @Parameter
    SourceAndConverterBdvDisplayService sacDisplayService;

    @Override
    public void run() {
        if (resetToDefault) {
            Gson gson = ScijavaGsonHelper.getGson(ctx);
            IBdvSupplier bdvSupplier = new DefaultBdvSupplier(new SerializableBdvOptions());
            String defaultBdvViewer = gson.toJson(new DefaultBdvSupplier(new SerializableBdvOptions()));
            Prefs.set("default_bigdataviewer", defaultBdvViewer);
            sacDisplayService.setDefaultBdvSupplier(bdvSupplier);
        } else {
            SerializableBdvOptions options = new SerializableBdvOptions();
            options.frameTitle = frametitle;
            options.is2D = is2d;
            options.numRenderingThreads = numrenderingthreads;
            options.screenScales = Arrays.stream(screenscales.split(",")).mapToDouble(Double::parseDouble).toArray();
            options.height = height;
            options.width = width;
            options.numSourceGroups = numsourcegroups;
            options.numTimePoints = numtimepoints;
            options.interpolate = interpolate;
            Gson gson = ScijavaGsonHelper.getGson(ctx);
            IBdvSupplier bdvSupplier = new DefaultBdvSupplier(options);
            // Update creator of new viewer immediatedly
            sacDisplayService.setDefaultBdvSupplier(bdvSupplier);
            String bdvSupplierSerialized = gson.toJson(bdvSupplier);
            // Saved in prefs for next session
            Prefs.set("default_bigdataviewer", bdvSupplierSerialized);
        }

    }
}
