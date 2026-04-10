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
 * Axes gizmo rendering adapted from BigVolumeViewer (bvb.gui.overlays.AxesOverlayRenderer)
 * Original code Copyright (C) 2025 - 2026 Cell Biology, Neurobiology and Biophysics
 * Department of Utrecht University. Used under BSD-2-Clause license.
 */

package sc.fiji.bdvpg.viewer.bdv.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import bdv.util.BdvOverlay;
import bdv.viewer.ViewerPanel;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

/**
 * Blender-style 3D axes gizmo overlay for BigDataViewer.
 * Displays an interactive orientation indicator in the corner of the viewer,
 * showing the current rotation of the view with labeled X, Y, Z axes.
 *
 * Adapted from BigVolumeViewer's AxesOverlayRenderer by Utrecht University.
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL, 2026
 */
public class AxesOverlay extends BdvOverlay {

	private final ViewerPanel viewer;

	private static final double C_QUAT = Math.cos(Math.PI / 4);

	private final Color[] axesColors = new Color[] {
		new Color(255, 54, 83, 255),   // X positive - red
		new Color(118, 178, 23, 255),  // Y positive - green
		new Color(48, 121, 204, 255),  // Z positive - blue
		new Color(255, 54, 83, 128),   // X negative - red transparent
		new Color(118, 178, 23, 128),  // Y negative - green transparent
		new Color(48, 121, 204, 128)   // Z negative - blue transparent
	};

	private final Color hoverBGColor = new Color(0xb0bbbbbb, true);
	private final BasicStroke vectorStroke = new BasicStroke(2.5f);
	private final BasicStroke ovalStroke = new BasicStroke(1);
	private final BasicStroke letterStroke = new BasicStroke(1.3f);

	private final double[] qRotation = new double[4];

	private final Point center = new Point();

	private final int vectRadius = 40;
	private final int circleAxisRadius = 9;
	private final int fullRadiusSquared = (vectRadius + circleAxisRadius) * (vectRadius + circleAxisRadius);

	/** current centers of circles with axes labels: 0-2 positive, 3-5 negative */
	private final double[][] dAxisCircleCenter = new double[6][3];

	/** current ends of vector sticks from origin to the circles: 0-2 positive, 3-5 negative */
	private final double[][] dAxisVector = new double[6][3];

	/** ordered indexes of circles/axes after sorting by depth */
	private final double[][] axisOrder = new double[6][2];

	private static final int X_LETTER_HALF = 3;
	private static final int Y_LETTER_HALF = 4;

	/** quaternions for the 6 alignment planes (ZY, XZ, XY and their opposites) */
	private final double[][] quaterLibrary = new double[6][4];

	/** index of the alignment plane matching current orientation, or -1 */
	private int nAlignIndex;

	/** last computed highlighted axis, accessible for click behaviour */
	private volatile int highlightedAxis = -1;

	private volatile boolean mouseAbove = false;

	//private final ClickBehaviourInstaller cbi;

	final Behaviours behaviours = new Behaviours(new InputTriggerConfig());
	final AxesOverlayClickBehaviour behaviour;


