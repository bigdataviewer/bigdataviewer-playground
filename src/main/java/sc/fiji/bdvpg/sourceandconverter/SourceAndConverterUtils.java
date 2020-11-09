/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.bdvpg.sourceandconverter;

import bdv.*;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.ARGBColorConverterSetup;
import bdv.util.LUTConverterSetup;
import bdv.util.UnmodifiableConverterSetup;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imglib2.*;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import sc.fiji.bdvpg.bdv.BdvUtils;
import sc.fiji.bdvpg.converter.RealARGBColorConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.Displaysettings;
import spimdata.util.DisplaysettingsHelper;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Following the logic of the repository, i.e. dealing with SourceAndConverter objects only,
 * This class contains the main functions which allow to convert objects which can be
 * vizualized in BDV windows into SourceAndConverters objects
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
 * ConverterSetup object. These objects can be used to adjust the B and C of the displayed
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

            nonVolatileConverter = createConverterRealType((RealType) source.getType());

            assert nonVolatileConverter!=null;

            Source volatileSource = createVolatileRealType(source);

            if (volatileSource!=null) {

                Converter volatileConverter = createConverterRealType((RealType) volatileSource.getType());
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

        boolean nonVolatile = WrapBasicImgLoader.wrapImgLoaderIfNecessary( asd );

        if ( nonVolatile )
        {
            System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
        }

        final AbstractSequenceDescription< ?, ?, ? > seq = asd.getSequenceDescription();

            final ViewerImgLoader imgLoader = ( ViewerImgLoader ) seq.getImgLoader();
            for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() ) {
                final int setupId = setup.getId();

                ViewerSetupImgLoader vsil = imgLoader.getSetupImgLoader(setupId);

                final Object type = vsil.getImageType();


                String sourceName = createSetupName(setup);

                if ( RealType.class.isInstance( type ) ) {

                    final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

                    Converter nonVolatileConverter = createConverterRealType((RealType)s.getType());

                    assert nonVolatileConverter!=null;
                    if (!nonVolatile) {

                        final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );

                        Converter volatileConverter = createConverterRealType((RealType)vs.getType());

                        out.put(setupId, new SourceAndConverter(s, nonVolatileConverter,
                                new SourceAndConverter<>(vs, volatileConverter)));

                    } else {

                        out.put(setupId, new SourceAndConverter(s, nonVolatileConverter));
                    }
                    // Metadata need to exist before the display settings (projection mode) are set

                    SourceAndConverterServices.getSourceAndConverterService().register(out.get(setupId));

                    // Applying display settings if some have been set
                    if (setup.getAttribute(Displaysettings.class)!=null) {

                        DisplaysettingsHelper.PullDisplaySettings(out.get(setupId),setup.getAttribute(Displaysettings.class));

                    }

                } else if ( ARGBType.class.isInstance( type ) ) {

                    final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );
                    final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

                    Converter nonVolatileConverter = createConverterARGBType(s);
                    assert nonVolatileConverter!=null;
                    if (vs!=null) {
                        Converter volatileConverter = createConverterARGBType(vs);
                        out.put(setupId, new SourceAndConverter(s, nonVolatileConverter,
                                new SourceAndConverter<>(vs, volatileConverter)));
                    } else {
                        out.put(setupId, new SourceAndConverter(s, nonVolatileConverter));
                    }
                    // Metadata need to exist before the display settings (projection mode) are set
                    SourceAndConverterServices.getSourceAndConverterService().register(out.get(setupId));
                    // Applying display settings if some have been set
                    if (setup.getAttribute(Displaysettings.class)!=null) {
                        DisplaysettingsHelper.PullDisplaySettings(out.get(setupId),setup.getAttribute(Displaysettings.class));
                    }

                } else {
                    errlog.accept("Cannot open Spimdata with Source of type "+type.getClass().getSimpleName());
                }
            }

        WrapBasicImgLoader.removeWrapperIfPresent( asd );

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
            return createConverterRealType((RealType)source.getType());//source);
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
    public static Converter cloneConverter(Converter converter, SourceAndConverter sac) {
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
            //RealARGBColorConverter
            Converter cvt = BigDataViewer.createConverterToARGB((NumericType)sac.getSpimSource().getType());
            if ((converter instanceof ColorConverter)&&(cvt instanceof ColorConverter)) {
                ((ColorConverter) cvt).setColor(((ColorConverter)converter).getColor());
            }

            if ((converter instanceof RealARGBColorConverter)&&(cvt instanceof RealARGBColorConverter)) {
                ((RealARGBColorConverter)cvt).setMin(((RealARGBColorConverter)converter).getMin());
                ((RealARGBColorConverter)cvt).setMax(((RealARGBColorConverter)converter).getMax());
            }

            if (cvt!=null) {
                return cvt;
            }

            errlog.accept("Could not clone the converter of class " + converter.getClass().getSimpleName());
            return null;
        }
    }

    public static ConverterSetup createConverterSetup(SourceAndConverter sac) {
        return  createConverterSetup(sac,-1);
    }

    public static ConverterSetup createConverterSetup(SourceAndConverter sac, int legacyId) {
        //return BigDataViewer.createConverterSetup(sac, legacyId);
        ConverterSetup setup;
        if (sac.getSpimSource().getType() instanceof RealType) {
            setup = createConverterSetupRealType(sac);
        } else if (sac.getSpimSource().getType() instanceof ARGBType) {
            setup = createConverterSetupARGBType(sac);
        } else {
            errlog.accept("Cannot create convertersetup for Source of type "+sac.getSpimSource().getType().getClass().getSimpleName());
            setup = null;
        }
        //setup.setViewer(() -> requestRepaint.run());
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
            log.accept( "Unsupported ConverterSetup for Converters of class " + source.getConverter().getClass() );
            if (source.asVolatile() != null) {
                setup = new UnmodifiableConverterSetup( source.getConverter(), source.asVolatile().getConverter());
            } else {
                setup = new UnmodifiableConverterSetup( source.getConverter());
            }
        }
        return setup;
    }

    private static String createSetupName( final BasicViewSetup setup ) {
        if ( setup.hasName() ) {
            if (!setup.getName().trim().equals("")) {
                return setup.getName();
            }
        }

        String name = "";

        final Angle angle = setup.getAttribute( Angle.class );
        if ( angle != null )
            name += ( name.isEmpty() ? "" : " " ) + "a " + angle.getName();

        final Channel channel = setup.getAttribute( Channel.class );
        if ( channel != null )
            name += ( name.isEmpty() ? "" : " " ) + "c " + channel.getName();

        if ((channel == null)&&(angle == null)) {
            name += "id "+setup.getId();
        }

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
     * @param <T>
     * @return
     */
    private static< T extends RealType< T >>  Converter createConverterRealType(final T type) {
        final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
        final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
        final RealARGBColorConverter< T > converter ;
        if ( type instanceof Volatile)
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
	 * Checks whether a given point (calibrated global coordinate)
	 * lies inside the voxel grid of the Source's underlying RandomAccessibleInterval.
	 *
	 * This can be used as an alternative to below method: isSourcePresentAt
	 *
	 * @param source
	 * @param globalPosition
	 * @param timepoint
	 * @param sourceIs2d
	 *
	 * @return
	 * 			boolean indicating whether the position falls within the source interval
	 */
	public static boolean isPositionWithinSourceInterval( SourceAndConverter< ? > source, RealPoint globalPosition, int timepoint, boolean sourceIs2d )
	{
		Source< ? > spimSource = source.getSpimSource();

		final long[] voxelPositionInSource = getVoxelPositionInSource( spimSource, globalPosition, timepoint, 0 );
		Interval sourceInterval = spimSource.getSource( 0, 0 );

		if ( sourceIs2d )
		{
			final long[] min = new long[ 2 ];
			final long[] max = new long[ 2 ];
			final long[] positionInSource2D = new long[ 2 ];
			for ( int d = 0; d < 2; d++ )
			{
				min[ d ] = sourceInterval.min( d );
				max[ d ] = sourceInterval.max( d );
				positionInSource2D[ d ] = voxelPositionInSource[ d ];
			}

			Interval interval2d = new FinalInterval( min, max );
			Point point2d = new Point( positionInSource2D );

			return Intervals.contains( interval2d, point2d ) ? true : false;
		}
		else
		{
			Interval interval3d = sourceInterval;
			Point point3d = new Point( voxelPositionInSource );

			return Intervals.contains( interval3d, point3d ) ? true : false;
		}
	}

	/**
	 * Given a calibrated global position, this function uses
	 * the source transform to compute the position within the
	 * voxel grid of the source.
	 *
	 * @param source
	 * @param globalPosition
	 * @param t
	 * @param level
	 * @return
	 */
	public static long[] getVoxelPositionInSource(
			final Source source,
			final RealPoint globalPosition,
			final int t,
			final int level )
	{
		final int numDimensions = 3;

		final AffineTransform3D sourceTransform = BdvUtils.getSourceTransform( source, t, level );

		final RealPoint voxelPositionInSource = new RealPoint( numDimensions );

		sourceTransform.inverse().apply( globalPosition, voxelPositionInSource );

		final long[] longPosition = new long[ numDimensions ];

		for ( int d = 0; d < numDimensions; ++d )
			longPosition[ d ] = (long) voxelPositionInSource.getFloatPosition( d );

		return longPosition;
	}

    /**
     * Is the point pt located inside the source  at a particular timepoint ?
     * Looks at highest resolution whether the alpha value of the displayed pixel is zero
     * TODO TO think Alternative : looks whether R, G and B values equal zero - source not present
     * Another option : if the display RGB value is zero, then consider it's not displayed and thus not selected
     * - Convenient way to adjust whether a source should be selected or not ?
     * TODO : Time out if too long to access the data
     * @param sac
     * @param pt
     * @return
     */
    public static boolean isSourcePresentAt(SourceAndConverter sac, int timePoint, RealPoint pt) {

        RealRandomAccessible rra_ible = sac.getSpimSource().getInterpolatedSource(timePoint, 0, Interpolation.NEARESTNEIGHBOR);

        if (rra_ible!=null) {
            // Get transformation of the source
            final AffineTransform3D sourceTransform = new AffineTransform3D();
            sac.getSpimSource().getSourceTransform(timePoint, 0, sourceTransform);

            // Get a access to the source at the pointer location
            RealRandomAccess rra = rra_ible.realRandomAccess();
            RealPoint iPt = new RealPoint(3);
            sourceTransform.inverse().apply(pt, iPt);
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
        } else {
            return false;
        }

    }

    /**
     * Default sorting order for SourceAndConverter
     * Because sometimes we want some consistency in channel ordering when exporting / importing
     *
     * TODO : find a better way to order between spimdata
     * @param sacs
     * @return
     */
    public static List<SourceAndConverter<?>> sortDefaultGeneric(Collection<SourceAndConverter<?>> sacs) {
        List<SourceAndConverter<?>> sortedList = new ArrayList<>(sacs.size());
        sortedList.addAll(sacs);
        Set<AbstractSpimData> spimData = new HashSet<>();
        // Gets all SpimdataInfo
        sacs.forEach(sac -> {
            if (SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)!=null) {
                SourceAndConverterService.SpimDataInfo sdi = ((SourceAndConverterService.SpimDataInfo)(SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)));
                spimData.add(sdi.asd);
            }
        });

        Comparator<SourceAndConverter> sacComparator = (s1, s2) -> {
            // Those who do not belong to spimdata are last:
            SourceAndConverterService.SpimDataInfo sdi1 = null, sdi2 = null;
            if (SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getMetadata(s1, SourceAndConverterService.SPIM_DATA_INFO)!=null) {
                sdi1 = ((SourceAndConverterService.SpimDataInfo)(SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getMetadata(s1, SourceAndConverterService.SPIM_DATA_INFO)));
            }

            if (SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getMetadata(s2, SourceAndConverterService.SPIM_DATA_INFO)!=null) {
                sdi2 = ((SourceAndConverterService.SpimDataInfo)(SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getMetadata(s2, SourceAndConverterService.SPIM_DATA_INFO)));
            }

            if ((sdi1==null)&&(sdi2!=null)) {
                return -1;
            }

            if ((sdi1!=null)&&(sdi2==null)) {
                return 1;
            }

            if ((sdi1!=null)&&(sdi2!=null)) {
                if (sdi1.asd==sdi2.asd) {
                    return sdi1.setupId-sdi2.setupId;
                } else {
                    return sdi2.toString().compareTo(sdi1.toString());
                }
            }

            return s2.getSpimSource().getName().compareTo(s1.getSpimSource().getName());
        };

        sortedList.sort(sacComparator);
        return sortedList;
    }

    /**
     * Default sorting order for SourceAndConverter
     * Because sometimes we want some consistency in channel ordering when exporting / importing
     *
     * TODO : find a better way to order between spimdata
     * @param sacs
     * @return
     */
    public static List<SourceAndConverter> sortDefaultNoGeneric(Collection<SourceAndConverter> sacs) {
        List<SourceAndConverter> sortedList = new ArrayList<>(sacs.size());
        sortedList.addAll(sacs);
        Set<AbstractSpimData> spimData = new HashSet<>();
        // Gets all SpimdataInfo
        sacs.forEach(sac -> {
            if (SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)!=null) {
                SourceAndConverterService.SpimDataInfo sdi = ((SourceAndConverterService.SpimDataInfo)(SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO)));
                spimData.add(sdi.asd);
            }
        });

        Comparator<SourceAndConverter> sacComparator = (s1, s2) -> {
            // Those who do not belong to spimdata are last:
            SourceAndConverterService.SpimDataInfo sdi1 = null, sdi2 = null;
            if (SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getMetadata(s1, SourceAndConverterService.SPIM_DATA_INFO)!=null) {
                sdi1 = ((SourceAndConverterService.SpimDataInfo)(SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getMetadata(s1, SourceAndConverterService.SPIM_DATA_INFO)));
            }

            if (SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getMetadata(s2, SourceAndConverterService.SPIM_DATA_INFO)!=null) {
                sdi2 = ((SourceAndConverterService.SpimDataInfo)(SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getMetadata(s2, SourceAndConverterService.SPIM_DATA_INFO)));
            }

            if ((sdi1==null)&&(sdi2!=null)) {
                return -1;
            }

            if ((sdi1!=null)&&(sdi2==null)) {
                return 1;
            }

            if ((sdi1!=null)&&(sdi2!=null)) {
                if (sdi1.asd==sdi2.asd) {
                    return sdi1.setupId-sdi2.setupId;
                } else {
                    return sdi2.toString().compareTo(sdi1.toString());
                }
            }

            return s2.getSpimSource().getName().compareTo(s1.getSpimSource().getName());
        };

        sortedList.sort(sacComparator);
        return sortedList;
    }

    /**
     * Return the center point in global coordinates of the source
     * Do not expect this to work with WarpedSource
     * @param source
     * @return
     */
    public static RealPoint getSourceAndConverterCenterPoint(SourceAndConverter source) {
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.identity();
        //double[] m = at3D.getRowPackedCopy();
        source.getSpimSource().getSourceTransform(0,0,at3D);
        long[] dims = new long[3];
        source.getSpimSource().getSource(0,0).dimensions(dims);

        RealPoint ptCenterGlobal = new RealPoint(3);
        RealPoint ptCenterPixel = new RealPoint((dims[0]-1.0)/2.0,(dims[1]-1.0)/2.0, (dims[2]-1.0)/2.0);

        at3D.apply(ptCenterPixel, ptCenterGlobal);

        return ptCenterGlobal;
    }


    /**
     * Applies the color converter settings from the src source to the dst sources
     * color, min, max
     * @param src
     * @param dst
     */
    public static void transferColorConverters(SourceAndConverter src, SourceAndConverter dst) {
        transferColorConverters(new SourceAndConverter[]{src}, new SourceAndConverter[]{dst});
    }

    /**
     * Applies the color converter settings from the sources srcs source to the sources dsts
     * color, min, max.
     *
     * If the number of sources is unequal, the transfer is applied up to the common number of sources
     *
     * If null is encountered for src or dst, nothing happens silently
     *
     * The transfer is performed for the volatile source as well if it exists.
     * The volatile source converter of src is ignored
     *
     * Fails si
     * @param srcs
     * @param dsts
     */
    public static void transferColorConverters(SourceAndConverter[] srcs, SourceAndConverter[] dsts) {
        if ((srcs!=null)&&(dsts!=null))
        for (int i = 0;i<Math.min(srcs.length, dsts.length);i++) {
            SourceAndConverter src = srcs[i];
            SourceAndConverter dst = dsts[i];
            if ((src!=null)&&(dst!=null))
            if ((dst.getConverter() instanceof ColorConverter) && (src.getConverter() instanceof ColorConverter)) {
                ColorConverter conv_src = (ColorConverter) src.getConverter();
                ColorConverter conv_dst = (ColorConverter) dst.getConverter();
                conv_dst.setColor(conv_src.getColor());
                conv_dst.setMin(conv_src.getMin());
                conv_dst.setMax(conv_src.getMax());
                if (dst.asVolatile()!=null) {
                    conv_dst = (ColorConverter) dst.asVolatile().getConverter();
                    conv_dst.setColor(conv_src.getColor());
                    conv_dst.setMin(conv_src.getMin());
                    conv_dst.setMax(conv_src.getMax());
                }
            }
        }
    }

}
