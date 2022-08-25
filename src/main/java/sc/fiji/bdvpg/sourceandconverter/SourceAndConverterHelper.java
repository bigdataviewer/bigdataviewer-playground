/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import bdv.AbstractSpimSource;
import bdv.BigDataViewer;
import bdv.img.WarpedSource;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.util.LUTConverterSetup;
import bdv.util.ResampledSource;
import bdv.util.UnmodifiableConverterSetup;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Intervals;
import org.scijava.vecmath.Point3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Following the logic of the repository, i.e. dealing with SourceAndConverter
 * objects only, This class contains the main functions which allow to convert
 * objects which can be visualized in BDV windows into SourceAndConverters
 * objects SourceAndConverters objects contains: - a Source, non volatile, which
 * holds the data - a converter from the Source type to ARGBType, for display
 * purpose - (optional) a volatile Source, which can be used for fast display
 * and lazy processing - a converter from the volatile Source type to
 * VolatileARGBType, fot display purpose Mainly this class supports RealTyped
 * source and ARGBTyped source It can deal wth conversion of: - Source to
 * SourceAndConverter - Spimdata to a List of SourceAndConverter - TODO : RAI
 * etc... to SourceAndConverter Additionally, this class contains - default
 * function which allow to create a ConverterSetup object. These objects can be
 * used to adjust the B and C of the displayed SourceAndConverter objects. -
 * Some helper functions for ColorConverter and Displaysettings objects
 * Limitations : TODO : think about CacheControls
 */
public class SourceAndConverterHelper {

	protected static final Logger logger = LoggerFactory.getLogger(
		SourceAndConverterHelper.class);

	/**
	 * Core function : makes SourceAndConverter object out of a Source Mainly
	 * duplicated functions from BdvVisTools
	 * 
	 * @param source source
	 * @return a SourceAndConverter from the source
	 */
	public static <T> SourceAndConverter<T> createSourceAndConverter(
		Source<T> source)
	{
		if (source.getType() instanceof RealType) {
			return (SourceAndConverter<T>) createSourceAndConverterRealType(
				(Source<? extends RealType>) source);
		}
		else if (source.getType() instanceof ARGBType) {
			return (SourceAndConverter<T>) createSourceAndConverterARGBType(
				(Source<ARGBType>) source);
		}
		else {
			logger.error(
				"Cannot create SourceAndConverter and converter for sources of type " +
					source.getType());
			return null;
		}
	}

	private static <T extends RealType<T>, V extends Volatile<T> & RealType>
		SourceAndConverter<T> createSourceAndConverterRealType(Source<T> source)
	{
		Converter<T, ARGBType> nonVolatileConverter = createConverterRealType(source
			.getType());
		try {
			Source<V> volatileSource = createVolatileRealType(source);
			Converter<V, ARGBType> volatileConverter = createConverterRealType(
				volatileSource.getType());
			return new SourceAndConverter<>(source, nonVolatileConverter,
				new SourceAndConverter<>(volatileSource, volatileConverter));
		}
		catch (Exception e) {
			return new SourceAndConverter<>(source, nonVolatileConverter);
		}
	}

	private static SourceAndConverter<ARGBType> createSourceAndConverterARGBType(
		Source<ARGBType> source)
	{
		Converter<ARGBType, ARGBType> nonVolatileConverter =
			new ScaledARGBConverter.ARGB(0, 255);
		try {
			Source<VolatileARGBType> volatileSource = createVolatileARGBType(source);
			Converter<VolatileARGBType, ARGBType> volatileConverter =
				new ScaledARGBConverter.VolatileARGB(0, 255);
			return new SourceAndConverter<>(source, nonVolatileConverter,
				new SourceAndConverter<>(volatileSource, volatileConverter));
		}
		catch (Exception e) {
			return new SourceAndConverter<>(source, nonVolatileConverter);
		}
	}

	/**
	 * Creates default converters for a Source Support Volatile or non-volatile
	 * Support RealTyped or ARGBTyped
	 * 
	 * @param source source
	 * @return one converter for the source
	 */
	public static <T> Converter<T, ARGBType> createConverter(
		Source<? extends T> source)
	{
		if (source.getType() instanceof RealType) {
			return (Converter<T, ARGBType>) (createConverterRealType(
				(RealType) (source.getType())));// source);
		}
		else if (source.getType() instanceof ARGBType) {
			return (Converter<T, ARGBType>) (createConverterARGBType(source));
		}
		else {
			logger.error("Cannot create converter for SourceAndConverter of type " +
				source.getType().getClass().getSimpleName());
			return null;
		}
	}

