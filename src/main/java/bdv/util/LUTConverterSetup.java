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

    protected RealLUTConverter converter;

    public LUTConverterSetup(final RealLUTConverter converter )
    {
        this.converter = converter;
        this.viewer = null;
        AbstractLinearRange alr;
    }


    @Override
    public void setDisplayRange( final double min, final double max )
    {
        converter.setMin( min );
        converter.setMax( max );
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
        return converter.getMin();
    }

    @Override
    public double getDisplayRangeMax()
    {
        return converter.getMax();
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
