package sc.fiji.bdvpg.sourceandconverter;

import bdv.AbstractSpimSource;
import bdv.SpimSource;
import bdv.ViewerImgLoader;
import bdv.VolatileSpimSource;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ARGBColorConverterSetup;
import bdv.util.BdvHandle;
import bdv.util.LUTConverterSetup;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import sc.fiji.bdvpg.converter.RealARGBColorConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import spimdata.util.DisplaySettings;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * Following the logic of the repository, i.e. dealing with SourceAndConverter objects only,
 * This class contains the main functions which allow to convert objects which can be
 * vizualized in Bdv windows into SourceAndConverters objects
 * SourceAndConverters objects contains:
 * - a Source, non volatile, which holds the data
 * - a converter from the Source type to ARGBType, for display purpose
 * - (optional) a volatile Source, which can be used for fast display and lazy processing
 * - a converter from the volatile Source type to VolatileARGBType, fot display purpose
 *
 * Mainly thie class supports RealTyped source and ARGBTyped source
 * It can deal wth conversion of:
 * - Source to SourceAndConverter
 * - Spimdata to a List of SourceAndConverter
 * - TODO : RAI, ImagePLUS, etc... to SourceAndConverter
 *
 * Additionally, this class contains default function which allow to create a
 * ConverterSetup object. These objects can be used to adjust the B&C of the displayed
 * SourceAndConverter objects.
 *
 * Limitations : TODO : think about CacheControls
 */
public class SourceAndConverterUtils {
    
    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println( SourceAndConverterBdvDisplayService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println( SourceAndConverterBdvDisplayService.class.getSimpleName()+":"+str);
    
    /**
     * Core function : makes SourceAndConverter object out of a Source
     * Mainly duplicated functions from BdvVisTools
     * @param source
     * @return
     */
    public static SourceAndConverter createSourceAndConverter(Source source) {
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

            errlog.accept("Cannot create sourceandconverter and converter for sources of type "+source.getType());
            return null;

        }

        return out;
    }