	/**
	 * @param converter to clone
	 * @param sac source using this converter, useful to retrieve extra
	 *          information if necessary to clone the converter
	 * @return a cloned converter ( could be the same instance ?)
	 */
	public static <I, O> Converter<I, O> cloneConverter(Converter<I, O> converter,
		SourceAndConverter<?> sac)
	{
		if (converter instanceof ICloneableConverter) { // Extensibility of
																										// converters which
																										// implements
																										// ICloneableConverter
			return (Converter<I, O>) ((ICloneableConverter) converter)
				.duplicateConverter(sac);
		}
		else if (converter instanceof ScaledARGBConverter.VolatileARGB) {
			return (Converter<I, O>) new ScaledARGBConverter.VolatileARGB(
				((ScaledARGBConverter.VolatileARGB) converter).getMin(),
				((ScaledARGBConverter.VolatileARGB) converter).getMax());
		}
		else if (converter instanceof ScaledARGBConverter.ARGB) {
			return (Converter<I, O>) new ScaledARGBConverter.ARGB(
				((ScaledARGBConverter.ARGB) converter).getMin(),
				((ScaledARGBConverter.ARGB) converter).getMax());
		}
		else if (converter instanceof RealLUTConverter) {
			return (Converter<I, O>) new RealLUTConverter(
				((RealLUTConverter) converter).getMin(), ((RealLUTConverter) converter)
					.getMax(), ((RealLUTConverter) converter).getLUT());
		}
		else {

			Converter clonedConverter = BigDataViewer.createConverterToARGB(
				(NumericType) sac.getSpimSource().getType());

			if (clonedConverter != null) {
				if ((converter instanceof ColorConverter) &&
					(clonedConverter instanceof ColorConverter))
				{
					((ColorConverter) clonedConverter).setColor(
						((ColorConverter) converter).getColor());
					((ColorConverter) clonedConverter).setMin(((ColorConverter) converter)
						.getMin());
					((ColorConverter) clonedConverter).setMax(((ColorConverter) converter)
						.getMax());
				}

				return (Converter<I, O>) clonedConverter;
			}
			else {
				logger.error("Could not clone the converter of class " + converter
					.getClass().getSimpleName());
				return null;
			}
		}
	}

	public static ConverterSetup createConverterSetup(SourceAndConverter<?> sac) {

		if (sac.getConverter() instanceof ColorConverter) {
			return BigDataViewer.createConverterSetup(sac, -1);
		}
		else if (sac.getConverter() instanceof RealLUTConverter) {
			if (sac.asVolatile() != null) {
				return new LUTConverterSetup((RealLUTConverter) sac.getConverter(),
					(RealLUTConverter) sac.asVolatile().getConverter());
			}
			else {
				return new LUTConverterSetup((RealLUTConverter) sac.getConverter());
			}
		}
		else {
			logger.debug("Unmodifiable ConverterSetup for Converters of class " + sac
				.getConverter().getClass());
			if (sac.asVolatile() != null) {
				return new UnmodifiableConverterSetup(sac.getConverter(), sac
					.asVolatile().getConverter());
			}
			else {
				return new UnmodifiableConverterSetup(sac.getConverter());
			}
		}
	}

	/**
	 * Here should go all the ways to build a Volatile Source from a non-volatile
	 * Source, RealTyped
	 * 
	 * @param source source
	 * @return the volatile source
	 */
	private static <T, V extends Volatile<T>> Source<V> createVolatileRealType(
		Source<T> source) throws UnsupportedOperationException
	{
		// TODO unsupported yet
		throw new UnsupportedOperationException(
			"Unimplemented createVolatileRealType method in SourceAndConverterHelper");
	}

