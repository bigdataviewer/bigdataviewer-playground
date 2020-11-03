package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import net.imglib2.realtransform.AffineTransform3D;

public class AdaptedViewCenterTransformCreator
{
	private final BdvHandle bdvHandle;
	private final double[] xyz;
	private final int t;

	/**
	 * Based on the current ViewerTransform, creates a new viewer transform with a new center position.
	 *
	 * @param bdvHandle
	 * @param xyz
	 * 			target coordinates in physical units
	 * @param t
	 * 			time point
	 */
	public AdaptedViewCenterTransformCreator( BdvHandle bdvHandle, double[] xyz, int t )
	{
		this.bdvHandle = bdvHandle;
		this.xyz = xyz;
		this.t = t;
	}

	private AffineTransform3D createAdaptedTransform()
	{
		bdvHandle.getViewerPanel().setTimepoint( t );

		final AffineTransform3D currentViewerTransform = new AffineTransform3D();
		bdvHandle.getViewerPanel().state().getViewerTransform( currentViewerTransform );

		AffineTransform3D adaptedViewerTransform = currentViewerTransform.copy();

		// ViewerTransform notes:
		// - applyInverse: coordinates in viewer => coordinates in image
		// - apply: coordinates in image => coordinates in viewer

		final double[] targetPositionInViewerInPixels = new double[ 3 ];
		currentViewerTransform.apply( xyz, targetPositionInViewerInPixels );

		for ( int d = 0; d < 3; d++ )
		{
			targetPositionInViewerInPixels[ d ] *= -1;
		}

		adaptedViewerTransform.translate( targetPositionInViewerInPixels );

		final double[] windowCentreInViewerInPixels = new double[ 3 ];
		windowCentreInViewerInPixels[ 0 ] = bdvHandle.getViewerPanel().getDisplay().getWidth() / 2.0;
		windowCentreInViewerInPixels[ 1 ] = bdvHandle.getViewerPanel().getDisplay().getHeight() / 2.0;

		adaptedViewerTransform.translate( windowCentreInViewerInPixels );

		return adaptedViewerTransform;
	}

	public AffineTransform3D getAdaptedCenterTransform()
	{
		AffineTransform3D adaptedTransform = createAdaptedTransform();
		return adaptedTransform;
	}
}
