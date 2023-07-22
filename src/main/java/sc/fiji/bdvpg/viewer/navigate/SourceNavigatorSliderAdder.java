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
import bdv.viewer.ViewerStateChangeListener;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.viewer.ViewerHelper;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adds a slider to navigate BigDataViewer sources. Sliding it will get you to get the
 * center of the sources. The slider number of positions is updated each time a
 * sources is added, removed, or made visible / hidden. Or when a timepoint is changed.
 *
 * Potentially : we could add smooth transitions...
 *
 * @author Nicolas Chiaruttini, EPFL, 2022
 */
public class SourceNavigatorSliderAdder implements Runnable {

	final AbstractViewerPanel viewer;

	private List<RealPoint> centerLocations = new ArrayList<>();
	private List<String> sourcesNames = new ArrayList<>();

	final JSlider slider;

	// Create the JSpinner
	final SpinnerNumberModel spinnerModel;// = new SpinnerNumberModel(50, 0, 100, 1);
	final JSpinner spinner;// = new JSpinner(spinnerModel);
	final JPanel panel;

	final JLabel sourceName = new JLabel();

	// static ImageIcon sourceIcon;
	static final ImageIcon expandIcon;

	static {
		URL iconExpandURL;
		iconExpandURL = SourceNavigatorSliderAdder.class.getResource(
				"/images/adjustview.png");
		expandIcon = new ImageIcon(iconExpandURL);
	}

	public SourceNavigatorSliderAdder(AbstractViewerPanel viewer) {
		this.viewer = viewer;
		slider = new JSlider(JSlider.HORIZONTAL);
		spinnerModel = new SpinnerNumberModel(0, 0, 1, 1);
		spinner = new JSpinner(spinnerModel);
		// Link the JSlider and JSpinner
		spinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int idx = (int) spinner.getValue();
				if (sourcesNames.size()>idx) {
					sourceName.setText(sourcesNames.get(idx));
				}
				slider.setValue(idx);
			}
		});
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int idx = slider.getValue();
				if (sourcesNames.size()>idx) {
					sourceName.setText(sourcesNames.get(idx));
				}
				spinner.setValue(idx);
			}
		});
		// Add the JSlider and JSpinner to a panel
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // To avoid problems when the name is changing
		panel.add(slider);
		panel.add(spinner);
		panel.add(sourceName);

		int newSize = (int) (panel.getPreferredSize().getHeight() * 0.6);

		Image img = expandIcon.getImage() ;
		Image newimg = img.getScaledInstance( newSize, newSize,  java.awt.Image.SCALE_SMOOTH ) ;
		ImageIcon icon = new ImageIcon( newimg );
		JButton adjustOnSource = new JButton(icon);
		adjustOnSource.addActionListener(e -> {
			int idx = slider.getValue();
			if (this.sourcesNames.size()>idx) {
				List<SourceAndConverter<?>> sortedSources = SourceAndConverterHelper.sortDefaultGeneric(viewer.state().getVisibleSources());
				int timePoint = viewer.state().getCurrentTimepoint();
				sortedSources = sortedSources.stream().filter(source -> source.getSpimSource().isPresent(timePoint)).collect(Collectors.toList());
				SourceAndConverter<?> src = sortedSources.get(idx);
				new ViewerTransformAdjuster(viewer,src).run();
			}
		});
		panel.add(adjustOnSource);

	}

	int currentPosition;

	final ViewerStateChangeListener changeListener = (viewerStateChange) -> {
		switch (viewerStateChange) {
			case NUM_SOURCES_CHANGED:
			case CURRENT_TIMEPOINT_CHANGED:
			case VISIBILITY_CHANGED:
				updatePositions();
		}
	};

	@Override
	public void run() {

		viewer.state().changeListeners().add(changeListener);
		viewer.add(panel, BorderLayout.NORTH);
		viewer.revalidate();

		slider.addChangeListener((e) -> {
			if (centerLocations.size() > 0) {
				JSlider slider = (JSlider) e.getSource();
				int newValue = slider.getValue();
				// Plane : slider.getValue() / slider.getMaximum()
				if ((currentPosition != newValue) && (newValue != -1) && (newValue< centerLocations.size()) && (newValue>=0)) {
					// User slider action: needs update of viewer from currentPosition to
					// newValue
					currentPosition = newValue;
					// Need to shift z by shiftZ

					AffineTransform3D at3d = new AffineTransform3D();

					// Change the position of the viewer with the new offset
					viewer.state().getViewerTransform(at3d);

					double[] newCenter = centerLocations.get(currentPosition).positionAsDoubleArray();

					viewer.state().setViewerTransform(ViewerHelper
						.getViewerTransformWithNewCenter(viewer, newCenter));

				}
			}
		});

		updatePositions();
	}

	public void updatePositions() {
		List<SourceAndConverter<?>> sortedSources = SourceAndConverterHelper.sortDefaultGeneric(viewer.state().getVisibleSources());
		int timePoint = viewer.state().getCurrentTimepoint();
		sortedSources = sortedSources.stream().filter(source -> source.getSpimSource().isPresent(timePoint)).collect(Collectors.toList());
		List<RealPoint> centers = new ArrayList<>(sortedSources.size());
		List<String> srcsNames = new ArrayList<>(sortedSources.size());
		for (SourceAndConverter<?> source : sortedSources) {
			centers.add(SourceAndConverterHelper.getSourceAndConverterCenterPoint(source, viewer.state().getCurrentTimepoint()));
			srcsNames.add(source.getSpimSource().getName());
		}
		centerLocations = centers;
		sourcesNames = srcsNames;
		if (centers.size()>0) {
			slider.setMaximum(centers.size() - 1);
			spinnerModel.setMaximum(centers.size() - 1);
		}
	}

	public AbstractViewerPanel getViewer() {
		return viewer;
	}

	public void remove() {
		viewer.state().changeListeners().remove(changeListener);
		viewer.remove(slider);
		viewer.revalidate();
	}
}
