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

package sc.fiji.bdvpg.viewer.navigate;

import bdv.viewer.AbstractViewerPanel;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.TransformListener;
import bdv.viewer.ViewerStateChangeListener;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.viewer.ViewerHelper;

import javax.swing.JSlider;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adds a Z slider for BigDataViewer windows. It works by casting a ray at the
 * middle of the bdv window and finding all intersecting planes. The slider
 * updates itself to the current plane when the user moves in a 'regular' way
 * the bdv window. The slider can be moved by the user in order to update the
 * viewer to a different plane.
 *
 * @author Nicolas Chiaruttini, EPFL, 2021
 */
public class RayCastPositionerSliderAdder implements Runnable {

	final AbstractViewerPanel viewer;

	private List<Double> zLocations = new ArrayList<>();

	final JSlider slider;

	public RayCastPositionerSliderAdder(AbstractViewerPanel viewer) {
		this.viewer = viewer;
		slider = new JSlider(JSlider.VERTICAL);
	}

	int nPositions;
	int currentPosition;
	RealPoint lastDirection = new RealPoint();

	synchronized void setPositions(List<Double> zLocations) {
		// if (zLocations.size()>0) {
		this.zLocations = new ArrayList<>(zLocations);
		if (nPositions != zLocations.size()) {
			// Change of number of planes
			nPositions = zLocations.size();
			slider.setMaximum(nPositions - 1);
		}
		int i = 0;
		while ((i < nPositions) && (this.zLocations.get(i) < 0))
			i++;
		if (currentPosition != i) {
			currentPosition = i;
			slider.setValue(i);
		}
		// }
	}

	final TransformListener<AffineTransform3D> transformListener = (
		transform) -> updatePositions();
	final ViewerStateChangeListener changeListener = (viewerStateChange) -> updatePositions();

	@Override
	public void run() {

		viewer.transformListeners().add(transformListener);
		viewer.state().changeListeners().add(changeListener);
		viewer.add(slider, BorderLayout.WEST);
		viewer.revalidate();

		slider.addChangeListener((e) -> {
			if (zLocations.size() > 0) {
				JSlider slider = (JSlider) e.getSource();
				int newValue = slider.getValue();
				// Plane : slider.getValue() / slider.getMaximum()
				if ((currentPosition != newValue) && (newValue != -1)) {
					// User slider action: needs update of viewer from currentPosition to
					// newValue
					currentPosition = newValue;
					double shiftZ = zLocations.get(currentPosition);
					// Need to shift z by shiftZ

					AffineTransform3D at3d = new AffineTransform3D();

					// Change the position of the viewer with the new offset
					viewer.state().getViewerTransform(at3d);
					double[] currentCenter = ViewerHelper
						.getWindowCentreInCalibratedUnits(viewer);
					double[] newCenter = new double[3];
					newCenter[0] = currentCenter[0] + lastDirection.getDoublePosition(0) *
						shiftZ;
					newCenter[1] = currentCenter[1] + lastDirection.getDoublePosition(1) *
						shiftZ;
					newCenter[2] = currentCenter[2] + lastDirection.getDoublePosition(2) *
						shiftZ;
					viewer.state().setViewerTransform(ViewerHelper
						.getViewerTransformWithNewCenter(viewer, newCenter));

				} // else: Bdv user movement: no update required

			}
		});

	}

	public void updatePositions() {

		// Find origin and direction of ray - center of the bdv window
		double[] c = ViewerHelper.getWindowCentreInCalibratedUnits(viewer);
		RealPoint origin = new RealPoint(3);
		origin.setPosition(c[0], 0);
		origin.setPosition(c[1], 1);
		origin.setPosition(c[2], 2);

		RealPoint direction = new RealPoint(0, 0, 1);
		final AffineTransform3D affineTransform3D = new AffineTransform3D();
		viewer.state().getViewerTransform(affineTransform3D);
		affineTransform3D.setTranslation(0, 0, 0);
		affineTransform3D.inverse().apply(direction, direction);
		SourceAndConverterHelper.normalize3(direction);

		lastDirection = new RealPoint(direction);

		// Initializes zLocations : empty
		List<Double> zLocations = new ArrayList<>();
		int timepoint = viewer.state().getCurrentTimepoint();

		for (SourceAndConverter<?> source : viewer.state()
			.getActiveSources())
		{
			zLocations.addAll(SourceAndConverterHelper.rayIntersect(source, timepoint,
				origin, direction));
		}

		// Precision loss for efficient duplicates removal
		zLocations = zLocations.stream().map(d -> (double) (d.floatValue()))
			.collect(Collectors.toList());

		// Fast
		zLocations = zLocations.stream().sorted().distinct() // Removes duplicate z
																													// positions
			.collect(Collectors.toList());

		setPositions(zLocations);
	}

	public AbstractViewerPanel getViewer() {
		return viewer;
	}

	public void removeFromBdv() {
		viewer.transformListeners().remove(transformListener);
		viewer.state().changeListeners().remove(changeListener);
		viewer.remove(slider);
		viewer.revalidate();
	}
}
