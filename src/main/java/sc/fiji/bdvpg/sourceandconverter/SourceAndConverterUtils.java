package sc.fiji.bdvpg.sourceandconverter;

import bdv.SpimSource;
import bdv.ViewerImgLoader;
import bdv.VolatileSpimSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Util;
import sc.fiji.bdvpg.converter.RealARGBColorConverter;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterDisplayService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SourceAndConverterUtils {
    
    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println(BdvSourceAndConverterDisplayService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println(BdvSourceAndConverterDisplayService.class.getSimpleName()+":"+str);
    
    /**
     * Core function : makes SourceAndConverter object out of a Source
     * Mainly duplicates functions from BdvVisTools
     * @param source
     * @return
     */
    static public SourceAndConverter makeSourceAndConverter(Source source) {
        Converter nonVolatileConverter;
        SourceAndConverter out;
        if (source.getType() instanceof RealType) {

            nonVolatileConverter = createConverterRealType(source);

            assert nonVolatileConverter!=null;

            Source volatileSource = createVolatileRealType(source);

            if (volatileSource!=null) {

                Converter volatileConverter = createConverterRealType(volatileSource);
                out = new SourceAndConverter(source, nonVolatileConverter,
                        new SourceAndConverter<>(volatileSource, volatileConverter));

            } else {

                out = new SourceAndConverter(source, nonVolatileConverter);

            }
            
        } else if (source.getType() instanceof ARGBType) {

            nonVolatileConverter = createConverterARGBType(source);

            assert nonVolatileConverter!=null;

            Source volatileSource = createVolatileARGBType(source);

            if (volatileSource!=null) {

                Converter volatileConverter = createConverterARGBType(volatileSource);
                out = new SourceAndConverter(source, nonVolatileConverter,
                        new SourceAndConverter<>(volatileSource, volatileConverter));

            } else {

                out = new SourceAndConverter(source, nonVolatileConverter);

            }

        } else {

            errlog.accept("Cannot create source and converter for sources of type "+source.getType());
            return null;

        }

        return out;
    }

    static public List<SourceAndConverter> makeSourceAndConverters(AbstractSpimData asd) {

        List<SourceAndConverter> out = new ArrayList<>();

        final AbstractSequenceDescription< ?, ?, ? > seq = asd.getSequenceDescription();
        final ViewerImgLoader imgLoader = ( ViewerImgLoader ) seq.getImgLoader();
        for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
        {
            final int setupId = setup.getId();
            final Object type = imgLoader.getSetupImgLoader( setupId ).getImageType();
            if ( RealType.class.isInstance( type ) ) {
                String sourceName = createSetupName(setup);
                final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );
                final SpimSource s = vs.nonVolatile();

                Converter nonVolatileConverter = createConverterRealType(s);
                assert nonVolatileConverter!=null;
                if (vs!=null) {
                    Converter volatileConverter = createConverterRealType(vs);
                    out.add(new SourceAndConverter(s, nonVolatileConverter,
                            new SourceAndConverter<>(vs, volatileConverter)));
                } else {
                    out.add(new SourceAndConverter(s, nonVolatileConverter));
                }

            } else if ( ARGBType.class.isInstance( type ) ) {
                final String setupName = createSetupName( setup );
                final VolatileSpimSource< ARGBType, VolatileARGBType> vs = new VolatileSpimSource<>( asd, setupId, setupName );
                final SpimSource< ARGBType > s = vs.nonVolatile();

                Converter nonVolatileConverter = createConverterARGBType(s);
                assert nonVolatileConverter!=null;
                if (vs!=null) {
                    Converter volatileConverter = createConverterARGBType(vs);
                    out.add(new SourceAndConverter(s, nonVolatileConverter,
                            new SourceAndConverter<>(vs, volatileConverter)));
                } else {
                    out.add(new SourceAndConverter(s, nonVolatileConverter));
                }

            } else {
                errlog.accept("Cannot open Spimdata with Source of type "+type.getClass().getSimpleName());
            }
        }

        return out;
    }

    private static String createSetupName( final BasicViewSetup setup )
    {
        if ( setup.hasName() )
            return setup.getName();

        String name = "";

        final Angle angle = setup.getAttribute( Angle.class );
        if ( angle != null )
            name += ( name.isEmpty() ? "" : " " ) + "a " + angle.getName();

        final Channel channel = setup.getAttribute( Channel.class );
        if ( channel != null )
            name += ( name.isEmpty() ? "" : " " ) + "c " + channel.getName();

        return name;
    }

    private static Source createVolatileRealType(Source source) {
        // TODO unsupported yet
        return null;
    }

    private static Source createVolatileARGBType(Source source) {
        // TODO unsupported yet
        return null;
    }

    /**
     * Creates ARGB converter from a RealTyped source. 
     * Supports Volatile RealTyped or non volatile
     * @param source
     * @param <T>
     * @return
     */
    public static< T extends RealType< T >>  Converter createConverterRealType(Source<T> source) {
        final T type = Util.getTypeFromInterval( source.getSource( 0, 0 ) );
        final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
        final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
        final RealARGBColorConverter< T > converter ;
        if ( source.getType() instanceof Volatile)
            converter = new RealARGBColorConverter.Imp0<>( typeMin, typeMax );
        else
            converter = new RealARGBColorConverter.Imp1<>( typeMin, typeMax );
        converter.setColor( new ARGBType( 0xffffffff ) );

        ((RealARGBColorConverter)converter).getValueToColor().put( 0D, ARGBType.rgba( 0, 0, 0, 0) );
        return converter;
    }

    /**
     * Creates ARGB converter from a RealTyped source. 
     * Supports Volatile ARGBType or non volatile
     * @param source
     * @return
     */
    public static  Converter createConverterARGBType(Source source) {
        final Converter converter ;
        if ( source.getType() instanceof Volatile)
            converter = new ScaledARGBConverter.VolatileARGB( 0, 255 );
        else
            converter = new ScaledARGBConverter.ARGB( 0, 255 );

        // Unsupported
        //converter.getValueToColor().put( 0D, ARGBType.rgba( 0, 0, 0, 0) );
        return converter;
    }
    
    
    
}
