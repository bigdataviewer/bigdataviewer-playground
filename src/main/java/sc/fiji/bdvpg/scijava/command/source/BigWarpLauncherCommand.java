package sc.fiji.bdvpg.scijava.command.source;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Launch BigWarp",
        label = "Starts BigWarp from existing sources")
public class BigWarpLauncherCommand implements Command {

    @Parameter
    String bigWarpName;

    @Parameter
    SourceAndConverter[] movingSources;

    @Parameter
    SourceAndConverter[] fixedSources;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvhQ;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvhP;

    @Parameter
	SourceAndConverterBdvDisplayService bsds;

    public void run() {
        List<SourceAndConverter> movingSacs = Arrays.stream(movingSources).collect(Collectors.toList());
        List<SourceAndConverter> fixedSacs = Arrays.stream(fixedSources).collect(Collectors.toList());

        List<ConverterSetup> converterSetups = Arrays.stream(movingSources).map(src -> bsds.getConverterSetup(src)).collect(Collectors.toList());
        converterSetups.addAll(Arrays.stream(fixedSources).map(src -> bsds.getConverterSetup(src)).collect(Collectors.toList()));

        // Launch BigWarp
        BigWarpLauncher bwl = new BigWarpLauncher(movingSacs, fixedSacs, bigWarpName, converterSetups);
        bwl.run();

        // Output bdvh handles -> will be put in the object service
        bdvhQ = bwl.getBdvHandleQ();
        bdvhP = bwl.getBdvHandleP();

        bsds.pairClosing(bdvhQ,bdvhP);

    }

}
