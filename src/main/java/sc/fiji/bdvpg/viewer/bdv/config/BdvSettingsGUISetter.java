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

import bdv.KeyConfigContexts;
import bdv.tools.PreferencesDialog;
import bdv.ui.keymap.KeymapManager;
import bdv.ui.keymap.KeymapSettingsPage;
import org.scijava.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * BigDataViewer Playground Action -- Opens the preferences dialog, which lets
 * the user choose the BDV preferences ({@link bdv.util.Prefs}) and edit the
 * key and mouse bindings.
 * <p>
 * The bindings page is BDV's own {@link KeymapSettingsPage}: it lists every
 * command of BDV, of the playground and of the plugins which declare a
 * {@code CommandDescriptionProvider}, and lets the user rebind them, duplicate
 * a keymap under a new name and pick which keymap is active. It edits the
 * keymap shared by all playground windows ({@link BdvKeymapHelper}), so a
 * change applies immediately to the windows which are already open, and is
 * saved under {@code <configDir>/keymaps/}.
 * <p>
 * The same dialog is reachable from within any BDV window with
 * {@code ctrl COMMA}.
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */

public class BdvSettingsGUISetter implements Runnable {

	protected static final Logger logger = LoggerFactory.getLogger(
		BdvSettingsGUISetter.class);

	/**
	 * @deprecated bindings are no longer stored in a per-folder YAML file of the
	 *             Fiji installation, but in the keymaps of
	 *             {@link BdvKeymapHelper#getKeymapManager()}.
	 */
	@Deprecated
	public final static String bdvKeyConfigFileName = "bdvkeyconfig.yaml";

	/**
	 * @deprecated see {@link #bdvKeyConfigFileName}
	 */
	@Deprecated
	public final static String defaultBdvPgSettingsRootPath = "plugins" +
		File.separator + "bdvpgsettings";

	public BdvSettingsGUISetter() {}

	/**
	 * @param path ignored, kept for backwards compatibility. Bindings used to be
	 *          stored per folder below the Fiji installation, they are now named
	 *          keymaps in the BDV config directory, see {@link BdvKeymapHelper}.
	 * @param context ignored, kept for backwards compatibility. The command
	 *          descriptions are discovered by the {@link KeymapManager} itself.
	 */
	public BdvSettingsGUISetter(String path, Context context) {
		if (path != null && !path.isEmpty()) {
			logger.debug(
				"The settings path '{}' is ignored: bindings are now stored as keymaps in the BDV config directory.",
				path);
		}
	}

	@Override
	public void run() {
		final KeymapManager keymapManager = BdvKeymapHelper.getKeymapManager();

		final PreferencesDialog dialog = new PreferencesDialog(null,
			BdvKeymapHelper.getKeymap(), new String[] {
				KeyConfigContexts.BIGDATAVIEWER });

		dialog.addPage(new BdvPrefsSettingsPage("bdv prefs"));
		dialog.addPage(new KeymapSettingsPage("Keymap", keymapManager,
			keymapManager.getCommandDescriptions()));

		dialog.setVisible(true);
	}

	/**
	 * @deprecated actions are no longer stored next to a per-folder binding file.
	 * @param path unused
	 * @param context unused
	 * @return the legacy location of the actions file
	 */
	@Deprecated
	static public File getActionFile(String path, String context) {
		if (!path.endsWith(File.separator)) {
			path += File.separator;
		}
		return new File(defaultBdvPgSettingsRootPath + File.separator + path +
			"bdvpg." + context + ".actions.txt");
	}

}
