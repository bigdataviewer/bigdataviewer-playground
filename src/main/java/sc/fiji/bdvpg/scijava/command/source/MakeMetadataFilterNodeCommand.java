/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Make Metadata Filter Node",
description = "Adds a node in the tree view which selects the sources which contain a certain key metadata and which matches a certain regular expression")

public class MakeMetadataFilterNodeCommand implements Command {

    @Parameter(label = "Name of the node")
    String groupName;

    @Parameter(label = "Select Metadata Key")
    String metadata_key;

    @Parameter(label = "Regular expression for Metadata Value (\".*\" matches everything)")
    String metadata_value_regex = ".*";

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {
        SourceFilterNode sfn = new SourceFilterNode(sac_service.getUI().getTreeModel(),
                groupName,
                (sac) -> {
                    if (sac_service.containsMetadata(sac, metadata_key)) {
                        Object o = sac_service.getMetadata(sac, metadata_key);
                        if ((o!=null)||(o instanceof String)) {
                            String str = (String) o;
                            return str.matches(metadata_value_regex);
                        } else return false;
                    } else return false;
                },
                false);
        sac_service.getUI().addNode(sfn);
    }
}
