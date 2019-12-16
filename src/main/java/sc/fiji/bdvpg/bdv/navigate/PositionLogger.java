package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import net.imglib2.RealPoint;
import sc.fiji.bdvpg.log.Logger;
import sc.fiji.bdvpg.log.Logs;
import sc.fiji.bdvpg.log.SystemLogger;

public class PositionLogger implements Runnable
{
	private final BdvHandle bdvHandle;
	private final Logger logger;

	public PositionLogger( BdvHandle bdvHandle )
	{
		this( bdvHandle, new SystemLogger() );
	}

	public PositionLogger( BdvHandle bdvHandle, Logger logger )
	{
		this.bdvHandle = bdvHandle;
		this.logger = logger;
	}

	@Override
	public void run()
	{
		final RealPoint realPoint = new RealPoint( 3 );
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( realPoint );
		logger.out( Logs.BDV + ": Position at Mouse: " + realPoint.toString() );
	}
}