	/**
	 * Here should go all the ways to build a Volatile Source from a non-volatile
	 * Source, ARGBTyped
	 * 
	 * @param source the source
	 * @return the volatile source created
	 */
	private static Source<VolatileARGBType> createVolatileARGBType(
		Source<ARGBType> source) throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException(
			"Unimplemented createVolatileARGBType method in SourceAndConverterHelper");
	}

	/**
	 * Creates ARGB converter from a RealTyped SourceAndConverter. Supports
	 * Volatile RealTyped or non-volatile
	 * 
	 * @param <T> RealType class
	 * @param type a pixel of type T
	 * @return a suited converter
	 */
	public static <T extends RealType<T>> Converter<T, ARGBType>
		createConverterRealType(final T type)
	{
		final double typeMin = Math.max(0, Math.min(type.getMinValue(), 65535));
		final double typeMax = Math.max(0, Math.min(type.getMaxValue(), 65535));
		final RealARGBColorConverter<T> converter;
		converter = RealARGBColorConverter.create(type, typeMin, typeMax);
		converter.setColor(new ARGBType(0xffffffff));
		return converter;
	}

	/**
	 * Creates ARGB converter from a RealTyped SourceAndConverter. Supports
	 * Volatile ARGBType or non-volatile
	 * 
	 * @param source source
	 * @return a compatible converter
	 */
	public static Converter<?, ARGBType> createConverterARGBType(
		Source<?> source)
	{
		final Converter converter;
		if (source.getType() instanceof Volatile) converter =
			new ScaledARGBConverter.VolatileARGB(0, 255);
		else converter = new ScaledARGBConverter.ARGB(0, 255);
		return converter;
	}

	/**
	 * Checks whether a given point (calibrated global coordinate) lies inside the
	 * voxel grid of the Source's underlying RandomAccessibleInterval. This can be
	 * used as an alternative to below method: isSourcePresentAt
	 *
	 * @param source source
	 * @param globalPosition position in global coordinate system
	 * @param timepoint timepoint used
	 * @param sourceIs2d is the source is 2d, avoids checking third dimension
	 * @return boolean indicating whether the position falls within the source
	 *         interval
	 */
	public static boolean isPositionWithinSourceInterval(
		SourceAndConverter<?> source, RealPoint globalPosition, int timepoint,
		boolean sourceIs2d)
	{
		Source<?> spimSource = source.getSpimSource();

		final long[] voxelPositionInSource = getVoxelPositionInSource(spimSource,
			globalPosition, timepoint, 0);
		Interval sourceInterval = spimSource.getSource(timepoint, 0);

		if (sourceIs2d) {
			final long[] min = new long[2];
			final long[] max = new long[2];
			final long[] positionInSource2D = new long[2];
			for (int d = 0; d < 2; d++) {
				min[d] = sourceInterval.min(d);
				max[d] = sourceInterval.max(d);
				positionInSource2D[d] = voxelPositionInSource[d];
			}

			Interval interval2d = new FinalInterval(min, max);
			Point point2d = new Point(positionInSource2D);

			return Intervals.contains(interval2d, point2d);
		}
		else {
			Point point3d = new Point(voxelPositionInSource);

			return Intervals.contains(sourceInterval, point3d);
		}
	}

	/**
	 * Given a calibrated global position, this function uses the source transform
	 * to compute the position within the voxel grid of the source. Probably : do
	 * not work with warped sources
	 *
	 * @param source source
	 * @param globalPosition position in global coordinate
	 * @param t time point
	 * @param level mipmap level of the source
	 * @return voxel coordinate
	 */
	public static long[] getVoxelPositionInSource(final Source<?> source,
		final RealPoint globalPosition, final int t, final int level)
	{
		final int numDimensions = 3;

		final AffineTransform3D sourceTransform = BdvHandleHelper
			.getSourceTransform(source, t, level);

		final RealPoint voxelPositionInSource = new RealPoint(numDimensions);

		sourceTransform.inverse().apply(globalPosition, voxelPositionInSource);

		final long[] longPosition = new long[numDimensions];

		for (int d = 0; d < numDimensions; ++d)
			longPosition[d] = (long) voxelPositionInSource.getFloatPosition(d);

		return longPosition;
	}

	/**
	 * @param sacs sources
	 * @return the max timepoint found in this source according to the next method
	 *         ( check limitations )
	 */
	public static int getMaxTimepoint(SourceAndConverter<?>[] sacs) {
		int max = 0;
		for (SourceAndConverter<?> sac : sacs) {
			int sourceMax = getMaxTimepoint(sac);
			if (sourceMax > max) {
				max = sourceMax;
			}
		}
		return max;
	}

	/**
	 * @param sacs sources
	 * @return the max timepoint found in this source according to the next method
	 *         ( check limitations )
	 */
	public static int getMaxTimepoint(Source<?>[] sacs) {
		int max = 0;
		for (Source<?> source : sacs) {
			int sourceMax = getMaxTimepoint(source);
			if (sourceMax > max) {
				max = sourceMax;
			}
		}
		return max;
	}

	/**
	 * Looks for the max number of timepoint present in this source and converter
	 * To do this multiply the 2 the max timepoint until no source is present TODO
	 * : use the spimdata object if present to fetch this TODO : Limitation : if
	 * the timepoint 0 is not present, this fails! Limitation : if the source is
	 * present at all timepoint, this fails
	 *
	 * @param source source
	 * @return the maximal timepoint where the source is still present
	 */
	public static int getMaxTimepoint(Source<?> source) {
		if (!source.isPresent(0)) {
			return 0;
		}
		int nFrames = 1;
		int iFrame = 1;
		int previous = iFrame;
		while ((iFrame < Integer.MAX_VALUE / 2) && (source.isPresent(iFrame))) {
			previous = iFrame;
			iFrame *= 2;
		}
		if (iFrame > 1) {
			for (int tp = previous; tp < iFrame + 1; tp++) {
				if (!source.isPresent(tp)) {
					nFrames = tp;
					break;
				}
			}
		}
		return nFrames;
	}

	public static int getMaxTimepoint(SourceAndConverter<?> sac) {
		return getMaxTimepoint(sac.getSpimSource());
	}

	/**
	 * Is the point pt located inside the source at a particular timepoint ? Looks
	 * at highest resolution whether the alpha value of the displayed pixel is
	 * zero TODO TO think Alternative : looks whether R, G and B values equal zero
	 * - source not present Another option : if the display RGB value is zero,
	 * then consider it's not displayed and thus not selected - Convenient way to
	 * adjust whether a source should be selected or not ? TODO : Time out if too
	 * long to access the data
	 * 
	 * @param sac source
	 * @param pt point
	 * @param timePoint timepoint investigated
	 * @return true if the source is present
	 */
	public static <T> boolean isSourcePresentAt(SourceAndConverter<T> sac,
		int timePoint, RealPoint pt)
	{

		RealRandomAccessible<T> rra_ible = sac.getSpimSource()
			.getInterpolatedSource(timePoint, 0, Interpolation.NEARESTNEIGHBOR);

		if (rra_ible != null) {
			// Get transformation of the source
			final AffineTransform3D sourceTransform = new AffineTransform3D();
			sac.getSpimSource().getSourceTransform(timePoint, 0, sourceTransform);

			// Get access to the source at the pointer location
			RealRandomAccess<T> rra = rra_ible.realRandomAccess();
			RealPoint iPt = new RealPoint(3);
			sourceTransform.inverse().apply(pt, iPt);
			rra.setPosition(iPt);

			// Gets converter -> will decide based on ARGB value whether the source is
			// present or not
			Converter<T, ARGBType> cvt = sac.getConverter();
			ARGBType colorOut = new ARGBType();
			cvt.convert(rra.get(), colorOut);

			// Gets ARGB int value
			int cValue = colorOut.get();

			// Alpha == 0 -> not present, otherwise it is present
			return ARGBType.alpha(cValue) != 0;
		}
		else {
			return false;
		}

	}

	public static SourceAndConverter<?>[] sortDefault(
		SourceAndConverter<?>[] sacs)
	{
		return sortDefaultGeneric(Arrays.asList(sacs)).toArray(
			new SourceAndConverter<?>[0]);
	}

	/**
	 * Default sorting order for SourceAndConverter Because sometimes we want some
	 * consistency in channel ordering when exporting / importing TODO : find a
	 * better way to order between spimdata
	 * 
	 * @param sacs sources
	 * @return sorted sources according to the default sorter
	 */
	public static List<SourceAndConverter<?>> sortDefaultGeneric(
		Collection<SourceAndConverter<?>> sacs)
	{
		List<SourceAndConverter<?>> sortedList = new ArrayList<>(sacs.size());
		sortedList.addAll(sacs);

		Comparator<SourceAndConverter<?>> sacComparator = (s1, s2) -> {
			// Those who do not belong to spimdata are last:
			SourceAndConverterService.SpimDataInfo sdi1 = null, sdi2 = null;
			if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(
				s1, SourceAndConverterService.SPIM_DATA_INFO) != null)
			{
				sdi1 =
					((SourceAndConverterService.SpimDataInfo) (SourceAndConverterServices
						.getSourceAndConverterService().getMetadata(s1,
							SourceAndConverterService.SPIM_DATA_INFO)));
			}

			if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(
				s2, SourceAndConverterService.SPIM_DATA_INFO) != null)
			{
				sdi2 =
					((SourceAndConverterService.SpimDataInfo) (SourceAndConverterServices
						.getSourceAndConverterService().getMetadata(s2,
							SourceAndConverterService.SPIM_DATA_INFO)));
			}

			if ((sdi1 == null) && (sdi2 != null)) {
				return -1;
			}

			if ((sdi1 != null) && (sdi2 == null)) {
				return 1;
			}

			if (sdi1 != null) {
				if (sdi1.asd == sdi2.asd) {
					return sdi1.setupId - sdi2.setupId;
				}
				else {
					return sdi2.toString().compareTo(sdi1.toString());
				}
			}

			return s2.getSpimSource().getName().compareTo(s1.getSpimSource()
				.getName());
		};

		sortedList.sort(sacComparator);
		return sortedList;
	}

	/**
	 * Default sorting order for SourceAndConverter Because sometimes we want some
	 * consistency in channel ordering when exporting / importing TODO : find a
	 * better way to order between spimdata
	 * 
	 * @param sacs sources
	 * @return ordered sources
	 */
	@SuppressWarnings("rawtypes")
	@Deprecated
	public static List<SourceAndConverter> sortDefaultNoGeneric(
		Collection<SourceAndConverter> sacs)
	{
		List<SourceAndConverter> sortedList = new ArrayList<>(sacs.size());
		sortedList.addAll(sacs);

		Comparator<SourceAndConverter> sacComparator = (s1, s2) -> {
			// Those who do not belong to spimdata are last:
			SourceAndConverterService.SpimDataInfo sdi1 = null, sdi2 = null;
			if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(
				s1, SourceAndConverterService.SPIM_DATA_INFO) != null)
			{
				sdi1 =
					((SourceAndConverterService.SpimDataInfo) (SourceAndConverterServices
						.getSourceAndConverterService().getMetadata(s1,
							SourceAndConverterService.SPIM_DATA_INFO)));
			}

			if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(
				s2, SourceAndConverterService.SPIM_DATA_INFO) != null)
			{
				sdi2 =
					((SourceAndConverterService.SpimDataInfo) (SourceAndConverterServices
						.getSourceAndConverterService().getMetadata(s2,
							SourceAndConverterService.SPIM_DATA_INFO)));
			}

			if ((sdi1 == null) && (sdi2 != null)) {
				return -1;
			}

			if ((sdi1 != null) && (sdi2 == null)) {
				return 1;
			}

			if (sdi1 != null) {
				if (sdi1.asd == sdi2.asd) {
					return sdi1.setupId - sdi2.setupId;
				}
				else {
					return sdi2.toString().compareTo(sdi1.toString());
				}
			}

			return s2.getSpimSource().getName().compareTo(s1.getSpimSource()
				.getName());
		};

		sortedList.sort(sacComparator);
		return sortedList;
	}

	/**
	 * Return the center point in global coordinates of the source Do not expect
	 * this to work with WarpedSource
	 * 
	 * @param source source
	 * @return the center point of the source (assuming not warped)
	 */
	public static RealPoint getSourceAndConverterCenterPoint(
		SourceAndConverter<?> source)
	{
		AffineTransform3D sourceTransform = new AffineTransform3D();
		sourceTransform.identity();

		source.getSpimSource().getSourceTransform(0, 0, sourceTransform);
		long[] dims = new long[3];
		source.getSpimSource().getSource(0, 0).dimensions(dims);

		RealPoint ptCenterGlobal = new RealPoint(3);
		RealPoint ptCenterPixel = new RealPoint((dims[0] - 1.0) / 2.0, (dims[1] -
			1.0) / 2.0, (dims[2] - 1.0) / 2.0);

		sourceTransform.apply(ptCenterPixel, ptCenterGlobal);

		return ptCenterGlobal;
	}

	/**
	 * Applies the color converter settings from the src source to the dst sources
	 * color, min, max
	 * 
	 * @param src converter source
	 * @param dst converter dest
	 */
	public static void transferColorConverters(SourceAndConverter<?> src,
		SourceAndConverter<?> dst)
	{
		transferColorConverters(new SourceAndConverter<?>[] { src },
			new SourceAndConverter<?>[] { dst });
	}

	/**
	 * Applies the color converter settings from the sources srcs source to the
	 * sources dsts color, min, max. If the number of sources is unequal, the
	 * transfer is applied up to the common number of sources If null is
	 * encountered for src or dst, nothing happens silently The transfer is
	 * performed for the volatile source as well if it exists. The volatile source
	 * converter of src is ignored
	 *
	 * @param srcs sources source
	 * @param dsts sources dest
	 */
	public static void transferColorConverters(SourceAndConverter<?>[] srcs,
		SourceAndConverter<?>[] dsts)
	{
		if ((srcs != null) && (dsts != null)) for (int i = 0; i < Math.min(
			srcs.length, dsts.length); i++)
		{
			SourceAndConverter<?> src = srcs[i];
			SourceAndConverter<?> dst = dsts[i];
			if ((src != null) && (dst != null)) if ((dst
				.getConverter() instanceof ColorConverter) && (src
					.getConverter() instanceof ColorConverter))
			{
				ColorConverter conv_src = (ColorConverter) src.getConverter();
				ColorConverter conv_dst = (ColorConverter) dst.getConverter();
				conv_dst.setColor(conv_src.getColor());
				conv_dst.setMin(conv_src.getMin());
				conv_dst.setMax(conv_src.getMax());
				if (dst.asVolatile() != null) {
					conv_dst = (ColorConverter) dst.asVolatile().getConverter();
					conv_dst.setColor(conv_src.getColor());
					conv_dst.setMin(conv_src.getMin());
					conv_dst.setMax(conv_src.getMax());
				}
			}
		}
	}

	/**
	 * Returns the most appropriate level of a multiresolution source for sampling
	 * it at a certain voxel size. To match the resolution, the 'middle dimension'
	 * of voxels is used to make comparison with the target size voxsize So if the
	 * voxel size is [1.2, 0.8, 50], the value 1.2 is used to compare the levels
	 * to the target resolution. This is a way to avoid the complexity of defining
	 * the correct pixel size while being also robust to comparing 2d and 3d
	 * sources. Indeed, 2d sources may have aberrant defined vox size along the
	 * third axis, either way too big or way too small in one case or the other,
	 * the missing dimension is ignored, which we hope works in most
	 * circumstances. Other complication : the SourceAndConverter could be a
	 * warped source, or a warped source of a warped source of a transformed
	 * source, etc. The proper computation of the level required is complicated,
	 * and could be ill-defined: Warping can cause local shrinking or expansion
	 * such that a single level won't be the best choice for all the image. Here
	 * the assumption that we make is that the transforms should not change
	 * drastically the scale of the image (which could be clearly wrong). Assuming
	 * this, we get to the 'root' of the source and converter and get the voxel
	 * value from this root source. Look at the
	 * {@link SourceAndConverterHelper#getRootSource(Source, AffineTransform3D)}
	 * implementation to see how this search is done So : the source root should
	 * be properly scaled from the beginning and weird transformation like
	 * spherical transformed will give wrong results.
	 *
	 * @param src source
	 * @param t timepoint
	 * @param voxSize target voxel size
	 * @return mipmap level fitted for the voxel size
	 */
	public static int bestLevel(Source<?> src, int t, double voxSize) {
		List<Double> originVoxSize = new ArrayList<>();
		AffineTransform3D chainedSourceTransform = new AffineTransform3D();
		Source<?> rootOrigin = getRootSource(src, chainedSourceTransform);

		for (int l = 0; l < rootOrigin.getNumMipmapLevels(); l++) {
			AffineTransform3D sourceTransform = new AffineTransform3D();
			rootOrigin.getSourceTransform(t, l, sourceTransform);
			double mid = getCharacteristicVoxelSize(sourceTransform.concatenate(
				chainedSourceTransform));
			originVoxSize.add(mid);
		}

		if (voxSize < originVoxSize.get(0)) return 0; // below highest resolution :
																									// return the highest
																									// resolution

		if (originVoxSize.get(0) == 0) {
			System.err.println(SourceAndConverterHelper.class.getSimpleName() +
				" error : couldn't find voxel size of source " + src.getName());
			// proceed anyway
			return 0;
		}

		/*
		How to decide which resolution level is the best ? The perfect answer is to blend
		the resolution levels, but we can't do this here. My assumption: we want to take the
		resolution level which has its voxel size ratio voxSize/originVoxSize nearest to 1
		*/

		double bestRatio = voxSize / originVoxSize.get(0);
		int bestLevel = 0;

		boolean doBetter = true;
		int currentLevel = 0;

		while ((doBetter) && (currentLevel < originVoxSize.size() - 1)) {
			currentLevel++;
			double currentRatio;
			double currentVoxSize = originVoxSize.get(currentLevel);
			if (currentVoxSize > voxSize) {
				currentRatio = currentVoxSize / voxSize;
			}
			else {
				currentRatio = voxSize / currentVoxSize;
			}
			if (currentRatio < bestRatio) {
				bestRatio = currentRatio;
				bestLevel = currentLevel;
			}
			else doBetter = false;
		}

		return bestLevel;
	}

	/**
	 * See {@link SourceAndConverterHelper#bestLevel(Source, int, double)}
	 * 
	 * @param sac source
	 * @param t timepoint
	 * @param voxSize target voxel size
	 * @return mipmap level chosen
	 */
	public static int bestLevel(SourceAndConverter<?> sac, int t,
		double voxSize)
	{
		return bestLevel(sac.getSpimSource(), t, voxSize);
	}

	/**
	 * See {@link SourceAndConverterHelper#bestLevel(Source, int, double)} for an
	 * example of the use of this function What the 'root' means is actually the
	 * origin source from which is derived the source so if a source has been
	 * affine transformed, warped, resampled, potentially in successive steps this
	 * function should return the source it was derived from. This function is
	 * used (for the moment) only when a source needs to be resampled see
	 * {@link ResampledSource}, in order to get the origin voxel size of the
	 * source root. TODO : maybe use inspector to improve this root finding
	 * provide an AffineTransform which would mutated and concatenated such as the
	 * voxel size changes can be taken into account, provided that the transform
	 * are affine. Is the source is transformed in a more complex way, then
	 * nothing can be done easily...
	 *
	 * @param chainedSourceTransform TODO
	 * @param source source
	 * @return the root source : it's not derived from another source
	 */
	public static Source<?> getRootSource(Source<?> source,
		AffineTransform3D chainedSourceTransform)
	{
		Source<?> rootOrigin = source;
		while ((rootOrigin instanceof WarpedSource) ||
			(rootOrigin instanceof TransformedSource) ||
			(rootOrigin instanceof ResampledSource))
		{
			if (rootOrigin instanceof WarpedSource) {
				rootOrigin = ((WarpedSource<?>) rootOrigin).getWrappedSource();
			}
			else if (rootOrigin instanceof TransformedSource) {
				AffineTransform3D m = new AffineTransform3D();
				((TransformedSource<?>) rootOrigin).getFixedTransform(m);
				chainedSourceTransform.concatenate(m);
				rootOrigin = ((TransformedSource<?>) rootOrigin).getWrappedSource();
			}
			else if (rootOrigin instanceof ResampledSource) {
				rootOrigin = ((ResampledSource<?>) rootOrigin)
					.getModelResamplerSource();
			}
		}
		return rootOrigin;
	}

	/**
	 * see
	 * {@link SourceAndConverterHelper#getCharacteristicVoxelSize(AffineTransform3D)}
	 * 
	 * @param sac source
	 * @param t timepoint
	 * @param level mipmap level
	 * @return the characteristic voxel size for this level
	 */
	public static double getCharacteristicVoxelSize(SourceAndConverter<?> sac,
		int t, int level)
	{
		return getCharacteristicVoxelSize(sac.getSpimSource(), t, level);
	}

	/**
	 * See
	 * {@link SourceAndConverterHelper#getCharacteristicVoxelSize(AffineTransform3D)}
	 * 
	 * @param src source
	 * @param t timepoint
	 * @param level mipmap level
	 * @return the characteristic voxel size
	 */
	public static double getCharacteristicVoxelSize(Source<?> src, int t,
		int level)
	{
		AffineTransform3D chainedSourceTransform = new AffineTransform3D();
		Source<?> root = getRootSource(src, chainedSourceTransform);

		AffineTransform3D sourceTransform = new AffineTransform3D();
		root.getSourceTransform(t, level, sourceTransform);

		return getCharacteristicVoxelSize(sourceTransform.concatenate(
			chainedSourceTransform));
	}

	/**
	 * See {@link SourceAndConverterHelper#bestLevel(Source, int, double)} for a
	 * description of what the 'characteristic voxel size' means
	 * 
	 * @param sourceTransform affine transform of the source
	 * @return voxel size inferred from this transform
	 */
	public static double getCharacteristicVoxelSize(
		AffineTransform3D sourceTransform)
	{ // method also present in resampled source
		// Gets three vectors
		Point3d v1 = new Point3d(sourceTransform.get(0, 0), sourceTransform.get(0,
			1), sourceTransform.get(0, 2));
		Point3d v2 = new Point3d(sourceTransform.get(1, 0), sourceTransform.get(1,
			1), sourceTransform.get(1, 2));
		Point3d v3 = new Point3d(sourceTransform.get(2, 0), sourceTransform.get(2,
			1), sourceTransform.get(2, 2));

		// 0 - Ensure v1 and v2 have the same norm
		double a = Math.sqrt(v1.x * v1.x + v1.y * v1.y + v1.z * v1.z);
		double b = Math.sqrt(v2.x * v2.x + v2.y * v2.y + v2.z * v2.z);
		double c = Math.sqrt(v3.x * v3.x + v3.y * v3.y + v3.z * v3.z);

		return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c)); // https://stackoverflow.com/questions/1582356/fastest-way-of-finding-the-middle-value-of-a-triple
	}

	/**
	 * Determines all visible sources at the current mouse position in the Bdv
	 * window. Note: this method can be slow as it needs a random access on the
	 * source data.
	 * 
	 * @param bdvHandle the bdv window to probe
	 * @return List of SourceAndConverters
	 */
	public static List<SourceAndConverter<?>>
		getSourceAndConvertersAtCurrentMousePosition(BdvHandle bdvHandle)
	{
		// Gets mouse location in space (global 3D coordinates) and time
		final RealPoint mousePosInBdv = new RealPoint(3);
		bdvHandle.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates(
			mousePosInBdv);
		int timePoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		final List<SourceAndConverter<?>> sourceAndConverters =
			SourceAndConverterServices.getBdvDisplayService().getSourceAndConverterOf(
				bdvHandle).stream().filter(sac -> isSourcePresentAt(sac, timePoint,
					mousePosInBdv)).filter(sac -> SourceAndConverterServices
						.getBdvDisplayService().isVisible(sac, bdvHandle)).collect(
							Collectors.toList());

		return sourceAndConverters;
	}

	/**
	 * Return the list of double position along the ray which should lead to
	 * different pixel values this is complicated... how to handle warped sources
	 * ? procedural sources ?
	 * 
	 * @param sac source that's investigated
	 * @param origin of the ray
	 * @param direction of the ray
	 * @return a list of double position along the ray which should sample each
	 *         pixel
	 */
	public static List<Double> rayIntersect(SourceAndConverter<?> sac,
		int timepoint, RealPoint origin, RealPoint direction)
	{
		if (sac.getSpimSource() == null) {
			return new ArrayList<>();
		}
		else {
			return rayIntersect(sac.getSpimSource(), timepoint, origin, direction);
		}
	}

	public static List<Double> rayIntersect(Source<?> source, int timepoint,
		RealPoint origin, RealPoint direction)
	{
		if (source.isPresent(timepoint)) {
			if ((source instanceof AbstractSpimSource) ||
				(source instanceof TransformedSource) ||
				(source instanceof ResampledSource))
			{
				return rayIntersectRaiSource(source, timepoint, origin, direction);
			}
			else return new ArrayList<>();
		}
		else return new ArrayList<>();
	}

	public static List<Double> rayIntersectRaiSource(Source<?> source,
		int timepoint, RealPoint origin, RealPoint direction)
	{
		long[] dims = source.getSource(timepoint, 0).dimensionsAsLongArray();
		AffineTransform3D at3d = new AffineTransform3D();
		source.getSourceTransform(timepoint, 0, at3d);

		// Ok, now let's find the intersection of the ray with the box
		// Let's find the plane (XY, XZ, YZ) which is the best aligned along the
		// direction

		RealPoint ui = new RealPoint(3);
		ui.setPosition(at3d.get(0, 0), 0);
		ui.setPosition(at3d.get(1, 0), 1);
		ui.setPosition(at3d.get(2, 0), 2);

		RealPoint vi = new RealPoint(3);
		vi.setPosition(at3d.get(0, 1), 0);
		vi.setPosition(at3d.get(1, 1), 1);
		vi.setPosition(at3d.get(2, 1), 2);

		RealPoint wi = new RealPoint(3);
		wi.setPosition(at3d.get(0, 2), 0);
		wi.setPosition(at3d.get(1, 2), 1);
		wi.setPosition(at3d.get(2, 2), 2);

		RealPoint oppositeCorner = new RealPoint(dims[0] - 1, dims[1] - 1, dims[2] -
			1);

		at3d.apply(oppositeCorner, oppositeCorner);

		RealPoint plane0Origin = new RealPoint(0, 0, 0);
		at3d.apply(plane0Origin, plane0Origin); // pix to physical space coordinates
																						// - origin of first plane

		if (!rayIntersectPlane(origin, direction, plane0Origin, ui, vi, dims[0],
			dims[1])) if (!rayIntersectPlane(origin, direction, plane0Origin, ui, wi,
				dims[0], dims[2])) if (!rayIntersectPlane(origin, direction,
					plane0Origin, vi, wi, dims[1], dims[2])) if (!rayIntersectPlane(
						origin, direction, oppositeCorner, minus3(ui), minus3(vi), dims[0],
						dims[1])) if (!rayIntersectPlane(origin, direction, oppositeCorner,
							minus3(ui), minus3(wi), dims[0], dims[2])) if (!rayIntersectPlane(
								origin, direction, oppositeCorner, minus3(vi), minus3(wi),
								dims[1], dims[2])) return new ArrayList<>();

		RealPoint u = prodVect(vi, wi);
		RealPoint v = prodVect(ui, wi);
		RealPoint w = prodVect(ui, vi);

		normalize3(u);
		normalize3(v);
		normalize3(w);

		double absud = Math.abs(prodScal3(u, direction));
		double absvd = Math.abs(prodScal3(v, direction));
		double abswd = Math.abs(prodScal3(w, direction));

		RealPoint mainDirection;

		RealPoint planeMaxOrigin = new RealPoint(0, 0, 0);

		long nPlanes;

		if (absud > absvd) {
			// u > v
			if (absud > abswd) {
				// u > w
				nPlanes = dims[0];
				planeMaxOrigin.setPosition(nPlanes, 0);
				mainDirection = new RealPoint(u);
			}
			else {
				// w > u
				nPlanes = dims[2];
				planeMaxOrigin.setPosition(nPlanes, 2);
				mainDirection = new RealPoint(w);
			}
		}
		else {
			// v > u
			if (absvd > abswd) {
				// v > w
				nPlanes = dims[1];
				planeMaxOrigin.setPosition(nPlanes, 1);
				mainDirection = new RealPoint(v);
			}
			else {
				// w > v
				nPlanes = dims[2];
				planeMaxOrigin.setPosition(nPlanes, 2);
				mainDirection = new RealPoint(w);
			}
		}
		normalize3(mainDirection);
		at3d.apply(planeMaxOrigin, planeMaxOrigin); // pix to physical space
																								// coordinates - origin of last
																								// plane

		// We now need to find where
		// the plane perpendicular to mainDirection and with offset offs
		// intersects with the ray
		// we subtract the offset of the origin ray

		// The equation of the first plane is for any vector i
		// (i-offs0).maindirection = 0
		// The equation of the ray is i = lambda.direction + origin
		// We look for p0, the coordinate along the ray of the point which belongs
		// both to the plane and to the ray
		// (p0.direction+origin-offs0).maindirection = 0
		// thus p0.direction.maindirection = (offs0-origin).maindirection
		// p0 = (offs.maindirection - origin.maindirection ) /
		// (direction.maindirection)

		double pOrigin = prodScal3(origin, mainDirection);
		double p0 = (prodScal3(plane0Origin, mainDirection) - pOrigin) / prodScal3(
			direction, mainDirection);
		double pMax = (prodScal3(planeMaxOrigin, mainDirection) - pOrigin) /
			prodScal3(direction, mainDirection);

		if (nPlanes >= Integer.MAX_VALUE) {
			logger.debug("Too many planes");
			return new ArrayList<>();
		}
		else {
			List<Double> zPositions = new ArrayList<>((int) nPlanes);
			double step = (pMax - p0) / (double) nPlanes;
			for (int i = 0; i < nPlanes; i++) {
				zPositions.add(p0 + i * step);
			}
			return zPositions;
		}
	}

	static RealPoint minus3(RealPoint pt) {
		return new RealPoint(-pt.getDoublePosition(0), -pt.getDoublePosition(1), -pt
			.getDoublePosition(2));
	}

	public static boolean rayIntersectPlane(RealPoint rayOrigin,
		RealPoint rayDirection, RealPoint planeOrigin, RealPoint u, RealPoint v,
		double maxCoordU, double maxCoordV)
	{
		// Equation of the plane:
		// (uxv).(i-planeOrigin) = 0, where x is the cross product

		// Equation of the ray:
		// i = rayOrigin + lambda.rayDirection

		// First let's find lambda, which is such that:

		// (uxv).(rayOrigin + lambda.rayDirection - planeOrigin) = 0
		// implies
		// lambda = (uxv).(planeOrigin-rayOrigin) / (uxv.rayDirection)
		// Edge case : denominator null = never crossing ( parallel )

		RealPoint uxv = prodVect(u, v);

		double denominator = prodScal3(uxv, rayDirection);

		if (Math.abs(denominator) < 1e-12) {
			return false;
		}
		else {

			RealPoint planeOriginMinusRayOrigin = new RealPoint(planeOrigin
				.getDoublePosition(0) - rayOrigin.getDoublePosition(0), planeOrigin
					.getDoublePosition(1) - rayOrigin.getDoublePosition(1), planeOrigin
						.getDoublePosition(2) - rayOrigin.getDoublePosition(2));

			double lambda = prodScal3(uxv, planeOriginMinusRayOrigin) / denominator;

			RealPoint ptCrossingInPlane = new RealPoint(rayOrigin.getDoublePosition(
				0) + rayDirection.getDoublePosition(0) * lambda - planeOrigin
					.getDoublePosition(0), rayOrigin.getDoublePosition(1) + rayDirection
						.getDoublePosition(1) * lambda - planeOrigin.getDoublePosition(1),
				rayOrigin.getDoublePosition(2) + rayDirection.getDoublePosition(2) *
					lambda - planeOrigin.getDoublePosition(2));

			double coordU = prodScal3(u, ptCrossingInPlane) / norm2(u);
			double coordV = prodScal3(v, ptCrossingInPlane) / norm2(v);

			return ((coordU > -0.5) && (coordU < maxCoordU - 0.5)) &&
				((coordV > -0.5) && (coordV < maxCoordV - 0.5)); // Account for 'pixel
																													// thickness' of 1
		}
	}

	public static double norm2(RealPoint pt) {
		double px = pt.getDoublePosition(0);
		double py = pt.getDoublePosition(1);
		double pz = pt.getDoublePosition(2);
		return px * px + py * py + pz * pz;
	}

	public static void normalize3(RealPoint pt) {
		double px = pt.getDoublePosition(0);
		double py = pt.getDoublePosition(1);
		double pz = pt.getDoublePosition(2);
		double d = Math.sqrt(px * px + py * py + pz * pz);
		pt.setPosition(pt.getDoublePosition(0) / d, 0);
		pt.setPosition(pt.getDoublePosition(1) / d, 1);
		pt.setPosition(pt.getDoublePosition(2) / d, 2);
	}

	public static double prodScal3(RealPoint pt1, RealPoint pt2) {
		return pt1.getDoublePosition(0) * pt2.getDoublePosition(0) + pt1
			.getDoublePosition(1) * pt2.getDoublePosition(1) + pt1.getDoublePosition(
				2) * pt2.getDoublePosition(2);
	}

	public static RealPoint prodVect(RealPoint pt1, RealPoint pt2) {

		double px = pt1.getDoublePosition(1) * pt2.getDoublePosition(2) - pt1
			.getDoublePosition(2) * pt2.getDoublePosition(1);

		double py = pt1.getDoublePosition(2) * pt2.getDoublePosition(0) - pt1
			.getDoublePosition(0) * pt2.getDoublePosition(2);

		double pz = pt1.getDoublePosition(0) * pt2.getDoublePosition(1) - pt1
			.getDoublePosition(1) * pt2.getDoublePosition(0);

		return new RealPoint(px, py, pz);
	}
}
