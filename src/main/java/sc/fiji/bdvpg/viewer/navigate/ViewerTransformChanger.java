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

import bdv.util.BdvHandle;
import bdv.viewer.AbstractViewerPanel;
import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;

/**
 * Action which changes the current viewerTransform of a {@link BdvHandle} with
 * the input {@link AffineTransform3D} See ViewTransformSetAndLogDemo for usage
 * examples
 *
 * @author haesleinhuepf, tischi - 12 2019 - 11 2020 @tischi: add option to
 *         animate; add option to apply absolute or relative (concatenate)
 */

public class ViewerTransformChanger implements Runnable {

	/**
	 * The bdvHandle of which the viewer transform should be changed
	 */
	private final AbstractViewerPanel viewer;

	/**
	 * The new viewer transform (either relative or absolute, see below)
	 */
	private AffineTransform3D transform;

	/**
	 * If {@code true} the transform will be concatenated (applied relative) to
	 * the current transform. This is useful, e.g., when one wants to rotate the
	 * current view. In this case the transform would only contain the rotation.
	 * If {@code false} the transform will be applied (absolute). This is useful,
	 * e.g., if one has stored a particular viewer transform, e.g., in some sort
	 * of bookmark and wants to come back to it.
	 */
	private final boolean concatenateToCurrentTransform;

	/**
	 * If <=0 the transform will be applied immediately. If >0 the change from the
	 * current transform to the new transform will be animated.
	 */
	private final int animationDurationMillis;

	public ViewerTransformChanger(AbstractViewerPanel viewer,
		AffineTransform3D transform, boolean concatenateToCurrentTransform)
	{
		this(viewer, transform, concatenateToCurrentTransform, 0);
	}

	public ViewerTransformChanger(AbstractViewerPanel viewer,
		AffineTransform3D transform, boolean concatenateToCurrentTransform,
		int animationDurationMillis)
	{
		this.viewer = viewer;
		this.transform = transform;
		this.concatenateToCurrentTransform = concatenateToCurrentTransform;
		this.animationDurationMillis = animationDurationMillis;
	}

	@Override
	public void run() {

		if (concatenateToCurrentTransform) {
			AffineTransform3D view = new AffineTransform3D();
			viewer.state().getViewerTransform(view);
			transform = view.concatenate(transform);
		}

		if (animationDurationMillis <= 0) {
			viewer.state().setViewerTransform(transform);
		}
		else {
			final AffineTransform3D currentViewerTransform = new AffineTransform3D();
			viewer.state().getViewerTransform(
				currentViewerTransform);

			final SimilarityTransformAnimator similarityTransformAnimator =
				new SimilarityTransformAnimator(currentViewerTransform, transform, 0, 0, // TODO:
																																									// understand
																																									// what
																																									// this
																																									// does
					animationDurationMillis);

			viewer.setTransformAnimator(
				similarityTransformAnimator);
		}
	}
}
