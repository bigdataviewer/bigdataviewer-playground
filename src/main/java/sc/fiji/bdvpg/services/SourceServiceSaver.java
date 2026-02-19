/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.services;

import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.services.SourceService;
import sc.fiji.bdvpg.scijava.services.tree.inspect.SourceInspector;
import sc.fiji.bdvpg.source.SourceHelper;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.services.ISourceService.SPIM_DATA_LOCATION;

/**
 * Big Objective : save the state of all open sources By Using Gson and specific
 * serialization depending on SourceAndConverter classes TODO : take care of
 * sources not built with SourceAndConverter TODO : BUG do not work if the same
 * spimdata is opened several times!
 */

public class SourceServiceSaver extends SourceAdapter
	implements Runnable
{

	protected static final Logger logger = LoggerFactory.getLogger(
		SourceServiceSaver.class);

	final File f;

	List<SourceAndConverter<?>> sources;

	public SourceServiceSaver(File f, Context ctx) {
		this(f, ctx, SourceServices.getSourceService()
			.getSourceAndConverters());
	}

	public SourceServiceSaver(File f, Context ctx,
							  List<SourceAndConverter<?>> sources)
	{
		super(ctx, f.getParentFile(), false);
		this.sources = sources;
		this.f = f;
		idToSac = new HashMap<>();
		sacToId = new HashMap<>();
		sourceToId = new HashMap<>();
		idToSource = new HashMap<>();
	}

	public SourceServiceSaver(File f, Context ctx,
							  List<SourceAndConverter<?>> sources, boolean useRelativePaths)
	{
		super(ctx, f.getParentFile(), useRelativePaths);
		this.sources = sources;
		this.f = f;
		idToSac = new HashMap<>();
		sacToId = new HashMap<>();
		sourceToId = new HashMap<>();
		idToSource = new HashMap<>();
	}

	final Set<SourceAndConverter<?>> setOfSourcesNeedingSerialization =
		new HashSet<>();

	@Override
	public void run() {
		synchronized (SourceServiceSaver.class) {

			// Makes sure each source is associated to at least one sourceAndConverter
			// this happens via recursive source inspection

			sources.forEach(source -> setOfSourcesNeedingSerialization.addAll(
				SourceInspector.appendInspectorResult(
					new DefaultMutableTreeNode(), source, SourceServices
						.getSourceService(), true)));

			// Then let's get back all the sources - they may have increase in number
			sources = new ArrayList<>(setOfSourcesNeedingSerialization);

			sources = SourceHelper.sortDefaultGeneric(sources);

			for (int i = 0; i < sources.size(); i++) {
				idToSac.put(i, sources.get(i));
				sacToId.put(sources.get(i), i);
				idToSource.put(i, sources.get(i).getSpimSource());
				sourceToId.put(sources.get(i).getSpimSource(), i);
			}

			Gson gson = getGson();

			// Let's launch serialization of all SpimDatasets first
			// This forces a saving of all datasets before they can be required by
			// other sourceAdnConverters
			// Serializes datasets - required to avoid serialization issues
			Set<AbstractSpimData<?>> asds = SourceServices
				.getSourceService().getSpimDatasets();

			// Avoid unnecessary serialization of unneeded spimdata
			asds = asds.stream().filter(asd -> {
				List<SourceAndConverter<?>> sources_in_asd = SourceServices
					.getSourceService().getSourceAndConverterFromSpimdata(
						asd);
				return sources_in_asd.stream().anyMatch(source -> sources.contains(source));
			}).collect(Collectors.toSet());

			Map<AbstractSpimData<?>, String> originalLocations = new HashMap<>();
			if (useRelativePaths) {
				// We need to reset where they were saved, and then maybe restore their location
				asds.forEach(asd -> {
					String dataLocation = (String) getScijavaContext()
							.getService(SourceService.class).getMetadata(asd, SPIM_DATA_LOCATION);

					originalLocations.put(asd, dataLocation);

					getScijavaContext()
							.getService(SourceService.class).setMetadata(asd, SPIM_DATA_LOCATION, ""); // reset -> enforce serialization to XML
				});

			}

			asds.forEach(gson::toJson);

			try {
				logger.info("Writing state file " + f.getAbsolutePath());
				FileWriter writer = new FileWriter(f.getAbsolutePath());
				gson.toJson(sources, writer);
				writer.flush();
				writer.close();

				if (useRelativePaths) {
					// We need to reset where they were saved, and then maybe restore their location
					asds.forEach(asd -> {
						//String dataLocation = (String) getScijavaContext()
						//		.getService(SourceAndConverterService.class).getMetadata(asd, SPIM_DATA_LOCATION);
						//originalLocations.put(asd, dataLocation);

						getScijavaContext()
								.getService(SourceService.class).setMetadata(asd, SPIM_DATA_LOCATION, originalLocations.get(asd)); // reset -> enforce serialization to XML
					});

				}
			}
			catch (Exception e) {
				logger.error("Couldn't write state file: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

}