    /**
     *
     * @param asd
     * @return
     */
    static public Map<Integer, SourceAndConverter> createSourceAndConverters(AbstractSpimData asd) {

        Map<Integer, SourceAndConverter> out = new HashMap<>();

        final AbstractSequenceDescription< ?, ?, ? > seq = asd.getSequenceDescription();
        final ViewerImgLoader imgLoader = ( ViewerImgLoader ) seq.getImgLoader();
        for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
        {
            final int setupId = setup.getId();
            final Object type = imgLoader.getSetupImgLoader( setupId ).getImageType();
            if ( RealType.class.isInstance( type ) ) {
                String sourceName = createSetupName(setup);
                final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );
                final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

                Converter nonVolatileConverter = createConverterRealType(s);
                assert nonVolatileConverter!=null;
                if (vs!=null) {
                    Converter volatileConverter = createConverterRealType(vs);
                    out.put(setupId, new SourceAndConverter(s, nonVolatileConverter,
                            new SourceAndConverter<>(vs, volatileConverter)));
                } else {
                    out.put(setupId, new SourceAndConverter(s, nonVolatileConverter));
                }

                // Applying display settings if some have been set
                if (setup.getAttribute(DisplaySettings.class)!=null) {
                    DisplaySettings.PullDisplaySettings(out.get(setupId),setup.getAttribute(DisplaySettings.class));
                }

            } else if ( ARGBType.class.isInstance( type ) ) {
                final String setupName = createSetupName( setup );
                final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, setupName ); // < ARGBType, VolatileARGBType>
                final SpimSource s = new SpimSource<>( asd, setupId, setupName );
                //final SpimSource< ARGBType > s = vs.nonVolatile();

                Converter nonVolatileConverter = createConverterARGBType(s);
                assert nonVolatileConverter!=null;
                if (vs!=null) {
                    Converter volatileConverter = createConverterARGBType(vs);
                    out.put(setupId, new SourceAndConverter(s, nonVolatileConverter,
                            new SourceAndConverter<>(vs, volatileConverter)));
                } else {
                    out.put(setupId, new SourceAndConverter(s, nonVolatileConverter));
                }

                // Applying display settings if some have been set
                if (setup.getAttribute(DisplaySettings.class)!=null) {
                    DisplaySettings.PullDisplaySettings(out.get(setupId),setup.getAttribute(DisplaySettings.class));
                }

            } else {
                errlog.accept("Cannot open Spimdata with Source of type "+type.getClass().getSimpleName());
            }
        }

        return out;
    }

    /**
     * Creates default converters for a Source
     * Support Volatile or non Volatile
     * Support RealTyped or ARGBTyped
     * @param source
     * @return
     */
    public static Converter createConverter(Source source) {
        if (source.getType() instanceof RealType) {
            return createConverterRealType(source);
        } else if (source.getType() instanceof ARGBType) {
            return createConverterARGBType(source);
        } else {
            errlog.accept("Cannot create converter for sourceandconverter of type "+source.getType().getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Clones a converter
     * TODO :
     * @return
     */
    public static Converter cloneConverter(Converter converter) {
        if (converter instanceof RealARGBColorConverter.Imp0) {
            RealARGBColorConverter.Imp0 out = new RealARGBColorConverter.Imp0<>( ((RealARGBColorConverter.Imp0) converter).getMin(), ((RealARGBColorConverter.Imp0) converter).getMax() );
            out.setColor(((RealARGBColorConverter.Imp0) converter).getColor());
            // For averaging
            // TODO : modularizes this / set as optional
            out.getValueToColor().put( 0D, ARGBType.rgba( 0, 0, 0, 0) );
            return out;
        } else if (converter instanceof RealARGBColorConverter.Imp1) {
            RealARGBColorConverter.Imp1 out = new RealARGBColorConverter.Imp1<>( ((RealARGBColorConverter.Imp1) converter).getMin(), ((RealARGBColorConverter.Imp1) converter).getMax() );
            out.setColor(((RealARGBColorConverter.Imp1) converter).getColor());
            // For averaging
            // TODO : modularizes this / set as optional
            out.getValueToColor().put( 0D, ARGBType.rgba( 0, 0, 0, 0) );
            return out;
        } else if (converter instanceof ScaledARGBConverter.VolatileARGB) {
            return new ScaledARGBConverter.VolatileARGB(((ScaledARGBConverter.VolatileARGB) converter).getMin(), ((ScaledARGBConverter.VolatileARGB) converter).getMax());
        } else if (converter instanceof ScaledARGBConverter.ARGB) {
            return new ScaledARGBConverter.ARGB(((ScaledARGBConverter.ARGB) converter).getMin(),((ScaledARGBConverter.ARGB) converter).getMax());
        } else if (converter instanceof RealLUTConverter) {
            return new RealLUTConverter(((RealLUTConverter) converter).getMin(),((RealLUTConverter) converter).getMax(),((RealLUTConverter) converter).getLUT());
        } else {
            errlog.accept("Could not clone the converter of class " + converter.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Creates a standard convertersetup for a source and converter
     * Switch based on pixel type (ARGBType and RealType supported)
     * @param sac
     * @param requestRepaint
     * @return
     */
    public static ConverterSetup createConverterSetup(SourceAndConverter sac, Runnable requestRepaint) {
        ConverterSetup setup;
        if (sac.getSpimSource().getType() instanceof RealType) {
            setup = createConverterSetupRealType(sac);
        } else if (sac.getSpimSource().getType() instanceof ARGBType) {
            setup = createConverterSetupARGBType(sac);
        } else {
            errlog.accept("Cannot create convertersetup for Source of type "+sac.getSpimSource().getType().getClass().getSimpleName());
            setup = null;
        }
        setup.setViewer(() -> requestRepaint.run());
        return setup;
    }

    /**
     * Creates converters and convertersetup for a ARGB typed sourceandconverter
     * @param source
     */
    static private ConverterSetup createConverterSetupARGBType(SourceAndConverter source) {
        ConverterSetup setup;
        if (source.getConverter() instanceof ColorConverter) {
            if (source.asVolatile()!=null) {
                setup = new ARGBColorConverterSetup( (ColorConverter) source.getConverter(), (ColorConverter) source.asVolatile().getConverter() );
            } else {
                setup = new ARGBColorConverterSetup( (ColorConverter) source.getConverter());
            }
        } else {
            errlog.accept("Cannot build ConverterSetup for Converters of class "+source.getConverter().getClass());
            setup = null;
        }
        return setup;
    }

    /**
     * Creates converters and convertersetup for a real typed sourceandconverter
     * @param source
     */
    static private ConverterSetup createConverterSetupRealType(SourceAndConverter source) {
        final ConverterSetup setup;
        if (source.getConverter() instanceof ColorConverter) {
            if (source.asVolatile() != null) {
                setup = new ARGBColorConverterSetup((ColorConverter) source.getConverter(), (ColorConverter) source.asVolatile().getConverter());
            } else {
                setup = new ARGBColorConverterSetup((ColorConverter) source.getConverter());
            }
        } else if (source.getConverter() instanceof RealLUTConverter) {
            if (source.asVolatile() != null) {
                setup = new LUTConverterSetup((RealLUTConverter) source.getConverter(), (RealLUTConverter) source.asVolatile().getConverter());
            } else {
                setup = new LUTConverterSetup((RealLUTConverter) source.getConverter());
            }
        } else {
            // Here different kinds of Converter can be supported
            errlog.accept("Cannot build ConverterSetup for Converters of class "+source.getConverter().getClass());
            setup = null;
        }
        return setup;
    }

    private static String createSetupName( final BasicViewSetup setup ) {
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

    /**
     * Here should go all the ways to build a Volatile Source
     * from a non Volatile Source, RealTyped
     * @param source
     * @return
     */
    private static Source createVolatileRealType(Source source) {
        // TODO unsupported yet
        return null;
    }

    /**
     * Here should go all the ways to build a Volatile Source
     * from a non Volatile Source, ARGBTyped
     * @param source
     * @return
     */
    private static Source createVolatileARGBType(Source source) {
        // TODO unsupported yet
        return null;
    }

    /**
     * Creates ARGB converter from a RealTyped sourceandconverter.
     * Supports Volatile RealTyped or non volatile
     * @param source
     * @param <T>
     * @return
     */
    private static< T extends RealType< T >>  Converter createConverterRealType(Source<T> source) {
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
     * Creates ARGB converter from a RealTyped sourceandconverter.
     * Supports Volatile ARGBType or non volatile
     * @param source
     * @return
     */
    private static Converter createConverterARGBType(Source source) {
        final Converter converter ;
        if ( source.getType() instanceof Volatile)
            converter = new ScaledARGBConverter.VolatileARGB( 0, 255 );
        else
            converter = new ScaledARGBConverter.ARGB( 0, 255 );

        // Unsupported
        //converter.getValueToColor().put( 0D, ARGBType.rgba( 0, 0, 0, 0) );
        return converter;
    }

    /**
     * Is the point pt located inside the source  at a particular timepoint ?
     * Looks at highest resolution whether the alpha value of the displayed pixel is zero
     * TODO TO think Alternative : looks whether R, G and B values equal zero -> source not present
     * Another option : if the display RGB value is zero, then consider it's not displayed and thus not selected
     * -> Convenient way to adjust whether a source should be selected or not ?
     * TODO : Time out if too long to access the data
     * @param sac
     * @param pt
     * @return
     */
    public static boolean isSourcePresentAt(SourceAndConverter sac, int timePoint, RealPoint pt) {

        RealRandomAccessible rra_ible = sac.getSpimSource().getInterpolatedSource(timePoint, 0, Interpolation.NEARESTNEIGHBOR);

        // Get transformation of the source
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        sac.getSpimSource().getSourceTransform(timePoint, 0, sourceTransform);

        // Get a access to the source at the pointer location
        RealRandomAccess rra = rra_ible.realRandomAccess();
        RealPoint iPt = new RealPoint(3);
        sourceTransform.inverse().apply(pt,iPt);
        rra.setPosition(iPt);

        // Gets converter -> will decide based on ARGB value whether the source is present or not
        Converter<Object, ARGBType> cvt = sac.getConverter();
        ARGBType colorOut = new ARGBType();
        cvt.convert(rra.get(), colorOut);

        // Gets ARGB int value
        int cValue = colorOut.get();

        // Alpha == 0 -> not present, otherwise it is present
        boolean ans = ARGBType.alpha(cValue) != 0;

        return ans;
    }


    /**
     * if a source has a linked spimdata, mutates the last registration to account for changes
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter mutateLastSpimdataTransformation(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sac).containsKey(SPIM_DATA_INFO);
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sac).get(SPIM_DATA_INFO) instanceof SourceAndConverterService.SpimDataInfo;

        SourceAndConverterService.SpimDataInfo sdi = ((SourceAndConverterService.SpimDataInfo)
                SourceAndConverterServices.getSourceAndConverterService()
                        .getSacToMetadata().get(sac).get(SPIM_DATA_INFO));

        // TODO : find a way to pass the ref of starter into this function ? but static looks great...
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        int timePoint = bdvHandle.getViewerPanel().getState().getCurrentTimepoint();

        ViewRegistration vr = sdi.asd.getViewRegistrations().getViewRegistration(timePoint,sdi.setupId);

        ViewTransform vt = vr.getTransformList().get(vr.getTransformList().size()-1);

        AffineTransform3D at3D = new AffineTransform3D();
        at3D.concatenate(vt.asAffine3D());
        at3D.preConcatenate(affineTransform3D);

        ViewTransform newvt = new ViewTransformAffine(vt.getName(), at3D);

        vr.getTransformList().remove(vt);
        vr.getTransformList().add(newvt);
        vr.updateModel();


        try {
            Method updateBdvSource = Class.forName("bdv.AbstractSpimSource").getDeclaredMethod("loadTimepoint", int.class);
            updateBdvSource.setAccessible(true);
            AbstractSpimSource ass = (AbstractSpimSource) sac.getSpimSource();
            updateBdvSource.invoke(ass, timePoint);

            if (sac.asVolatile() != null) {
                ass = (AbstractSpimSource) sac.asVolatile().getSpimSource();
                updateBdvSource.invoke(ass, timePoint);
            }

        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sac;
    }

    /**
     * if a source has a linked spimdata, appends a new transformation in the registration model
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter appendNewSpimdataTransformation(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sac).containsKey(SPIM_DATA_INFO);
        assert SourceAndConverterServices
                .getSourceAndConverterService()
                .getSacToMetadata().get(sac).get(SPIM_DATA_INFO) instanceof SourceAndConverterService.SpimDataInfo;

        SourceAndConverterService.SpimDataInfo sdi = ((SourceAndConverterService.SpimDataInfo)
                SourceAndConverterServices.getSourceAndConverterService()
                        .getSacToMetadata().get(sac).get(SPIM_DATA_INFO));

        // TODO : find a way to pass the ref of starter into this function ? but static looks great...
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        int timePoint = bdvHandle.getViewerPanel().getState().getCurrentTimepoint();

        ViewTransform newvt = new ViewTransformAffine("Manual transform", affineTransform3D);

        sdi.asd.getViewRegistrations().getViewRegistration(timePoint,sdi.setupId).preconcatenateTransform(newvt);
        sdi.asd.getViewRegistrations().getViewRegistration(timePoint,sdi.setupId).updateModel();

        try {
            Method updateBdvSource = Class.forName("bdv.AbstractSpimSource").getDeclaredMethod("loadTimepoint", int.class);
            updateBdvSource.setAccessible(true);
            AbstractSpimSource ass = (AbstractSpimSource) sac.getSpimSource();
            updateBdvSource.invoke(ass, timePoint);

            if (sac.asVolatile() != null) {
                ass = (AbstractSpimSource) sac.asVolatile().getSpimSource();
                updateBdvSource.invoke(ass, timePoint);
            }

        } catch (ClassCastException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sac;
    }

    /**
     *
     * branch between mutateTransformedSourceAndConverter and mutateLastSpimdataTransformation depending  on the source class
     *
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter mutate(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        if (sac.getSpimSource() instanceof AbstractSpimSource) {
            if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac,SPIM_DATA_INFO)!=null) {
                return mutateLastSpimdataTransformation(affineTransform3D, sac);
            } else {
                if (sac.getSpimSource() instanceof TransformedSource) {
                    return mutateTransformedSourceAndConverter(affineTransform3D,sac);
                } else {
                    return createNewTransformedSourceAndConverter(affineTransform3D,sac);
                }
            }
        } else if (sac.getSpimSource() instanceof TransformedSource) {
            return mutateTransformedSourceAndConverter(affineTransform3D,sac);
        } else {
            return createNewTransformedSourceAndConverter(affineTransform3D,sac);
        }
    }

    /**
     *  branch between createNewTransformedSourceAndConverter and appendNewSpimdataTransformation depending on the source class
     *
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter append(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        if (sac.getSpimSource() instanceof AbstractSpimSource) {
            if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac,SPIM_DATA_INFO)!=null) {
                return appendNewSpimdataTransformation(affineTransform3D, sac);
            } else {
                return createNewTransformedSourceAndConverter(affineTransform3D,sac);
            }
        } else {
            return createNewTransformedSourceAndConverter(affineTransform3D,sac);
        }
    }

    /**
     * Ignores registration
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter cancel(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        return sac;
    }

    /**
     * Wraps into transformed sources the registered sources
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter createNewTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        SourceAndConverter transformedSac = new SourceAffineTransformer(sac, affineTransform3D).getSourceOut();
        return transformedSac;
    }

    /**
     * provided a source was already a trasnformed source, updates the inner affineTransform3D
     * @param affineTransform3D
     * @param sac
     * @return
     */
    public static SourceAndConverter mutateTransformedSourceAndConverter(AffineTransform3D affineTransform3D, SourceAndConverter sac) {
        assert sac.getSpimSource() instanceof TransformedSource;
        AffineTransform3D at3D = new AffineTransform3D();
        ((TransformedSource)sac.getSpimSource()).getFixedTransform(at3D);
        ((TransformedSource)sac.getSpimSource()).setFixedTransform(at3D.preConcatenate(affineTransform3D));
        return sac;
    }

}
