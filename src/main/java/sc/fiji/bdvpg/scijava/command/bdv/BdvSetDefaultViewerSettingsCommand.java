package sc.fiji.bdvpg.scijava.command.bdv;

import com.google.gson.Gson;
import ij.Prefs;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.serializers.ScijavaGsonHelper;

import java.util.Arrays;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Set BDV window (default)",
        description = "Set preferences of Bdv Window")
public class BdvSetDefaultViewerSettingsCommand implements BdvPlaygroundActionCommand{

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
            IBdvSupplier bdvSupplier = new DefaultBdvSupplier(new SerializableBdvOptions());
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
            IBdvSupplier bdvSupplier = new DefaultBdvSupplier(options);
            sacDisplayService.setDefaultBdvSupplier(bdvSupplier);
        }

    }
}
