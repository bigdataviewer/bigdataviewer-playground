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

package sc.fiji.bdvpg.command.view.bdv.settings;

import bdv.util.BdvHandle;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.viewers.behaviour.EditorBehaviourInstaller;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceBdvDisplayService;
import sc.fiji.bdvpg.services.SourceServices;

import javax.swing.SwingUtilities;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
                                              // are set by SciJava
                                              // pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menu = {
			@Menu(label = ScijavaBdvDefaults.RootMenuL1),
			@Menu(label = ScijavaBdvDefaults.RootMenuL2),
			@Menu(label = ScijavaBdvDefaults.ViewMenu, weight = ScijavaBdvDefaults.ViewW),
			@Menu(label = "BDV"),
			@Menu(label = "Settings"),
			@Menu(label = "BDV - Add Editor", weight = 7)
	},
	description = "Installs the source selection editor on BDV windows. " +
		"Press the toggle key to switch between navigation and editor mode.")
public class BdvEditorInstallCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select BDV Windows",
		description = "The BigDataViewer windows where the editor will be installed",
		persist = false)
	BdvHandle[] bdvhs;

	@Parameter(label = "Toggle key",
		description = "Keyboard shortcut to toggle between navigation and editor mode")
	String toggle_key = "E";

	@Parameter
	SourceBdvDisplayService bdv_display_service;

	@Override
	public void run() {
		SwingUtilities.invokeLater(() -> {
			for (BdvHandle bdvh : bdvhs) {
				// Skip if an editor is already installed on this window
				if (SourceServices.getBdvDisplayService().getDisplayMetadata(
					bdvh, EditorBehaviourInstaller.class.getSimpleName()) != null)
				{
					continue;
				}
				SourceSelectorBehaviour ssb = new SourceSelectorBehaviour(bdvh,
					toggle_key);
				bdv_display_service.setDisplayMetadata(bdvh,
					SourceSelectorBehaviour.class.getSimpleName(), ssb);
				new EditorBehaviourInstaller(ssb).run();
			}
		});
	}
}
