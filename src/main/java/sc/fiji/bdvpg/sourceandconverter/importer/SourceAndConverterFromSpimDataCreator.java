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

package sc.fiji.bdvpg.sourceandconverter.importer;

import bdv.SpimSource;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.VolatileSpimSource;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.HashMap;
import java.util.Map;

public class SourceAndConverterFromSpimDataCreator {

	protected static final Logger logger = LoggerFactory.getLogger(
		SourceAndConverterFromSpimDataCreator.class);

	private final AbstractSpimData<?> asd;
	private final Map<Integer, SourceAndConverter<?>> setupIdToSourceAndConverter;
	private final Map<SourceAndConverter<?>, Map<String, Object>> sourceAndConverterToMetadata;

	public SourceAndConverterFromSpimDataCreator(AbstractSpimData<?> asd) {
		this.asd = asd;
		setupIdToSourceAndConverter = new HashMap<>();
		sourceAndConverterToMetadata = new HashMap<>();
		createSourceAndConverters();
	}

	public Map<Integer, SourceAndConverter<?>> getSetupIdToSourceAndConverter() {
		return setupIdToSourceAndConverter;
	}

	public Map<String, Object> getMetadata(
		SourceAndConverter<?> sourceAndConverter)
	{
		return sourceAndConverterToMetadata.get(sourceAndConverter);
	}

	private void createSourceAndConverters() {
		boolean nonVolatile = WrapBasicImgLoader.wrapImgLoaderIfNecessary(asd);

		if (nonVolatile) {
			System.err.println(
				"WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance.");
		}

		final AbstractSequenceDescription<?, ?, ?> seq = asd
			.getSequenceDescription();

		final ViewerImgLoader imgLoader = (ViewerImgLoader) seq.getImgLoader();

		for (final BasicViewSetup setup : seq.getViewSetupsOrdered()) {

			final int setupId = setup.getId();

			ViewerSetupImgLoader<?, ?> vsil = imgLoader.getSetupImgLoader(setupId);

			String sourceName = createSetupName(setup);

			final Object type = vsil.getImageType();

			if (type instanceof RealType) {

				createRealTypeSourceAndConverter(nonVolatile, setupId, sourceName);

			}
			else if (type instanceof ARGBType) {

				createARGBTypeSourceAndConverter(setupId, sourceName);

			}
			else {
				logger.error("Cannot open Spimdata with Source of type " + type
					.getClass().getSimpleName());
			}

			sourceAndConverterToMetadata.put(setupIdToSourceAndConverter.get(setupId),
				new HashMap<>());

		}

		WrapBasicImgLoader.removeWrapperIfPresent(asd);
	}

	private void createRealTypeSourceAndConverter(boolean nonVolatile,
		int setupId, String sourceName)
	{
		final SpimSource<?> s = new SpimSource<>(asd, setupId, sourceName);

		Converter<?, ARGBType> nonVolatileConverter = SourceAndConverterHelper
			.createConverterRealType((RealType) (s.getType())); // IN FACT CASTING IS NECESSARY!!

		if (!nonVolatile) {

			final VolatileSpimSource<?> vs = new VolatileSpimSource<>(asd, setupId,
				sourceName);

			Converter<?, ARGBType> volatileConverter = SourceAndConverterHelper
				.createConverterRealType((RealType) (vs.getType()));

			setupIdToSourceAndConverter.put(setupId, new SourceAndConverter(s,
				nonVolatileConverter, new SourceAndConverter(vs, volatileConverter)));

		}
		else {

			setupIdToSourceAndConverter.put(setupId, new SourceAndConverter(s,
				nonVolatileConverter));
		}
	}

	private void createARGBTypeSourceAndConverter(int setupId,
		String sourceName)
	{
		final VolatileSpimSource vs = new VolatileSpimSource<>(asd, setupId,
			sourceName);
		final SpimSource s = new SpimSource<>(asd, setupId, sourceName);

		Converter nonVolatileConverter = SourceAndConverterHelper
			.createConverterARGBType(s);

		Converter volatileConverter = SourceAndConverterHelper
			.createConverterARGBType(vs);
		setupIdToSourceAndConverter.put(setupId, new SourceAndConverter(s,
			nonVolatileConverter, new SourceAndConverter<>(vs, volatileConverter)));

	}

	private static String createSetupName(final BasicViewSetup setup) {
		if (setup.hasName()) {
			if (!setup.getName().trim().equals("")) {
				return setup.getName();
			}
		}

		String name = "";

		final Angle angle = setup.getAttribute(Angle.class);
		if (angle != null) name += "a " + angle.getName();

		final Channel channel = setup.getAttribute(Channel.class);
		if (channel != null) name += (name.isEmpty() ? "" : " ") + "c " + channel
			.getName();

		if ((channel == null) && (angle == null)) {
			name += "id " + setup.getId();
		}

		return name;
	}

}
