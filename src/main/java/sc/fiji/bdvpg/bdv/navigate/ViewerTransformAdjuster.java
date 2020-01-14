package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.Affine3DHelpers;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.state.ViewerState;
import net.imglib2.Interval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;

public class ViewerTransformAdjuster implements Runnable
{
	private final BdvHandle bdvHandle;
	private final SourceAndConverter source;
	private boolean zoomedIn = false; // TODO: what's the point of this?

	public ViewerTransformAdjuster( BdvHandle bdvHandle, SourceAndConverter source )
	{
		this.bdvHandle = bdvHandle;
		this.source = source;
	}

	public void run()
	{
		final AffineTransform3D transform = getTransform();
		bdvHandle.getViewerPanel().setCurrentViewerTransform( transform );
	}

	/**
	 * Get a "good" initial viewer transform. The viewer transform is chosen
	 * such that for the sourceandconverter,
	 * <ul>
	 * <li>the XY plane is aligned with the screen plane,
	 * <li>the <em>z = dim_z / 2</em> slice is shown,
	 * <li>centered and scaled such that the full <em>dim_x</em> by
	 * <em>dim_y</em> is visible.
	 * </ul>
	 */
	public AffineTransform3D getTransform( )
	{
		final ViewerState state = bdvHandle.getViewerPanel().getState();

		final int viewerWidth = bdvHandle.getBdvHandle().getViewerPanel().getWidth();
		final int viewerHeight = bdvHandle.getBdvHandle().getViewerPanel().getHeight();

		final double cX = viewerWidth / 2.0;
		final double cY = viewerHeight / 2.0;

		final int timepoint = state.getCurrentTimepoint();
		if ( !source.getSpimSource().isPresent( timepoint ) )
			return new AffineTransform3D();

		final AffineTransform3D sourceTransform = new AffineTransform3D();
		source.getSpimSource().getSourceTransform( timepoint, 0, sourceTransform );

		final Interval sourceInterval = source.getSpimSource().getSource( timepoint, 0 );
		final double sX0 = sourceInterval.min( 0 );
		final double sX1 = sourceInterval.max( 0 );
		final double sY0 = sourceInterval.min( 1 );
		final double sY1 = sourceInterval.max( 1 );
		final double sZ0 = sourceInterval.min( 2 );
		final double sZ1 = sourceInterval.max( 2 );
		final double sX = ( sX0 + sX1 ) / 2;
		final double sY = ( sY0 + sY1 ) / 2;
		final double sZ = Math.round( ( sZ0 + sZ1 ) / 2 ); // z-slice in the middle of a pixel

		final double[][] m = new double[ 3 ][ 4 ];

		// rotation
		final double[] qSource = new double[ 4 ];
		final double[] qViewer = new double[ 4 ];
		Affine3DHelpers.extractApproximateRotationAffine( sourceTransform, qSource, 2 );
		LinAlgHelpers.quaternionInvert( qSource, qViewer );
		LinAlgHelpers.quaternionToR( qViewer, m );

		// translation
		final double[] centerSource = new double[] { sX, sY, sZ };
		final double[] centerGlobal = new double[ 3 ];
		final double[] translation = new double[ 3 ];
		sourceTransform.apply( centerSource, centerGlobal );
		LinAlgHelpers.quaternionApply( qViewer, centerGlobal, translation );
		LinAlgHelpers.scale( translation, -1, translation );
		LinAlgHelpers.setCol( 3, translation, m );

		final AffineTransform3D viewerTransform = new AffineTransform3D();
		viewerTransform.set( m );

		// scale
		final double[] pSource = new double[] { sX1 + 0.5, sY1 + 0.5, sZ };
		final double[] pGlobal = new double[ 3 ];
		final double[] pScreen = new double[ 3 ];
		sourceTransform.apply( pSource, pGlobal );
		viewerTransform.apply( pGlobal, pScreen );
		final double scaleX = cX / pScreen[ 0 ];
		final double scaleY = cY / pScreen[ 1 ];
		final double scale;
		if ( zoomedIn )
			scale = Math.max( scaleX, scaleY );
		else
			scale = Math.min( scaleX, scaleY );
		viewerTransform.scale( scale );

		// window center offset
		viewerTransform.set( viewerTransform.get( 0, 3 ) + cX - 0.5, 0, 3 );
		viewerTransform.set( viewerTransform.get( 1, 3 ) + cY - 0.5, 1, 3 );
		return viewerTransform;
	}
}
