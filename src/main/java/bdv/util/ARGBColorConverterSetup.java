package bdv.util;

import bdv.tools.brightness.ConverterSetup;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.listeners.Listeners;

import java.util.Arrays;
import java.util.List;

public class ARGBColorConverterSetup implements ConverterSetup
{

    protected final List<ColorConverter> converters;

    private final Listeners.List< SetupChangeListener > listeners = new Listeners.SynchronizedList<>();

    public ARGBColorConverterSetup( final ColorConverter ... converters )
    {
        this( Arrays.< ColorConverter >asList( converters ) );
    }

    public ARGBColorConverterSetup( final List< ColorConverter > converters )
    {
        this.converters = converters;
    }

    @Override
    public void setDisplayRange( final double min, final double max )
    {
        for ( final ColorConverter converter : converters )
        {
            converter.setMin( min );
            converter.setMax( max );
        }

        listeners.list.forEach(scl -> scl.setupParametersChanged(this));
    }

    @Override
    public void setColor( final ARGBType color )
    {
        for ( final ColorConverter converter : converters )
            converter.setColor( color );

        listeners.list.forEach(scl -> scl.setupParametersChanged(this));
    }

    @Override
    public boolean supportsColor()
    {
        return converters.get( 0 ).supportsColor();
    }

    @Override
    public int getSetupId()
    {
        return 0;
    }

    @Override
    public double getDisplayRangeMin()
    {
        return converters.get( 0 ).getMin();
    }

    @Override
    public double getDisplayRangeMax()
    {
        return converters.get( 0 ).getMax();
    }

    @Override
    public ARGBType getColor()
    {
        return converters.get( 0 ).getColor();
    }

    @Override
    public Listeners<SetupChangeListener> setupChangeListeners() {
        return listeners;
    }
}
