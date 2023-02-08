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

import bdv.tools.brightness.ConverterSetup;
import bdv.ui.CardPanel;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.viewer.Source;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.util.LinAlgHelpers;
import org.scijava.cache.CacheService;
import org.scijava.object.ObjectService;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerConfigHelper;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.bdv.config.BdvSettingsGUISetter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.viewers.ViewerAdapter;

import javax.swing.JComponent;
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

	/**
	 * Creates a viewer transform with a new center position. Author: @tischi - 11
	 * 2020
	 *
	 * @param bdvHandle the bdvHandle, used to fetch the current viewerTransform
	 *          and the current window size
	 * @param xyz target coordinates for the new center in physical units
	 * @return viewerTransform that keeps the orientation of the current
	 *         viewerTransform but with a shifted center
	 */
	public static AffineTransform3D getViewerTransformWithNewCenter(
		BdvHandle bdvHandle, double[] xyz)
	{
		return getViewerTransformWithNewCenter(new ViewerAdapter(bdvHandle), xyz);
	}

	public static AffineTransform3D getViewerTransformWithNewCenter(
		ViewerAdapter handle, double[] xyz)
	{
		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		handle.state().getViewerTransform(currentViewerTransform);

		AffineTransform3D adaptedViewerTransform = currentViewerTransform.copy();

		// ViewerTransform notes:
		// - applyInverse: coordinates in viewer => coordinates in image
		// - apply: coordinates in image => coordinates in viewer

		final double[] targetPositionInViewerInPixels = new double[3];
		currentViewerTransform.apply(xyz, targetPositionInViewerInPixels);

		for (int d = 0; d < 3; d++) {
			targetPositionInViewerInPixels[d] *= -1;
		}

		adaptedViewerTransform.translate(targetPositionInViewerInPixels);

		final double[] windowCentreInViewerInPixels = getWindowCentreInPixelUnits(
			handle);

		adaptedViewerTransform.translate(windowCentreInViewerInPixels);

		return adaptedViewerTransform;
	}

	public static double[] getWindowCentreInPixelUnits(ViewerAdapter handle) {
		final double[] windowCentreInPixelUnits = new double[3];
		windowCentreInPixelUnits[0] = handle.getWidth() / 2.0;
		windowCentreInPixelUnits[1] = handle.getHeight() / 2.0;
		return windowCentreInPixelUnits;
	}

	public static double[] getWindowCentreInPixelUnits(BdvHandle bdvHandle) {
		return getWindowCentreInPixelUnits(new ViewerAdapter(bdvHandle));
	}

	public static double[] getWindowCentreInCalibratedUnits(BdvHandle bdvHandle) {
		final double[] centreInPixelUnits = getWindowCentreInPixelUnits(bdvHandle);
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform(affineTransform3D);
		final double[] centreInCalibratedUnits = new double[3];
		affineTransform3D.inverse().apply(centreInPixelUnits,
			centreInCalibratedUnits);
		return centreInCalibratedUnits;
	}

	public static double getViewerVoxelSpacing(BdvHandle bdv) {
		final int windowWidth = bdv.getViewerPanel().getDisplay().getWidth();
		final int windowHeight = bdv.getViewerPanel().getDisplay().getHeight();

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		bdv.getViewerPanel().state().getViewerTransform(viewerTransform);

		final double[] physicalA = new double[3];
		final double[] physicalB = new double[3];

		viewerTransform.applyInverse(physicalA, new double[] { 0, 0, 0 });
		viewerTransform.applyInverse(physicalB, new double[] { 0, windowWidth, 0 });

		double viewerPhysicalWidth = LinAlgHelpers.distance(physicalA, physicalB);

		viewerTransform.applyInverse(physicalA, new double[] { 0, 0, 0 });
		viewerTransform.applyInverse(physicalB, new double[] { windowHeight, 0,
			0 });

		double viewerPhysicalHeight = LinAlgHelpers.distance(physicalA, physicalB);

		final double viewerPhysicalVoxelSpacingX = viewerPhysicalWidth /
			windowWidth;
		final double viewerPhysicalVoxelSpacingY = viewerPhysicalHeight /
			windowHeight;

		logger.debug("windowWidth = " + windowWidth);
		logger.debug("windowHeight = " + windowHeight);
		logger.debug("viewerPhysicalWidth = " + viewerPhysicalWidth);
		logger.debug("viewerPhysicalHeight = " + viewerPhysicalHeight);
		logger.debug("viewerPhysicalVoxelSpacingX = " +
			viewerPhysicalVoxelSpacingX);
		logger.debug("viewerPhysicalVoxelSpacingY = " +
			viewerPhysicalVoxelSpacingY);

		return viewerPhysicalVoxelSpacingX;
	}

	public static boolean isSourceIntersectingCurrentView(BdvHandle bdv,
		Source<?> source, boolean is2D)
	{
		if (source.getSource(0, 0) == null) {
			// Overlays have no RAI -> discard them
			return false;
		}

		final Interval interval = getSourceGlobalBoundingInterval(source, bdv
			.getViewerPanel().state().getCurrentTimepoint());

		final Interval viewerInterval = Intervals.smallestContainingInterval(
			getViewerGlobalBoundingInterval(bdv));

		boolean intersects;
		if (is2D) {
			intersects = !Intervals.isEmpty(intersect2D(interval, viewerInterval));
		}
		else {
			intersects = !Intervals.isEmpty(Intervals.intersect(interval,
				viewerInterval));
		}
		return intersects;
	}

	/**
	 * @param intervalA 3d interval A
	 * @param intervalB 3d interval B
	 * @return the intersection 3D interval between A and B
	 */
	public static FinalInterval intersect2D(final Interval intervalA,
		final Interval intervalB)
	{
		assert intervalA.numDimensions() == intervalB.numDimensions();

		final long[] min = new long[2];
		final long[] max = new long[2];
		for (int d = 0; d < 2; ++d) {
			min[d] = Math.max(intervalA.min(d), intervalB.min(d));
			max[d] = Math.min(intervalA.max(d), intervalB.max(d));
		}
		return new FinalInterval(min, max);
	}

	/**
	 * @param bdvHandle bdv window
	 * @return an interval containing the bdv current view
	 */
	public static FinalRealInterval getViewerGlobalBoundingInterval(
		BdvHandle bdvHandle)
	{
		AffineTransform3D viewerTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform(viewerTransform);
		viewerTransform = viewerTransform.inverse();
		final long[] min = new long[3];
		final long[] max = new long[3];
		max[0] = bdvHandle.getViewerPanel().getWidth();
		max[1] = bdvHandle.getViewerPanel().getHeight();
		return viewerTransform.estimateBounds(new FinalInterval(min, max));
	}

	/**
	 * @param source source to probe
	 * @param timepoint timepoint probed for the source
	 * @return an interval containing the source
	 */
	public static Interval getSourceGlobalBoundingInterval(Source<?> source,
		int timepoint)
	{
		final AffineTransform3D sourceTransform = getSourceTransform(source,
			timepoint);
		final RandomAccessibleInterval<?> rai = source.getSource(timepoint, 0);
		return Intervals.smallestContainingInterval(sourceTransform.estimateBounds(
			rai));
	}

	/**
	 * @param source source
	 * @param timepoint timepoint
	 * @return the transform of the source at its highest resolution level
	 */
	public static AffineTransform3D getSourceTransform(Source<?> source,
		int timepoint)
	{
		return getSourceTransform(source, timepoint, 0);
	}

	/**
	 * Returns the highest level where the {@link bdv.viewer.SourceAndConverter}
	 * voxel spacings are inferior or equals to the requested ones.
	 *
	 * @param source the source
	 * @param voxelSpacings voxel spacings of the source
	 * @return the optimal level for image visualization
	 */
	public static int getLevel(Source<?> source, double... voxelSpacings) {
		final int numMipmapLevels = source.getNumMipmapLevels();
		final int numDimensions = voxelSpacings.length;

		for (int level = numMipmapLevels - 1; level >= 0; level--) {
			final double[] calibration = getCalibration(source, level);

			boolean allSpacingsSmallerThanRequested = true;

			for (int d = 0; d < numDimensions; d++) {
				if (calibration[d] > voxelSpacings[d]) {
					allSpacingsSmallerThanRequested = false;
					break;
				}
			}

			if (allSpacingsSmallerThanRequested) return level;
		}
		return 0;
	}

	public static AffineTransform3D getSourceTransform(Source<?> source, int t,
		int level)
	{
		AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSourceTransform(t, level, sourceTransform);
		return sourceTransform;
	}

	public static double[] getCalibration(Source<?> source, int level) {
		final AffineTransform3D sourceTransform = new AffineTransform3D();

		source.getSourceTransform(0, level, sourceTransform);

		return getScale(sourceTransform);
	}

	public static double[] getScale(AffineTransform3D sourceTransform) {
		double[] calibration = new double[3];

		for (int d = 0; d < 3; ++d) {
			double[] vector = new double[3];

			for (int i = 0; i < 3; ++i) {
				vector[i] = sourceTransform.get(d, i);
			}

			calibration[d] = LinAlgHelpers.length(vector);
		}

		return calibration;
	}

	public static double[] getDisplayRange(ConverterSetup converterSetup) {
		final double displayRangeMin = converterSetup.getDisplayRangeMin();
		final double displayRangeMax = converterSetup.getDisplayRangeMax();

		return new double[] { displayRangeMin, displayRangeMax };
	}

	public static JFrame getJFrame(BdvHandle bdvh) {
		return (JFrame) SwingUtilities.getWindowAncestor(bdvh.getViewerPanel());
	}

	public static void setBdvHandleCloseOperation(BdvHandle bdvh, CacheService cs,
		SourceAndConverterBdvDisplayService bdvsds, boolean putWindowOnTop,
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
				bdvsds.closeBdv(bdvh);
				topFrame.removeWindowListener(this); // Avoid memory leak
				e.getWindow().dispose();
				bdvh.close();
			}

			@Override
			public void windowActivated(WindowEvent e) {
				super.windowActivated(e);
				cs.put("LAST_ACTIVE_BDVH", new WeakReference<>(bdvh));
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
			cs.put("LAST_ACTIVE_BDVH", new WeakReference<>(bdvh));// why a weak
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

	public static void closeWindow(BdvHandle bdvh) {
		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh
			.getViewerPanel());
		topFrame.dispatchEvent(new WindowEvent(topFrame,
			WindowEvent.WINDOW_CLOSING));
	}

	public static void setWindowTitle(BdvHandle bdvh, String title) {
		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh
			.getViewerPanel());
		topFrame.setTitle(title);
	}

	public static String getWindowTitle(BdvHandle bdvh) {
		JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(bdvh
			.getViewerPanel());
		return topFrame.getTitle();
	}

	public static String getUniqueWindowTitle(ObjectService os, String iniTitle) {
		List<BdvHandle> bdvs = os.getObjects(BdvHandle.class);
		boolean duplicateExist;
		String uniqueTitle = iniTitle;
		duplicateExist = bdvs.stream().filter(bdv -> (bdv.toString().equals(
			iniTitle)) || (getWindowTitle(bdv).equals(iniTitle))).count() > 1;
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
				uTTest)) || (getWindowTitle(bdv).equals(uTTest)));
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

	/**
	 * Calls bdvh.addCard in a thread safe manner
	 * @param bdvh bdv handle
	 * @param title title of the card
	 * @param component swing graphical component
	 * @param expanded whether the card is expanded on creation or not
	 */
	public static void addCard(BdvHandle bdvh, String title, JComponent component, boolean expanded) {
		SwingUtilities.invokeLater(() -> bdvh.getCardPanel().addCard(title, component, expanded));
	}

	/**
	 * Calls bdvh.removeCard in a thread safe manner
	 * @param bdvh bdv handle
	 * @param key key of the card to remove
	 */
	public static void removeCard(BdvHandle bdvh, Object key) {
		SwingUtilities.invokeLater(() -> bdvh.getCardPanel().removeCard(key));
	}

}
