package bdv.util;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.RequestRepaint;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.AbstractLinearRange;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;

import java.util.Arrays;
import java.util.List;

public class LUTConverterSetup implements ConverterSetup
{

    protected RequestRepaint viewer;

    protected final List<RealLUTConverter> converters;
    //protected RealLUTConverter converter;

    public LUTConverterSetup(final RealLUTConverter ... converters )
    {
        this( Arrays.< RealLUTConverter >asList( converters ) );
    }

    public LUTConverterSetup(final List< RealLUTConverter > converters  )
    {
        this.converters = converters;
        this.viewer = null;
        AbstractLinearRange alr;
    }


    @Override
    public void setDisplayRange( final double min, final double max )
    {

        for ( final RealLUTConverter converter : converters ) {
            converter.setMin(min);
            converter.setMax(max);
        }
        if ( viewer != null )
            viewer.requestRepaint();
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

    @Override
    public void setViewer( final RequestRepaint viewer )
    {
        this.viewer = viewer;
    }
}
