package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;

import java.util.function.Consumer;

public class BrightnessAdjuster implements Runnable, Consumer< SourceAndConverter > {

    BdvHandle bdvHandle;
    SourceAndConverter< ? > sac;
    double min;
    double max;

    public BrightnessAdjuster( final BdvHandle bdvHandle, final SourceAndConverter< ? > sac, double min, double max )
    {
        this.bdvHandle = bdvHandle;
        this.sac = sac;
        this.min = min;
        this.max = max;
    }

    @Override
    public void run() {
        accept( sac );
    }

    @Override
    public void accept(SourceAndConverter sac) {

        final ColorConverter converter = asColorConverter( sac.getConverter() );
        converter.setMin( min );
        converter.setMax( max );

        final ColorConverter vConverter = asColorConverter( sac.asVolatile().getConverter() );
        vConverter.setMin( min );
        vConverter.setMax( max );

        bdvHandle.getViewerPanel().requestRepaint();
    }

    public ColorConverter asColorConverter( Converter converter ) {
        if( ! ( converter instanceof ColorConverter ) )
            throw new UnsupportedOperationException( "Cannot adjust brightness of: " + converter.getClass() );
        else return (ColorConverter) converter;
    }
}
