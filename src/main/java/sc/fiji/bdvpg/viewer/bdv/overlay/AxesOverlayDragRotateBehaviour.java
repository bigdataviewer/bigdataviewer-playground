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
 * Rotation maths adapted from BigDataViewer (bdv.TransformEventHandler3D.Rotate)
 * Original code Copyright (C) 2012 - 2026 BigDataViewer developers.
 * Used under BSD-2-Clause license.
 */

package sc.fiji.bdvpg.viewer.bdv.overlay;

import bdv.viewer.ViewerPanel;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.DragBehaviour;

/**
 * Drag behaviour that rotates the BDV view while the mouse is over an
 * {@link AxesOverlay}, so that dragging the gizmo turns the volume the way
 * dragging a trackball would.
 * <p>
 * This mirrors the rotation of {@code bdv.TransformEventHandler3D.Rotate},
 * including its speed, with one difference: BigDataViewer pivots around the
 * point where the drag started, which is what you want when you grab the image
 * itself, but not when you grab a gizmo sitting in a corner — the volume would
 * swing away around that corner. Here the pivot is the centre of the window,
 * which is how orientation gizmos behave in Blender and in the viewers it
 * inspired.
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL, 2026
 */
public class AxesOverlayDragRotateBehaviour implements DragBehaviour {

	/** One step of rotation (radian), as in {@code TransformEventHandler3D}. */
	private static final double STEP = Math.PI / 180;

	private final ViewerPanel viewer;

	private final double speed;

	private final AffineTransform3D affineDragStart = new AffineTransform3D();

	private final AffineTransform3D affineDragCurrent = new AffineTransform3D();

	/** where the drag started */
	private int oX, oY;

	/** pivot of the rotation, the centre of the window when the drag started */
	private double cX, cY;

	/**
	 * @param viewer the panel whose view is rotated
	 * @param speed rotation speed, 1 being the one of a normal BDV drag rotate
	 */
	public AxesOverlayDragRotateBehaviour(ViewerPanel viewer, double speed) {
		this.viewer = viewer;
		this.speed = speed;
	}

	public AxesOverlayDragRotateBehaviour(ViewerPanel viewer) {
		this(viewer, 1.0);
	}

	@Override
	public void init(int x, int y) {
		oX = x;
		oY = y;
		cX = viewer.getWidth() * 0.5;
		cY = viewer.getHeight() * 0.5;
		viewer.state().getViewerTransform(affineDragStart);
	}

	@Override
	public void drag(int x, int y) {
		final double dX = oX - x;
		final double dY = oY - y;

		affineDragCurrent.set(affineDragStart);

		// center shift
		affineDragCurrent.set(affineDragCurrent.get(0, 3) - cX, 0, 3);
		affineDragCurrent.set(affineDragCurrent.get(1, 3) - cY, 1, 3);

		final double v = STEP * speed;
		affineDragCurrent.rotate(0, -dY * v);
		affineDragCurrent.rotate(1, dX * v);

		// center un-shift
		affineDragCurrent.set(affineDragCurrent.get(0, 3) + cX, 0, 3);
		affineDragCurrent.set(affineDragCurrent.get(1, 3) + cY, 1, 3);

		viewer.state().setViewerTransform(affineDragCurrent);
	}

	@Override
	public void end(int x, int y) {}

}