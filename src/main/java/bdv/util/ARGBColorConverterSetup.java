package bdv.util;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.RequestRepaint;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;

import java.util.Arrays;
import java.util.List;

public class ARGBColorConverterSetup implements ConverterSetup
{

    protected final List<ColorConverter> converters;

    protected RequestRepaint viewer;

    public ARGBColorConverterSetup( final ColorConverter ... converters )
    {
        this( Arrays.< ColorConverter >asList( converters ) );
    }

    public ARGBColorConverterSetup( final List< ColorConverter > converters )
    {
        this.converters = converters;
        this.viewer = null;
    }

    @Override
    public void setDisplayRange( final double min, final double max )
    {
        for ( final ColorConverter converter : converters )
        {
            converter.setMin( min );
            converter.setMax( max );
        }
        if ( viewer != null )
            viewer.requestRepaint();
    }

    @Override
    public void setColor( final ARGBType color )
    {
        for ( final ColorConverter converter : converters )
            converter.setColor( color );
        if ( viewer != null )
            viewer.requestRepaint();
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
    public void setViewer( final RequestRepaint viewer )
    {
        this.viewer = viewer;
    }

    public String toString() {
        return this.getClass().getSimpleName()+" : "+converters.get( 0 ).getColor().toString();
    }
}
