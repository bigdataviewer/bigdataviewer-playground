package sc.fiji.bdv.navigate;

import bdv.util.BdvHandle;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdv.log.Logger;
import sc.fiji.bdv.log.Logs;
import sc.fiji.bdv.log.SystemLogger;

public class ViewerTransformLogger implements Runnable
{
	private final BdvHandle bdvHandle;
	private final Logger logger;

	public ViewerTransformLogger( BdvHandle bdvHandle )
	{
		this( bdvHandle, new SystemLogger() );
	}

	public ViewerTransformLogger( BdvHandle bdvHandle, Logger logger )
	{
		this.bdvHandle = bdvHandle;
		this.logger = logger;
	}

	@Override
	public void run()
	{
		final AffineTransform3D view = new AffineTransform3D();
		bdvHandle.getViewerPanel().getState().getViewerTransform( view );
		logger.out( Logs.BDV + ": Viewer Transform: " + view.toString() );
	}
}
