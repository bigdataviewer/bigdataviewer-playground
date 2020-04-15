package bdv.util;

import bdv.tools.brightness.ConverterSetup;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.type.numeric.ARGBType;

import java.util.Arrays;
import java.util.List;
import org.scijava.listeners.*;

public class LUTConverterSetup implements ConverterSetup
{
    protected final List<RealLUTConverter> converters;

    private final Listeners.List< SetupChangeListener > listeners = new Listeners.SynchronizedList<>();

    public LUTConverterSetup(final RealLUTConverter ... converters )
    {
        this( Arrays.< RealLUTConverter >asList( converters ) );
    }

    public LUTConverterSetup(final List< RealLUTConverter > converters  )
    {
        this.converters = converters;
    }

    @Override
    public void setDisplayRange( final double min, final double max )
    {
        for ( final RealLUTConverter converter : converters ) {
            converter.setMin(min);
            converter.setMax(max);
        }

        listeners.list.forEach(scl -> scl.setupParametersChanged(this));
    }

    @Override
    public void setColor( final ARGBType color )
    {
        // Do nothing : unsupported
    }

    @Override
    public boolean supportsColor()
    {
        return false;
    }

    @Override
    public Listeners<SetupChangeListener> setupChangeListeners() {
        return listeners;
    }

    @Override
    public int getSetupId()
    {
        return 0;
    }

    @Override
    public double getDisplayRangeMin()
    {
        return converters.get(0).getMin();
    }

    @Override
    public double getDisplayRangeMax()
    {
        return converters.get(0).getMax();
    }

    @Override
    public ARGBType getColor()
    {
        return null;
    }

}
