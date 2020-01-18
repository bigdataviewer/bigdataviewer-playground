package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SacServices;

public class BrightnessAdjuster implements Runnable
{
	private final SourceAndConverter sac;
	private final double min;
	private final double max;

	public BrightnessAdjuster(final SourceAndConverter sac, double min, double max )
	{
		this.sac = sac;
		this.min = min;
		this.max = max;
	}

	@Override
	public void run()
	{
		SacServices.getSacDisplayService().getConverterSetup( sac ).setDisplayRange(min, max);
	}

}
