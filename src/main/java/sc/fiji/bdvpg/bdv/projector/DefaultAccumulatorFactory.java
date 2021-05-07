package sc.fiji.bdvpg.bdv.projector;

import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import bdv.viewer.render.VolatileProjector;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.ARGBType;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class DefaultAccumulatorFactory implements AccumulateProjectorFactory<ARGBType> {
    @Override
    public VolatileProjector createProjector(
            final List< VolatileProjector > sourceProjectors,
            final List<SourceAndConverter< ? >> sources,
            final List< ? extends RandomAccessible< ? extends ARGBType>> sourceScreenImages,
            final RandomAccessibleInterval< ARGBType > targetScreenImage,
            final int numThreads,
            final ExecutorService executorService )
    {
        try
        {
            return new AccumulateProjectorARGB( sourceProjectors, sourceScreenImages, targetScreenImage, numThreads, executorService );
        }
        catch ( IllegalArgumentException ignored )
        {}
        return new AccumulateProjectorARGB.AccumulateProjectorARGBGeneric( sourceProjectors, sourceScreenImages, targetScreenImage, numThreads, executorService );
    }
}
