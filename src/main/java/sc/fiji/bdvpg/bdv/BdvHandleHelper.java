/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.cache.CacheService;
import org.scijava.object.ObjectService;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerConfigHelper;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.bdv.config.BdvSettingsGUISetter;
import sc.fiji.bdvpg.scijava.services.BdvService;
import sc.fiji.bdvpg.viewer.ViewerHelper;

import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.Consumer;

/**
 * BdvHandleHelper Author: haesleinhuepf, tischi, nicokiaru 12 2019
 */
public class BdvHandleHelper {

	protected static final Logger logger = LoggerFactory.getLogger(
			BdvHandleHelper.class);

	final public static String LAST_ACTIVE_BDVH_KEY = "LAST_ACTIVE_BDVH";

	public static void setBdvHandleCloseOperation(BdvHandle bdvh, CacheService cs,
												  BdvService bdvsds, boolean putWindowOnTop,
												  Runnable runnable)
	{
		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh
			.getViewerPanel());
		WindowAdapter wa;
		wa = new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (runnable != null) {
					runnable.run();
				}
				super.windowClosing(e);
				bdvsds.closeViewer(bdvh);
				topFrame.removeWindowListener(this); // Avoid memory leak
				e.getWindow().dispose();
				bdvh.close();
			}

			@Override
			public void windowActivated(WindowEvent e) {
				super.windowActivated(e);
				cs.put(LAST_ACTIVE_BDVH_KEY, new WeakReference<>(bdvh));
				// Very old school
				/*if (Recorder.record) {
				    // run("Select Bdv Window", "bdvh=bdv.util.BdvHandleFrame@e6c7718");
				    String cmdrecord = "run(\"Select Bdv Window\", \"bdvh=" + getWindowTitle(bdvh) + "\");\n";
				    Recorder.recordString(cmdrecord);
				}*/
			}
		};
		topFrame.addWindowListener(wa);

		if (putWindowOnTop) {
			cs.put(LAST_ACTIVE_BDVH_KEY, new WeakReference<>(bdvh));// why a weak
																														// reference ?
																														// because we want
																														// to dispose the
																														// bdvhandle if it
																														// is closed
		}
	}

	public static void activateWindow(BdvHandle bdvh) {
		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh
			.getViewerPanel());
		topFrame.toFront();
		topFrame.requestFocus();
	}

	public static String getUniqueWindowTitle(ObjectService os, String iniTitle) {
		List<BdvHandle> bdvs = os.getObjects(BdvHandle.class);
		boolean duplicateExist;
		String uniqueTitle = iniTitle;
		duplicateExist = bdvs.stream().filter(bdv -> (bdv.toString().equals(
			iniTitle)) || (ViewerHelper.getViewerTitle(bdv.getViewerPanel()).equals(iniTitle))).count() > 1;
		while (duplicateExist) {
			if (uniqueTitle.matches(".+(_)\\d+")) {
				int idx = Integer.parseInt(uniqueTitle.substring(uniqueTitle
					.lastIndexOf("_") + 1));
				uniqueTitle = uniqueTitle.substring(0, uniqueTitle.lastIndexOf("_") +
					1);
				uniqueTitle += String.format("%02d", idx + 1);
			}
			else {
				uniqueTitle += "_00";
			}
			try {
				Thread.sleep(300);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			String uTTest = uniqueTitle;
			duplicateExist = bdvs.stream().anyMatch(bdv -> (bdv.toString().equals(
				uTTest)) || (ViewerHelper.getViewerTitle(bdv.getViewerPanel()).equals(uTTest)));
		}
		return uniqueTitle;
	}

	/**
	 * For debugging: - print actions and triggers of a bdv
	 * 
	 * @param stringEater consumer of the string to print the bindings (system
	 *          out, ij log, etc.)
	 * @param bdv ze bdv
	 */
	public static void printBindings(BdvHandle bdv,
		Consumer<String> stringEater)
	{
		stringEater.accept("--------------------- Behaviours");
		bdv.getTriggerbindings().getConcatenatedBehaviourMap().getAllBindings()
			.forEach((label, behaviour) -> {
				stringEater.accept("Label: " + label);
				stringEater.accept("\t" + behaviour.getClass().getSimpleName());
			});
		stringEater.accept("--------------------- Triggers");
		bdv.getTriggerbindings().getConcatenatedInputTriggerMap().getAllBindings()
			.forEach((trigger, actions) -> {
				stringEater.accept("Trigger: " + trigger.toString());
				for (String action : actions)
					stringEater.accept("\t" + action);
			});
		stringEater.accept("--------------------- Key Action");
		for (Object o : bdv.getKeybindings().getConcatenatedActionMap().allKeys()) {
			stringEater.accept("\t" + o);
		}
		stringEater.accept("--------------------- Key Triggers");
		if (bdv.getKeybindings().getConcatenatedInputMap() != null) {
			if (bdv.getKeybindings().getConcatenatedInputMap().allKeys() != null) {
				for (KeyStroke ks : bdv.getKeybindings().getConcatenatedInputMap()
					.allKeys())
				{
					stringEater.accept("\t" + ks + ":" + bdv.getKeybindings()
						.getConcatenatedInputMap().get(ks));
				}
				stringEater.accept("Null keys in Keybindings ConcatenatedInputMap!");
			}
		}
		else {
			stringEater.accept("Null Keybindings InputMap!");
		}
	}

	/**
	 * Install trigger bindings according to the path specified See
	 * {@link BdvSettingsGUISetter} Key bindings can not be overridden yet
	 * 
	 * @param bdv bdvhandle
	 * @param pathToBindings string path to the folder containing the yaml file
	 */
	static void install(BdvHandle bdv, String pathToBindings) {
		String yamlDataLocation = pathToBindings + File.separator +
			BdvSettingsGUISetter.bdvKeyConfigFileName;

		InputTriggerConfig yamlConf = null;

		try {
			yamlConf = new InputTriggerConfig(YamlConfigIO.read(yamlDataLocation));
		}
		catch (final Exception e) {
			logger.warn("Could not create " + yamlDataLocation +
				" file. Using defaults instead.");
		}

		if (yamlConf != null) {

			bdv.getTriggerbindings().addInputTriggerMap(pathToBindings,
				InputTriggerConfigHelper.getInputTriggerMap(yamlConf), "transform");

			// TODO : support replacement of key bindings
			// bdv.getKeybindings().addInputMap("bdvpg", new InputMap(), "bdv",
			// "navigation");
		}

	}

	public static BdvOverlay addCenterCross(BdvHandle bdvh) {
		final BdvOverlay overlay = new BdvOverlay() {

			@Override
			protected void draw(final Graphics2D g) {
				int colorCode = this.info.getColor().get();
				int w = bdvh.getViewerPanel().getWidth();
				int h = bdvh.getViewerPanel().getHeight();
				g.setColor(new Color(ARGBType.red(colorCode), ARGBType.green(colorCode),
					ARGBType.blue(colorCode), ARGBType.alpha(colorCode)));
				g.drawLine(w / 2, h / 2 - h / 4, w / 2, h / 2 + h / 4);
				g.drawLine(w / 2 - w / 4, h / 2, w / 2 + w / 4, h / 2);
			}

		};

		int nTimepoints = bdvh.getViewerPanel().state().getNumTimepoints();
		int iTimePoint = bdvh.getViewerPanel().state().getCurrentTimepoint();
		BdvFunctions.showOverlay(overlay, "cross_overlay", BdvOptions.options()
			.addTo(bdvh));
		bdvh.getViewerPanel().state().setNumTimepoints(nTimepoints);
		bdvh.getViewerPanel().state().setCurrentTimepoint(iTimePoint);
		return overlay;
	}


}
