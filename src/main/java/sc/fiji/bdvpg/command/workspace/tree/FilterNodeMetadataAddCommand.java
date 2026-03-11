/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.command.workspace.tree;

import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvPgMenus;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.service.SourceService;
import sc.fiji.bdvpg.scijava.service.tree.FilterNode;
import sc.fiji.bdvpg.scijava.service.tree.SourceTreeModel;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
		menu = {
				@Menu(label = BdvPgMenus.L1),
				@Menu(label = BdvPgMenus.L2),
				@Menu(label = BdvPgMenus.WorkspaceMenu, weight = BdvPgMenus.WorkspaceW),
				@Menu(label = "Tree", weight = 1),
				@Menu(label = "Tree - Filter By Metadata", weight = -5)
		},
		//menuPath = ScijavaBdvDefaults.RootMenu + "Workspace>Tree - Make Metadata Filter Node",
	description = "Adds a node in the tree view which selects the sources which contain a certain key metadata and which matches a certain regular expression")

public class FilterNodeMetadataAddCommand implements
	BdvPlaygroundActionCommand
{

	@Parameter(label = "Name of the node",
			description = "Display name for the filter node in the tree view")
	String group_name;

	@Parameter(label = "Metadata Key",
			description = "The metadata key to filter sources by")
	String key;

	@Parameter(label = "Value regex",
		description = "Regular expression to match metadata values (\".*\" matches everything)")
	String value_regex = ".*";

	@Parameter
	SourceService source_service;

	@Override
	public void run() {
		FilterNode filterNode = new FilterNode(group_name, (source) -> {
			if (source_service.containsMetadata(source, key)) {
				Object o = source_service.getMetadata(source, key);
				if (o instanceof String) {
					String str = (String) o;
					return str.matches(value_regex);
				}
				else return false;
			}
			else return false;
		}, false);
		SourceTreeModel model = source_service.tree().getSourceTreeModel();
		model.addNode(model.getRoot(), filterNode);
	}
}

