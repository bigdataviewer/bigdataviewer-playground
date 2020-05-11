package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.log.Logger;
import sc.fiji.bdvpg.log.Logs;
import sc.fiji.bdvpg.log.SystemLogger;

/**
 * BigDataViewer Playground Action -->
 * Action which logs the view transform of a {@link BdvHandle}
 *
 * See {@link ViewTransformSetAndLogDemo} for a usage example
 *
 * @author Robert Haase, MPI CBG
 */

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
		bdvHandle.getViewerPanel().state().getViewerTransform( view );
		logger.out( Logs.BDV + ": Viewer Transform: " + view.toString() );
	}
}
