package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.DiscreteFrequencyDistribution;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.services.BdvService;

public class BrightnessAdjuster implements Runnable
{
	private final SourceAndConverter source;
	private final double min;
	private final double max;

	public BrightnessAdjuster(final SourceAndConverter source, double min, double max )
	{
		this.source = source;
		this.min = min;
		this.max = max;
	}

	@Override
	public void run()
	{
		BdvService.getSourceDisplayService().getConverterSetup(source).setDisplayRange(min, max);
	}

}
