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

/*
 * Axes alignment logic adapted from BigVolumeViewer (bvb.core.BVBActions)
 * Original code Copyright (C) 2025 - 2026 Cell Biology, Neurobiology and Biophysics
 * Department of Utrecht University. Used under BSD-2-Clause license.
 */

package sc.fiji.bdvpg.viewer.bdv.overlay;

import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.RotationAnimator;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;

/**
 * Click behaviour that aligns the BDV view to the axis highlighted on an
 * {@link AxesOverlay}. When the user clicks on one of the gizmo's axis circles,
 * the viewer animates a rotation to align the view with the corresponding plane.
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL, 2026
 */
public class AxesOverlayClickBehaviour implements ClickBehaviour {

	private final AxesOverlay axesOverlay;
	private final long animationDurationMs;

	/**
	 * @param axesOverlay the axes overlay to read the highlighted axis from
	 * @param animationDurationMs duration of the rotation animation in milliseconds
	 */
	public AxesOverlayClickBehaviour(AxesOverlay axesOverlay, long animationDurationMs) {
		this.axesOverlay = axesOverlay;
		this.animationDurationMs = animationDurationMs;
	}

	public AxesOverlayClickBehaviour(AxesOverlay axesOverlay) {
		this(axesOverlay, 300);
	}

	@Override
	public void click(int x, int y) {
		final int nAxis = axesOverlay.getHighlightedAxis();
		if (nAxis < 0) return;

		final double[] qTarget = axesOverlay.getAlignmentQuaternion(nAxis);
		if (qTarget == null) return;

		final ViewerPanel viewer = axesOverlay.getViewer();
		final AffineTransform3D transform = viewer.state().getViewerTransform();
		final double centerX = viewer.getWidth() * 0.5;
		final double centerY = viewer.getHeight() * 0.5;

		viewer.setTransformAnimator(
			new RotationAnimator(transform, centerX, centerY, qTarget, animationDurationMs));
	}
}
