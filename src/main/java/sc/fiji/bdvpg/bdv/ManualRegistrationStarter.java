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

package sc.fiji.bdvpg.bdv;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import bdv.viewer.TransformListener;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * BigDataViewer Playground Action -- Action which starts the manual
 * registration of n SourceAndConverters Works in coordination with
 * ManualRegistrationStopper Works with a single BdvHandle (TODO : synchronizes
 * with multiple BdvHandle) Working principle: - Sources are transiently wrapped
 * into TransformedSource (only the ones which are displayed) and displayed -
 * The original sources are removed from the display (those who were displayed)
 * - When a BdvHandle view is transformed because of a user action, all
 * transformed sources are transformed in such a way that there position
 * relative to the viewer is kept identical. The ManualRegistrationStopper
 * action does actually stores the registration once it is finished Note : all
 * the selected sources will be registered ( parameter 'SourceAndConverter...
 * sacs' in the constructor ), however, only those who were displayed originally
 * will be used for the interactive manual registration, this allows for a much
 * stronger performance : you can actually register multiple sources but only
 * perform your registration based on a subset of them (those which are
 * currently displayed). Limitation : if no other source is visible, you don't
 * see what's happening at all...
 *
 * @author : Nicolas Chiaruttini, BIOP, EPFL 2019
 */
public class ManualRegistrationStarter implements Runnable {

	/**
	 * Sources that will be transformed
	 */
	final SourceAndConverter<?>[] sacs;

	/**
	 * From the sources that will be transformed, list of sources which were
	 * actually displayed at the beginning of the action
	 */
	final List<SourceAndConverter<?>> originallyDisplayedSacs = new ArrayList<>();

	/**
	 * Transient transformed source displayed for the registration
	 */
	final List<SourceAndConverter<?>> displayedSacsWrapped = new ArrayList<>();

	/**
	 * bdvHandle used for the manual registration
	 */
	final BdvHandle bdvHandle;

	/**
	 * Current registration state
	 */
	AffineTransform3D currentRegistration;

	/**
	 * Listener to BdvHandle view transform changes - maintains the displayed
	 * registered source at the same location relative to the viewer
	 */
	TransformListener<AffineTransform3D> manualRegistrationListener;

	public ManualRegistrationStarter(BdvHandle bdvHandle,
		SourceAndConverter<?>... sacs)
	{
		this.sacs = sacs;
		this.bdvHandle = bdvHandle;
	}

	@Override
	public void run() {

		for (SourceAndConverter<?> sourceAndConverter : sacs) {

			// Wraps into a Transformed Source, if the source was displayed originally
			if (SourceAndConverterServices.getBdvDisplayService().getDisplaysOf(
				sourceAndConverter).contains(bdvHandle))
			{
				if (SourceAndConverterServices.getBdvDisplayService().isVisible(
					sourceAndConverter, bdvHandle))
				{
					displayedSacsWrapped.add(new SourceAffineTransformer<>(
						sourceAndConverter, new AffineTransform3D()).get());
					originallyDisplayedSacs.add(sourceAndConverter);
				}
			}
		}

		// Remove from display the originally displayed sources
		SourceAndConverterServices.getBdvDisplayService().remove(bdvHandle,
			originallyDisplayedSacs.toArray(new SourceAndConverter[0]));

		// Shows the displayed wrapped Source
		SourceAndConverterServices.getBdvDisplayService().show(bdvHandle,
			displayedSacsWrapped.toArray(new SourceAndConverter[0]));

		// View of the BdvHandle before starting the registration
		AffineTransform3D originalViewTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform(
			originalViewTransform);

		manualRegistrationListener = (newView) -> {
			// Compute "difference" of ViewTransform between the original state and
			// the current state

			// Global difference of transform is
			currentRegistration = newView.copy();
			currentRegistration = currentRegistration.inverse();
			currentRegistration.concatenate(originalViewTransform);

			// Sets view transform fo transiently wrapped source to maintain relative
			// position
			displayedSacsWrapped.forEach(sac -> ((TransformedSource<?>) sac
				.getSpimSource()).setFixedTransform(currentRegistration));
		};

		// Sets the listener
		bdvHandle.getViewerPanel().transformListeners().add(
			manualRegistrationListener);
	}

	public BdvHandle getBdvHandle() {
		return bdvHandle;
	}

	/**
	 * Gets the listener, this is useful to stop the registration
	 * 
	 * @return the listener, this is useful to stop the registration
	 */
	public TransformListener<AffineTransform3D> getListener() {
		return manualRegistrationListener;
	}

	/**
	 * Returns the transient wrapped transformed sources displayed (and then used
	 * by the user for the registration)
	 * 
	 * @return the transient wrapped transformed sources displayed (and then used
	 *         by the user for the registration)
	 */
	public List<SourceAndConverter<?>>
		getTransformedSourceAndConverterDisplayed()
	{
		return displayedSacsWrapped;
	}

	/**
	 * Returns the sources that need to be registered
	 * 
	 * @return the sources that need to be registered
	 */
	public SourceAndConverter<?>[] getOriginalSourceAndConverter() {
		return sacs;
	}

	/**
	 * Returns the sources (within the sources that need to be transformed) that
	 * were originally displayed in the bdvHandle
	 * 
	 * @return the sources (within the sources that need to be transformed) that
	 *         were originally displayed in the bdvHandle
	 */
	public List<SourceAndConverter<?>>
		getOriginallyDisplayedSourceAndConverter()
	{
		return originallyDisplayedSacs;
	}

	/**
	 * Gets the current registration state, based on the difference between the
	 * initial bdvhandle view transform and its current view transform
	 * 
	 * @return its current view transform
	 */
	public AffineTransform3D getCurrentTransform() {
		return ensureOrthoNormalTransform(currentRegistration);
	}

	// Maybe unnecessary : makes sure the transformation is orthonormal
	public static AffineTransform3D ensureOrthoNormalTransform(
		AffineTransform3D at3D)
	{
		AffineTransform3D correctedAffineTransform = new AffineTransform3D();
		correctedAffineTransform.set(at3D);

		// Gets three vectors
		double v1x = at3D.get(0, 0);
		double v1y = at3D.get(0, 1);
		double v1z = at3D.get(0, 2);

		double v2x = at3D.get(1, 0);
		double v2y = at3D.get(1, 1);
		double v2z = at3D.get(1,2);

		// 0 - Ensure v1 and v2 have the same norm
		double normv1 = Math.sqrt(v1x * v1x + v1y * v1y + v1z * v1z);
		double normv2 = Math.sqrt(v2x * v2x + v2y * v2y + v2z * v2z);

		// If v1 and v2 do not have the same norm
		if (Math.abs(normv1 - normv2) / normv1 > 1e-10) {
			// We make v2 having the norm of v1
			v2x = v2x / normv2 * normv1;
			v2y = v2y / normv2 * normv1;
			v2z = v2z / normv2 * normv1;

			correctedAffineTransform.set(v2x, 1, 0);
			correctedAffineTransform.set(v2y, 1, 1);
			correctedAffineTransform.set(v2z, 1, 2);
		}

		// 1 - Ensure v1 and v2 are perpendicular

		if (Math.abs(v1x * v2x + v1y * v2y + v1z * v2z) / (normv1 *
			normv2) > (1e-10))
		{
			// v1 and v2 not perpendicular enough
			// Compute the projection of v1 onto v2
			double u1x = v1x / normv1;
			double u1y = v1y / normv1;
			double u1z = v1z / normv1;
			double dotProductNormalized = (u1x * v2x + u1y * v2y + u1z * v2z);
			v2x = v2x - dotProductNormalized * u1x;
			v2y = v2y - dotProductNormalized * u1y;
			v2z = v2z - dotProductNormalized * u1z;

			normv2 = Math.sqrt(v2x * v2x + v2y * v2y + v2z * v2z);
			v2x = v2x / normv2 * normv1;
			v2y = v2y / normv2 * normv1;
			v2z = v2z / normv2 * normv1;

			correctedAffineTransform.set(v2x, 1, 0);
			correctedAffineTransform.set(v2y, 1, 1);
			correctedAffineTransform.set(v2z, 1, 2);
		}

		// 2 - We now set v3 as the cross product of v1 and v2, no matter what
		double xr = (v1y * v2z - v1z * v2y) / normv1;
		double yr = (v1z * v2x - v1x * v2z) / normv1;
		double zr = (v1x * v2y - v1y * v2x) / normv1;

		//double v3orthonormal = new Point3d(xr, yr, zr);

		correctedAffineTransform.set(xr, 2, 0);
		correctedAffineTransform.set(yr, 2, 1);
		correctedAffineTransform.set(zr, 2, 2);

		return correctedAffineTransform;

	}

}
