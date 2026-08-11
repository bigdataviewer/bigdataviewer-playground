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

package sc.fiji.bdvpg.viewer.bdv.config;

import bdv.BigDataViewer;
import bdv.KeyConfigContexts;
import bdv.ui.keymap.Keymap;
import bdv.ui.keymap.KeymapManager;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

import javax.swing.SwingUtilities;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single entry point for the key and mouse bindings of the BDV windows created
 * by BigDataViewer-Playground.
 * <p>
 * Bindings are stored as YAML files by BDV's own {@link KeymapManager}, under
 * {@code <configDir>/keymaps/}, where {@code configDir} defaults to
 * {@link BigDataViewer#configDir}. A {@code keymaps.yaml} index lists the
 * user-defined keymaps and records which one is selected; each keymap is one
 * further YAML file mapping command names to triggers. Users normally do not
 * edit those files by hand: the keymap page of the BDV preferences dialog
 * (bound to {@code ctrl COMMA}) is a graphical editor for them.
 * <p>
 * All playground windows share a single {@link KeymapManager} so that editing
 * the keymap in one window applies to every other one, and so that the
 * selection is saved once. Suppliers must declare it with
 * {@link #applyTo(BdvOptions)}; behaviours installed on an existing window must
 * read their triggers from {@link #getConfig(BdvHandle)} and refresh them from
 * {@link #onKeymapChanged(BdvHandle, Runnable)}.
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class BdvKeymapHelper {

	private static KeymapManager keymapManager;

	/**
	 * @return the {@link KeymapManager} shared by all playground BDV windows,
	 *         created on first access from the BDV config directory.
	 */
	public static synchronized KeymapManager getKeymapManager() {
		if (keymapManager == null) {
			keymapManager = new KeymapManager(BigDataViewer.configDir);
		}
		return keymapManager;
	}

	/**
	 * Overrides the shared {@link KeymapManager}, for instance to store the
	 * keymaps somewhere else than {@link BigDataViewer#configDir}. Windows which
	 * have already been created keep the previous one.
	 *
	 * @param manager the manager to share, or null to fall back to the default
	 */
	public static synchronized void setKeymapManager(KeymapManager manager) {
		keymapManager = manager;
	}

	/**
	 * @return the keymap currently selected in the shared manager. This is a
	 *         stable object which follows the selection, so it is safe to keep a
	 *         reference to it and to register listeners on it.
	 */
	public static Keymap getKeymap() {
		return getKeymapManager().getForwardSelectedKeymap();
	}

	/**
	 * @param bdvh a BDV window
	 * @return the keymap of the manager this particular window was built with,
	 *         which is the shared one for windows created by the playground
	 *         suppliers.
	 */
	public static Keymap getKeymap(BdvHandle bdvh) {
		return bdvh.getKeymapManager().getForwardSelectedKeymap();
	}

	/**
	 * @param bdvh a BDV window
	 * @return the key config to hand to a {@code Behaviours} or {@code Actions}
	 *         being installed on this window, so that its triggers become user
	 *         configurable.
	 */
	public static InputTriggerConfig getConfig(BdvHandle bdvh) {
		return getKeymap(bdvh).getConfig();
	}

	/**
	 * Declares the shared keymap on {@code options}, so that the resulting
	 * window uses it and shares it with the other playground windows.
	 * <p>
	 * Both the manager and the config have to be set:
	 * {@code BdvHandleFrame#createViewer} passes an explicit
	 * {@link InputTriggerConfig} to {@link BigDataViewer}, which then ignores the
	 * config of the keymap. Setting the config here to the keymap's own means
	 * the window starts with exactly the bindings the preferences dialog
	 * displays, instead of falling back to the triggers hardcoded in the source.
	 *
	 * @param options the options of the window about to be created
	 * @return {@code options}, with the keymap declared
	 */
	public static BdvOptions applyTo(BdvOptions options) {
		final KeymapManager manager = getKeymapManager();
		return options.keymapManager(manager).inputTriggerConfig(manager
			.getForwardSelectedKeymap().getConfig());
	}

	/**
	 * Gives the triggers a command is currently bound to, to label a button or a
	 * tooltip with the shortcut which actually applies rather than with a
	 * hardcoded one. Combine with
	 * {@link #onKeymapChanged(BdvHandle, Runnable)} to keep the label right
	 * after the user rebinds the command.
	 *
	 * @param bdvh the window whose keymap is queried
	 * @param commandName the name of the command, as declared to
	 *          {@code Behaviours} or {@code Actions}
	 * @param defaults what the command is bound to when the keymap does not
	 *          mention it
	 * @return the triggers, comma separated, for instance {@code "E"}
	 */
	public static String getTriggerLabel(BdvHandle bdvh, String commandName,
		String... defaults)
	{
		final Set<InputTrigger> triggers = getConfig(bdvh).getInputs(commandName,
			KeyConfigContexts.BIGDATAVIEWER);
		if (triggers.isEmpty()) {
			return String.join(", ", defaults);
		}
		return triggers.stream().map(InputTrigger::toString).collect(Collectors
			.joining(", "));
	}

	/**
	 * Runs {@code action} whenever the user edits the keymap of {@code bdvh},
	 * typically to call {@code updateKeyConfig} on the behaviours installed on
	 * that window so that the change applies without reopening it.
	 * <p>
	 * The listener is dropped when the window is closed: the keymap outlives the
	 * window, so a listener left behind would keep the whole {@link BdvHandle}
	 * reachable.
	 *
	 * @param bdvh the window whose keymap is watched
	 * @param action what to run when the keymap changed
	 * @return the registered listener, if it has to be removed earlier
	 */
	public static Keymap.UpdateListener onKeymapChanged(BdvHandle bdvh,
		Runnable action)
	{
		final Keymap keymap = getKeymap(bdvh);
		final Keymap.UpdateListener listener = action::run;
		keymap.updateListeners().add(listener);

		SwingUtilities.invokeLater(() -> {
			final Window window = SwingUtilities.getWindowAncestor(bdvh
				.getViewerPanel());
			if (window != null) {
				window.addWindowListener(new WindowAdapter() {

					@Override
					public void windowClosed(WindowEvent e) {
						keymap.updateListeners().remove(listener);
						window.removeWindowListener(this);
					}
				});
			}
		});

		return listener;
	}

}
