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

package sc.fiji.bdvpg.viewer.behaviour;

import bdv.KeyConfigContexts;
import bdv.KeyConfigScopes;
import bdv.util.BdvHandle;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import ch.epfl.biop.bdv.select.ToggleListener;
import org.scijava.plugin.Plugin;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionProvider;
import org.scijava.ui.behaviour.io.gui.CommandDescriptions;
import org.scijava.ui.behaviour.util.Behaviours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.viewer.bdv.config.BdvKeymapHelper;

/**
 * BDV Actions called by default on each BDV Window being created See
 * {@link EditorBehaviourUnInstaller} to remove the default editor and replace
 * by a custom if necessary
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL 2020
 */

public class EditorBehaviourInstaller implements Runnable {

	protected static final Logger logger = LoggerFactory.getLogger(
		EditorBehaviourInstaller.class);

	/**
	 * Command names, i.e. the keys a user edits in the keymap page of the BDV
	 * preferences dialog, and the triggers used when the keymap does not define
	 * them.
	 */
	public static final String REMOVE_SOURCES_FROM_BDV =
		"remove-sources-from-bdv";
	public static final String SOURCES_CONTEXT_MENU = "sources-context-menu";

	public static final String[] REMOVE_SOURCES_FROM_BDV_KEYS = new String[] {
		"DELETE" };
	public static final String[] SOURCES_CONTEXT_MENU_KEYS = new String[] {
		"button3" };

	final SourceSelectorBehaviour ssb;
	final BdvHandle bdvh;

	private ToggleListener toggleListener;

	public EditorBehaviourInstaller(SourceSelectorBehaviour ssb) {
		this.ssb = ssb;
		this.bdvh = ssb.getBdvHandle();
	}

	@Override
	public void run() {
		final InputTriggerConfig config = BdvKeymapHelper.getConfig(bdvh);
		Behaviours editor = new Behaviours(config,
			KeyConfigContexts.BIGDATAVIEWER);

		ClickBehaviour delete = (x, y) -> bdvh.getViewerPanel().state()
			.removeSources(ssb.getSelectedSources());

		editor.behaviour(delete, REMOVE_SOURCES_FROM_BDV,
			REMOVE_SOURCES_FROM_BDV_KEYS);

		editor.behaviour(new SourceContextMenuClickBehaviour(bdvh,
			ssb::getSelectedSources), SOURCES_CONTEXT_MENU,
			SOURCES_CONTEXT_MENU_KEYS);

		// Re-read the triggers when the user edits the keymap, and forward the
		// change to the selector, whose own bindings live in another module
		BdvKeymapHelper.onKeymapChanged(bdvh, () -> {
			editor.updateKeyConfig(config);
			ssb.updateKeyConfig(config);
		});

		toggleListener = new ToggleListener() {

			@Override
			public void isEnabled() {
				bdvh.getViewerPanel().showMessage("Editor Mode");
				// Enable the editor behaviours when the selector is enabled
				editor.install(bdvh.getTriggerbindings(), "sources-editor");
			}

			@Override
			public void isDisabled() {
				bdvh.getViewerPanel().showMessage("Navigation Mode");
				// Disable the editor behaviours the selector is disabled
				bdvh.getTriggerbindings().removeInputTriggerMap("sources-editor");
				bdvh.getTriggerbindings().removeBehaviourMap("sources-editor");
			}
		};

		// One way to chain the behaviour : install and uninstall on source selector
		// toggling:
		// The delete key will act only when the source selection mode is on
		ssb.addToggleListener(toggleListener);

		// Provides a way to retrieve this installer -> can be used to uninstalling
		// it {@link EditorBehaviourUninstaller}
		SourceServices.getBdvDisplayService().setDisplayMetadata(bdvh,
			EditorBehaviourInstaller.class.getSimpleName(), this);

	}

	public ToggleListener getToggleListener() {
		return toggleListener;
	}

	/**
	 * Lists the editor mode commands in the keymap page of the BDV preferences
	 * dialog, next to the commands of BDV itself.
	 */
	@Plugin(type = CommandDescriptionProvider.class)
	public static class Descriptions extends CommandDescriptionProvider {

		public Descriptions() {
			super(KeyConfigScopes.BIGDATAVIEWER, KeyConfigContexts.BIGDATAVIEWER);
		}

		@Override
		public void getCommandDescriptions(final CommandDescriptions descriptions) {
			descriptions.add(REMOVE_SOURCES_FROM_BDV, REMOVE_SOURCES_FROM_BDV_KEYS,
				"Remove the selected sources from the viewer. Editor mode only.");
			descriptions.add(SOURCES_CONTEXT_MENU, SOURCES_CONTEXT_MENU_KEYS,
				"Open the context menu acting on the selected sources. Editor mode only.");
		}
	}

}
