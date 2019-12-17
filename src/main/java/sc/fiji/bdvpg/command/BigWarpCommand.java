package sc.fiji.bdvpg.command;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.source.get.GetSourceAndConverterByIndexFromBdv;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.source.register.BigWarpLauncher;

import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BigWarp>Launch BigWarp",
        label = "Starts BigWarp from existing sources")
public class BigWarpCommand implements Command {

    @Parameter
    String bigWarpName;

    @Parameter
    BdvHandle bdvhMovingSource;

    @Parameter(label="Source Index, comma separated, range allowed '0:10'")
    public String sourceIndexStringMovingSource = "0";

    @Parameter
    BdvHandle bdvhFixedSource;

    @Parameter(label="Source Index, comma separated, range allowed '0:10'")
    public String sourceIndexStringFixedSource = "0";


    GetSourceAndConverterByIndexFromBdv getter;

    public void run() {
        // Get Sources
        // Moving
        List<Integer> idxMovingSources = CommandHelper.commaSeparatedListToArray(sourceIndexStringMovingSource);
        getter = new GetSourceAndConverterByIndexFromBdv(bdvhMovingSource, -1); // Index not relevant when using Functional Interface
        List<SourceAndConverter> movingSources = idxMovingSources.stream().map(idx -> getter.apply(idx)).collect(Collectors.toList());

        // Fixed
        List<Integer> idxFixedSources = CommandHelper.commaSeparatedListToArray(sourceIndexStringFixedSource);
        getter = new GetSourceAndConverterByIndexFromBdv(bdvhFixedSource, -1); // Index not relevant when using Functional Interface
        List<SourceAndConverter> fixedSources = idxFixedSources.stream().map(idx -> getter.apply(idx)).collect(Collectors.toList());

        // Launch BigWarp
        new BigWarpLauncher(movingSources, fixedSources, bigWarpName).run();
    }

}
