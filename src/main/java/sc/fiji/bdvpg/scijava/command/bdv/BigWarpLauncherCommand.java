package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;
import sc.fiji.bdvpg.scijava.services.BdvSourceService;
import sc.fiji.bdvpg.source.register.BigWarpLauncher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BigWarp>Launch BigWarp",
        label = "Starts BigWarp from existing sources")
public class BigWarpLauncherCommand implements Command {

    @Parameter
    String bigWarpName;

    @Parameter
    Source[] movingSources;

    @Parameter
    Source[] fixedSources;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvhQ;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvhP;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter[] warpedSources;

    @Parameter
    ConvertService cs;

    @Parameter
    BdvSourceDisplayService bsds;

    public void run() {
        List<SourceAndConverter> movingSacs = Arrays.stream(movingSources).map(src -> cs.convert(src, SourceAndConverter.class)).collect(Collectors.toList());
        List<SourceAndConverter> fixedSacs = Arrays.stream(fixedSources).map(src -> cs.convert(src, SourceAndConverter.class)).collect(Collectors.toList());

        List<ConverterSetup> converterSetups = Arrays.stream(movingSources).map(src -> bsds.getConverterSetup(src)).collect(Collectors.toList());
        converterSetups.addAll(Arrays.stream(fixedSources).map(src -> bsds.getConverterSetup(src)).collect(Collectors.toList()));

        // Launch BigWarp
        BigWarpLauncher bwl = new BigWarpLauncher(movingSacs, fixedSacs, bigWarpName, converterSetups);
        bwl.run();

        // Output bdvh handles -> will be put in the object service
        bdvhQ = bwl.getBdvHandleQ();
        bdvhP = bwl.getBdvHandleP();

        bsds.pairClosing(bdvhQ,bdvhP);

        // TODO
        warpedSources = new SourceAndConverter[movingSources.length];

        for (int i=0;i<warpedSources.length;i++) {
            warpedSources[i] = bdvhP.getViewerPanel().getState().getSources().get(i);
        }
    }

}
