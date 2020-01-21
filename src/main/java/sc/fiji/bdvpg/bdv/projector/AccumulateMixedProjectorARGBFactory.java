package sc.fiji.bdvpg.bdv.projector;

import bdv.util.Bdv;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class AccumulateMixedProjectorARGBFactory implements AccumulateProjectorFactory< ARGBType >
{
	private BdvHandle bdvHandle;

	public AccumulateMixedProjectorARGBFactory()
	{
	}

	public void setBdvHandle( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	@Override
	public VolatileProjector createAccumulateProjector(
			ArrayList< VolatileProjector > sourceProjectors,
			ArrayList< Source< ? > > sources,
			ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			RandomAccessibleInterval< ARGBType > targetScreenImage,
			int numThreads,
			ExecutorService executorService )
	{
		return new AccumulateMixedProjectorARGB(
						bdvHandle,
						sourceProjectors,
						sources,
						sourceScreenImages,
						targetScreenImage,
						numThreads,
						executorService );
	}
}
