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
import bdv.img.WarpedSource;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ARGBColorConverterSetup;
import bdv.util.LUTConverterSetup;
import bdv.util.ResampledSource;
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
import org.scijava.vecmath.Point3d;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.converter.RealARGBColorConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.Displaysettings;
import spimdata.util.DisplaysettingsHelper;

import java.util.*;
import java.util.function.Consumer;


/**
 * Following the logic of the repository, i.e. dealing with SourceAndConverter objects only,
 * This class contains the main functions which allow to convert objects which can be
 * visualized in BDV windows into SourceAndConverters objects
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
 * - TODO : RAI etc... to SourceAndConverter
 *
 * Additionally, this class contains
 * - default function which allow to create a
 * ConverterSetup object. These objects can be used to adjust the B and C of the displayed
 * SourceAndConverter objects.
 *
 * - Some helper functions for ColorConverter and Displaysettings objects
 *
 *
 * Limitations : TODO : think about CacheControls
 */
public class SourceAndConverterHelper {

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
     * @param source source
     * @return a sourceandconverter from the source
     */
    public static SourceAndConverter createSourceAndConverter(Source source) {
        Converter nonVolatileConverter;
        SourceAndConverter out;
        if (source.getType() instanceof RealType) {

            nonVolatileConverter = createConverterRealType((RealType) source.getType());

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
     * @param asd spimdata
     * @return all sources in a map : id to source
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

                if (type instanceof RealType) {

                    final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

                    Converter nonVolatileConverter = createConverterRealType((RealType)s.getType()); // IN FACT THE CASTING IS NECESSARY!!

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

                } else if (type instanceof ARGBType) {

                    final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );
                    final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

                    Converter nonVolatileConverter = createConverterARGBType(s);
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
     * @param source source
     * @return one converter for the source
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
     * @return the cloned converter
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
     * @param source source
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
     * @param source source
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
     * @param source source
     * @return the volatile source
     */
    private static Source createVolatileRealType(Source source) {
        // TODO unsupported yet
        return null;
    }

    /**
     * Here should go all the ways to build a Volatile Source
     * from a non Volatile Source, ARGBTyped
     * @param source the source
     * @return
     */
    private static Source createVolatileARGBType(Source source) {
        // TODO unsupported yet
        return null;
    }

    /**
     * Creates ARGB converter from a RealTyped sourceandconverter.
     * Supports Volatile RealTyped or non volatile
     * @param <T> realtype class
     * @return a suited converter
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
     * @param source source
     * @return a compatible converter
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
	 * @param source source
	 * @param globalPosition position in global coordinate system
	 * @param timepoint timepoint used
	 * @param sourceIs2d is the source is 2d, avoids checking third dimension
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

			return Intervals.contains(interval2d, point2d);
		}
		else
		{
            Point point3d = new Point( voxelPositionInSource );

			return Intervals.contains(sourceInterval, point3d);
		}
	}

	/**
	 * Given a calibrated global position, this function uses
	 * the source transform to compute the position within the
	 * voxel grid of the source.
     *
     * Probably : do not work with warped sources
	 *
	 * @param source source
	 * @param globalPosition position in global coordinate
	 * @param t time point
	 * @param level mipmap level of the source
	 * @return voxel coordinate
	 */
	public static long[] getVoxelPositionInSource(
			final Source source,
			final RealPoint globalPosition,
			final int t,
			final int level )
	{
		final int numDimensions = 3;

		final AffineTransform3D sourceTransform = BdvHandleHelper.getSourceTransform( source, t, level );

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
     * @param sac source
     * @param pt point
     * @return true if the source is present
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

            return ARGBType.alpha(cValue) != 0;
        } else {
            return false;
        }

    }

    /**
     * Default sorting order for SourceAndConverter
     * Because sometimes we want some consistency in channel ordering when exporting / importing
     *
     * TODO : find a better way to order between spimdata
     * @param sacs sources
     * @return sorted sources according to the default sorter
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
     * @param sacs sources
     * @return ordered sources
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
     * @param source source
     * @return the center point of the source (assuming not warped)
     */
    public static RealPoint getSourceAndConverterCenterPoint(SourceAndConverter source) {
        AffineTransform3D sourceTransform = new AffineTransform3D();
        sourceTransform.identity();

        source.getSpimSource().getSourceTransform(0,0,sourceTransform);
        long[] dims = new long[3];
        source.getSpimSource().getSource(0,0).dimensions(dims);

        RealPoint ptCenterGlobal = new RealPoint(3);
        RealPoint ptCenterPixel = new RealPoint((dims[0]-1.0)/2.0,(dims[1]-1.0)/2.0, (dims[2]-1.0)/2.0);

        sourceTransform.apply(ptCenterPixel, ptCenterGlobal);

        return ptCenterGlobal;
    }


    /**
     * Applies the color converter settings from the src source to the dst sources
     * color, min, max
     * @param src converter source
     * @param dst converter dest
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
     * @param srcs sources source
     * @param dsts sources dest
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

    /**
     * Returns the most appropriate level of a multiresolution source for
     * sampling it at a certain voxel size.
     *
     * To match the resolution, the 'middle dimension' of voxels is used to make comparison
     * with the target size voxsize
     *
     * So if the voxel size is [1.2, 0.8, 50], the value 1.2 is used to compare the levels
     * to the target resolution. This is a way to avoid the complexity of defining the correct
     * pixel size while being also robust to comparing 2d and 3d sources. Indeed 2d sources may
     * have aberrantly defined vox size along the third axis, either way too big or way too small
     * in one case or the other, the missing dimension is ignored, which we hope works
     * in most circumstances.
     *
     * Other complication : the sourceandconverter could be a warped source, or a warped source
     * of a warped source of a transformed source, etc.
     *
     * The proper computation of the level required is complicated, and could be ill defined:
     * Warping can cause local shrinking or expansion such that a single level won't be the
     * best choice for all the image.
     *
     * Here the assumption that we make is that the transforms should not change drastically
     * the scale of the image (which could be clearly wrong). Assuming this, we get to the 'root'
     * of the source and converter and get the voxel value from this root source.
     *
     * Look at the {@link SourceAndConverterHelper#getRootSource(Source, AffineTransform3D)} implementation to
     * see how this search is done
     *
     * So : the source root should be properly scaled from the beginning and weird transformation
     * (like spherical transformed will give wrong results.
     *
     * @param src source
     * @param voxSize target voxel size
     * @return mipmap level fitted for the voxel size
     */
    public static int bestLevel(Source src, int t, double voxSize) {
        List<Double> originVoxSize = new ArrayList<>();
        AffineTransform3D chainedSourceTransform = new AffineTransform3D();
        Source rootOrigin = getRootSource(src, chainedSourceTransform);

        for (int l=0;l<rootOrigin.getNumMipmapLevels();l++) {
            AffineTransform3D sourceTransform = new AffineTransform3D();
            rootOrigin.getSourceTransform(t,l,sourceTransform);
            double mid = getCharacteristicVoxelSize(sourceTransform.concatenate(chainedSourceTransform));
            originVoxSize.add(mid);
        }

        int level = 0;
        while((originVoxSize.get(level)<voxSize)&&(level<originVoxSize.size()-1)) {
            level=level+1;
        }

        return Math.max(level-1,0);
    }

    /**
     * See {@link SourceAndConverterHelper#bestLevel(Source, int, double)}
     * @param sac source
     * @param t timepoint
     * @param voxSize target voxel size
     * @return mipmap level chosen
     */
    public static int bestLevel(SourceAndConverter sac, int t, double voxSize) {
        return bestLevel(sac.getSpimSource(), t, voxSize);
    }
    
    /**
     * See {@link SourceAndConverterHelper#bestLevel(Source, int, double)}
     * for an example of the use of this function
     * 
     * What the 'root' means is actually the origin source from which is derived the source
     * so if a source has been affine tranformed, warped, resampled, potentially in successive steps
     * this function should return the source it was derived from.
     * 
     * This function is used (for the moment) only when a source needs to be resampled
     * see {@link ResampledSource}, in order to get the origin voxel size  of the source root.
     * 
     * TODO : maybe use inspector to improve this root finding
     *
     * provide an AffineTransform which would  mutated and concatenated such as the voxel size changes can be taken
     * into account, provided that the transform are affine. Is the source is transformed in a more
     * complex way, then nothing can be done easily...
     *
     * @param source source
     * @return the root source : it's not derived from another source
     */
    public static Source getRootSource(Source source, AffineTransform3D chainedSourceTransform) {
        Source rootOrigin = source;
        while ((rootOrigin instanceof WarpedSource)
                ||(rootOrigin instanceof TransformedSource)
                ||(rootOrigin instanceof ResampledSource)) {
            if (rootOrigin instanceof WarpedSource) {
                rootOrigin = ((WarpedSource) rootOrigin).getWrappedSource();
            } else if (rootOrigin instanceof TransformedSource) {
                AffineTransform3D m = new AffineTransform3D();
                ((TransformedSource) rootOrigin).getFixedTransform(m);
                chainedSourceTransform.concatenate(m);
                rootOrigin = ((TransformedSource) rootOrigin).getWrappedSource();
            } else if (rootOrigin instanceof ResampledSource) {
                rootOrigin = ((ResampledSource) rootOrigin).getModelResamplerSource();
            }
        }
        return rootOrigin;
    }

    /**
     * see {@link SourceAndConverterHelper#getCharacteristicVoxelSize(AffineTransform3D)}
     * @param sac source
     * @param t timepoiont
     * @param level mipmap level
     * @return the characteristic voxel size for this level
     */
    public static double getCharacteristicVoxelSize(SourceAndConverter sac, int t, int level) {
        return getCharacteristicVoxelSize(sac.getSpimSource(), t, level);
    }

    /**
     * See {@link SourceAndConverterHelper#getCharacteristicVoxelSize(AffineTransform3D)}
     * @param src source
     * @param t timepoint
     * @param level mipmap level
     * @return the characteristic voxel size
     */
    public static double getCharacteristicVoxelSize(Source src, int t, int level) {
        AffineTransform3D chainedSourceTransform = new AffineTransform3D();
        Source root = getRootSource(src, chainedSourceTransform);

        AffineTransform3D sourceTransform = new AffineTransform3D();
        root.getSourceTransform(t, level, sourceTransform);

        return getCharacteristicVoxelSize(sourceTransform.concatenate(chainedSourceTransform));
    }

    /**
     * See {@link SourceAndConverterHelper#bestLevel(Source, int, double)}
     * for a description of what the 'characteristic voxel size' means
     * 
     * @param sourceTransform affine transform of the source
     * @return voxel size inferred from this transform
     */
    public static double getCharacteristicVoxelSize(AffineTransform3D sourceTransform) { // method also present in resampled source
        // Gets three vectors
        Point3d v1 = new Point3d(sourceTransform.get(0,0), sourceTransform.get(0,1), sourceTransform.get(0,2));
        Point3d v2 = new Point3d(sourceTransform.get(1,0), sourceTransform.get(1,1), sourceTransform.get(1,2));
        Point3d v3 = new Point3d(sourceTransform.get(2,0), sourceTransform.get(2,1), sourceTransform.get(2,2));

        // 0 - Ensure v1 and v2 have the same norm
        double a = Math.sqrt(v1.x*v1.x+v1.y*v1.y+v1.z*v1.z);
        double b = Math.sqrt(v2.x*v2.x+v2.y*v2.y+v2.z*v2.z);
        double c = Math.sqrt(v3.x*v3.x+v3.y*v3.y+v3.z*v3.z);

        return Math.max(Math.min(a,b), Math.min(Math.max(a,b),c)); //https://stackoverflow.com/questions/1582356/fastest-way-of-finding-the-middle-value-of-a-triple
    }

}
