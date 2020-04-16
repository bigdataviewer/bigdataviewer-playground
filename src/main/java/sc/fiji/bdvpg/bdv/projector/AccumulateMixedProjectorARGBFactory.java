package sc.fiji.bdvpg.bdv.projector;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

public class AccumulateMixedProjectorARGBFactory implements AccumulateProjectorFactory< ARGBType >
{

	public AccumulateMixedProjectorARGBFactory()
	{
	}


	@Override
	public VolatileProjector createProjector(
			ArrayList< VolatileProjector > sourceProjectors,
			ArrayList<SourceAndConverter< ? >> sources,
			ArrayList< ? extends RandomAccessible< ? extends ARGBType > > sourceScreenImages,
			RandomAccessibleInterval< ARGBType > targetScreenImage,
			int numThreads,
			ExecutorService executorService )
	{
		return new AccumulateMixedProjectorARGB(
						sourceProjectors,
						sources,
						sourceScreenImages,
						targetScreenImage,
						numThreads,
						executorService );
	}
}
