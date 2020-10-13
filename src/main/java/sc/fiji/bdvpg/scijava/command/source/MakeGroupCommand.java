package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceFilterNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Make Global Source Group",
description = "Adds a node in the tree view which selects the sources specified in the command")

public class MakeGroupCommand implements Command {

    @Parameter(label = "Name of the group")
    String groupName;

    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sacs;

    @Parameter(label = "Display Sources")
    boolean displaySources;

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {
        final Set<SourceAndConverter> sacs_set = new HashSet<>(Arrays.asList(sacs));
        SourceFilterNode sfn = new SourceFilterNode(sac_service.getUI().getTreeModel(), groupName, sacs_set::contains, displaySources);
        sac_service.getUI().addNode(sfn);
    }
}
