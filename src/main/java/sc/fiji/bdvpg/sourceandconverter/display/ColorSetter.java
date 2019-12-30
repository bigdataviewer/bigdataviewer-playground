package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;

import java.util.function.Consumer;

public class ColorSetter implements Runnable, Consumer< SourceAndConverter > {

    BdvHandle bdvHandle;
    SourceAndConverter< ? > sac;
    private final ARGBType color;
    double min;
    double max;

    public ColorSetter( final BdvHandle bdvHandle, final SourceAndConverter< ? > sac, ARGBType color )
    {
        this.bdvHandle = bdvHandle;
        this.sac = sac;
        this.color = color;
    }

    @Override
    public void run() {
        accept( sac );
    }

    @Override
    public void accept(SourceAndConverter sac) {

        final ColorConverter converter = asColorConverter( sac.getConverter() );
        converter.setColor( color );

        final ColorConverter vConverter = asColorConverter( sac.asVolatile().getConverter() );
        vConverter.setColor( color );

        bdvHandle.getViewerPanel().requestRepaint();
    }

    public ColorConverter asColorConverter( Converter converter ) {
        if( ! ( converter instanceof ColorConverter ) )
            throw new UnsupportedOperationException( "Cannot adjust brightness of: " + converter.getClass() );
        else return (ColorConverter) converter;
    }
}
