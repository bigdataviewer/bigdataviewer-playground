package sc.fiji.bdvpg.bdv.projector;

import bdv.viewer.render.AccumulateProjectorARGB;
import bdv.viewer.render.AccumulateProjectorFactory;
import net.imglib2.type.numeric.ARGBType;

import java.util.function.Supplier;

public class ProjectorFactory implements Supplier< AccumulateProjectorFactory< ARGBType > >
{
	String projector;

	public ProjectorFactory( String projector )
	{
		this.projector = projector;
	}

	@Override
	public AccumulateProjectorFactory< ARGBType > get()
	{
		switch ( projector ) {
			case Projector.MIXED_PROJECTOR:
				return new AccumulateMixedProjectorARGBFactory();
			case Projector.SUM_PROJECTOR:
				return AccumulateProjectorARGB.factory; // default
			case Projector.AVERAGE_PROJECTOR:
				return AccumulateAverageProjectorARGB.factory;
			default:
				return AccumulateProjectorARGB.factory;
		}
	}
}