	public AxesOverlay(BdvHandle bdvh) {
		this.viewer = bdvh.getViewerPanel();
		behaviour =	new AxesOverlayClickBehaviour(AxesOverlay.this);
		//cbi = new ClickBehaviourInstaller(bdvh, new AxesOverlayClickBehaviour(AxesOverlay.this));
		viewer.getDisplay().addHandler(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				// Do nothing

			}

			@Override
			public void mouseMoved(MouseEvent e) {
				final int x = e.getX();
				final int y = e.getY();

				if (isPresentAt(x,y)) {
					if (!mouseAbove) {
						// Install Axes Overlay Behaviour
						mouseAbove = true;
						behaviours.install(bdvh.getTriggerbindings(), "axes_overlay");
						behaviours.behaviour(behaviour,"axes_overlay", "button1");
						bdvh.getViewerPanel().getDisplay().repaint();
					}
				} else {
					if (mouseAbove) {
						mouseAbove = false;
						// Uninstall Axes Overlay Behaviour
						bdvh.getTriggerbindings().removeBehaviourMap("axes_overlay");
						bdvh.getTriggerbindings().removeInputTriggerMap("axes_overlay");
						bdvh.getViewerPanel().getDisplay().repaint();
					}
				}

				// Force redraw when the mouse is over the gizmo
				if (mouseAbove) {
					bdvh.getViewerPanel().getDisplay().repaint();
				}
			}
		});
		initAlignmentQuaternions();
	}

	private boolean isPresentAt(int x, int y) {
		final float dMouseX = x - center.x;
		final float dMouseY = y - center.y;
		return (dMouseX * dMouseX + dMouseY * dMouseY < fullRadiusSquared);
	}

	private void initAlignmentQuaternions() {
		// Alignment quaternions from BVB conventions:
		// ZY, XZ, XY for positive directions; YZ, ZX, YX for negative
		double[][] alignQuats = new double[][] {
			{ 0.5, -0.5, -0.5, 0.5 },       // ZY
			{ 0, 0, C_QUAT, -C_QUAT },       // XZ
			{ 0, 0, 1, 0 },                   // XY
			{ 0.5, -0.5, 0.5, -0.5 },        // YZ
			{ C_QUAT, -C_QUAT, 0, 0 },       // ZX
			{ 0, 0, 0, 1 }                    // YX
		};
		for (int i = 0; i < 6; i++) {
			LinAlgHelpers.quaternionInvert(alignQuats[i], quaterLibrary[i]);
		}
	}

	@Override
	protected void draw(Graphics2D graphics) {
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final Rectangle clipBounds = graphics.getClipBounds();
		center.setLocation(clipBounds.getWidth() - 80, 100);
		int nHighlightedAxis;

		final Point pMouse = new Point(0,0);
		RealPoint point = new RealPoint(2);
		viewer.getMouseCoordinates(point);
		pMouse.x = (int) point.getDoublePosition(0);
		pMouse.y = (int) point.getDoublePosition(1);

		if (mouseAbove) {
			graphics.setColor(hoverBGColor);
			graphics.fillOval(
				center.x - vectRadius - circleAxisRadius,
				center.y - vectRadius - circleAxisRadius,
				2 * (vectRadius + circleAxisRadius) + 2,
				2 * (vectRadius + circleAxisRadius) + 2);
		}

		AffineTransform3D transform = new AffineTransform3D();
		viewer.state().getViewerTransform(transform);
		Affine3DHelpers.extractRotationAnisotropic(transform, qRotation);

		for (int d = 0; d < 6; d++) {
			for (int i = 0; i < 3; i++) {
				dAxisCircleCenter[d][i] = 0.0;
				dAxisVector[d][i] = 0.0;
			}
			if (d < 3) {
				dAxisCircleCenter[d][d] = vectRadius;
				dAxisVector[d][d] = vectRadius - circleAxisRadius*0 - 2*0;
			} else {
				dAxisCircleCenter[d][d - 3] = -vectRadius;
				dAxisVector[d][d - 3] = circleAxisRadius*0 - vectRadius + 2*0;
			}
			LinAlgHelpers.quaternionApply(qRotation, dAxisCircleCenter[d], dAxisCircleCenter[d]);
			LinAlgHelpers.quaternionApply(qRotation, dAxisVector[d], dAxisVector[d]);

			dAxisCircleCenter[d][0] += center.x;
			dAxisCircleCenter[d][1] += center.y;
			dAxisVector[d][0] += center.x;
			dAxisVector[d][1] += center.y;

			// prepare for depth sorting
			axisOrder[d][0] = dAxisCircleCenter[d][2];
			axisOrder[d][1] = d;
		}

		// sort by depth (z-coord) for correct rendering order
		Arrays.sort(axisOrder, (a, b) -> (-1) * Double.compare(a[0], b[0]));

		// find closest axis circle to mouse pointer
		if (mouseAbove) {
			float dx = (float) dAxisCircleCenter[0][0] - pMouse.x;
			float dy = (float) dAxisCircleCenter[0][1] - pMouse.y;
			float fDistance = dx * dx + dy * dy;
			nHighlightedAxis = 0;
			for (int d = 1; d < 6; d++) {
				dx = (float) dAxisCircleCenter[d][0] - pMouse.x;
				dy = (float) dAxisCircleCenter[d][1] - pMouse.y;
				if (dx * dx + dy * dy < fDistance) {
					nHighlightedAxis = d;
					fDistance = dx * dx + dy * dy;
				}
			}
		} else {
			nHighlightedAxis = -1;
		}

		highlightedAxis = nHighlightedAxis;

		// check if current orientation is aligned to a known plane
		nAlignIndex = isAlignedWithPlanes(qRotation);

		for (int d = 0; d < 6; d++) {
			graphics.setStroke(vectorStroke);

			final int index = (int) axisOrder[d][1];

			int x = (int) Math.round(dAxisCircleCenter[index][0]);
			int y = (int) Math.round(dAxisCircleCenter[index][1]);

			// positive axes
			if (index < 3) {
				graphics.setColor(axesColors[index]);
				graphics.drawLine(center.x, center.y, x, y);
				graphics.setStroke(ovalStroke);
				graphics.fillOval(x - circleAxisRadius, y - circleAxisRadius,
					2 * circleAxisRadius + 2, 2 * circleAxisRadius + 2);
				drawLetter(graphics, x + 1, y + 1, index, nHighlightedAxis);
			}
			// negative axes
			else {
				graphics.setColor(axesColors[index].darker());
				if (nAlignIndex >= 0) {
					graphics.drawLine(center.x, center.y,
						(int) Math.round(dAxisVector[index][0]),
						(int) Math.round(dAxisVector[index][1]));
					if (nAlignIndex == index)
						graphics.setColor(axesColors[index - 3]);
				}
				graphics.setStroke(ovalStroke);
				graphics.fillOval(x - circleAxisRadius, y - circleAxisRadius,
					2 * circleAxisRadius + 2, 2 * circleAxisRadius + 2);
				graphics.setColor(axesColors[index - 3]);
				graphics.drawOval(x - circleAxisRadius, y - circleAxisRadius,
					2 * circleAxisRadius + 2, 2 * circleAxisRadius + 2);
				drawLetter(graphics, x + 1, y + 1, index, nHighlightedAxis);
			}
		}
	}

	/**
	 * Draws axis label letters (X, Y, Z) using lines instead of fonts for consistent
	 * cross-platform appearance.
	 */
	private void drawLetter(final Graphics2D graphics, final int x, final int y,
		final int index, final int highlightedAxis)
	{
		if (highlightedAxis == index)
			graphics.setPaint(Color.WHITE);
		else
			graphics.setPaint(Color.BLACK);

		graphics.setStroke(letterStroke);
		int nFinalX = x;
		int nFinIndex = index;
		if (index > 2) {
			if (highlightedAxis == index) {
				// draw minus sign
				graphics.drawLine(nFinalX - 2 * X_LETTER_HALF, y, nFinalX - X_LETTER_HALF, y);
				nFinIndex = index - 3;
				nFinalX += 3;
			} else if (nAlignIndex == index) {
				graphics.drawLine(nFinalX - 2 * X_LETTER_HALF, y, nFinalX - X_LETTER_HALF, y);
				nFinIndex = index - 3;
				nFinalX += 3;
			} else {
				return;
			}
		}
		switch (nFinIndex) {
			case 0: // X
				graphics.drawLine(nFinalX - X_LETTER_HALF, y - Y_LETTER_HALF,
					nFinalX + X_LETTER_HALF, y + Y_LETTER_HALF);
				graphics.drawLine(nFinalX - X_LETTER_HALF, y + Y_LETTER_HALF,
					nFinalX + X_LETTER_HALF, y - Y_LETTER_HALF);
				break;
			case 1: // Y
				graphics.drawLine(nFinalX - X_LETTER_HALF, y - Y_LETTER_HALF, nFinalX, y + 1);
				graphics.drawLine(nFinalX + X_LETTER_HALF, y - Y_LETTER_HALF, nFinalX, y + 1);
				graphics.drawLine(nFinalX, y + 1, nFinalX, y + Y_LETTER_HALF);
				break;
			case 2: // Z
				graphics.drawLine(nFinalX - X_LETTER_HALF, y - Y_LETTER_HALF,
					nFinalX + X_LETTER_HALF, y - Y_LETTER_HALF);
				graphics.drawLine(nFinalX - X_LETTER_HALF, y + Y_LETTER_HALF,
					nFinalX + X_LETTER_HALF, y - Y_LETTER_HALF);
				graphics.drawLine(nFinalX - X_LETTER_HALF, y + Y_LETTER_HALF,
					nFinalX + X_LETTER_HALF, y + Y_LETTER_HALF);
				break;
			default:
				break;
		}
	}

	/**
	 * Checks if the provided quaternion is close to one of the 6 alignment plane quaternions.
	 * @return the plane index (0-5) or -1 if not aligned
	 */
	private int isAlignedWithPlanes(final double[] qCurrent) {
		for (int i = 0; i < 6; i++) {
			double dot = Math.abs(LinAlgHelpers.dot(qCurrent, quaterLibrary[i]));
			if (dot > 0.99999) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the axis circle currently closest to the mouse,
	 * or -1 if the mouse is not hovering over the gizmo.
	 * Indices 0-2 are positive X, Y, Z; indices 3-5 are negative X, Y, Z.
	 */
	public int getHighlightedAxis() {
		return highlightedAxis;
	}

	/**
	 * Returns the target quaternion for aligning the view to the given axis index.
	 * @param axisIndex 0-5, matching the highlighted axis indices
	 * @return the target quaternion, or null if the index is invalid
	 */
	public double[] getAlignmentQuaternion(int axisIndex) {
		if (axisIndex < 0 || axisIndex >= 6) return null;
		return quaterLibrary[axisIndex].clone();
	}

	public ViewerPanel getViewer() {
		return viewer;
	}

	@Override
	public void setCanvasSize(int width, int height) {
		// not needed
	}
}
