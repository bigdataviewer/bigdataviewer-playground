/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2024 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterInspector;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.services.ISourceAndConverterService.SPIM_DATA_LOCATION;

/**
 * Big Objective : save the state of all open sources By Using Gson and specific
 * serialization depending on SourceAndConverter classes TODO : take care of
 * sources not built with SourceAndConverter TODO : BUG do not work if the same
 * spimdata is opened several times!
 */

public class SourceAndConverterServiceSaver extends SourceAndConverterAdapter
	implements Runnable
{

	protected static final Logger logger = LoggerFactory.getLogger(
		SourceAndConverterServiceSaver.class);

	final File f;

	List<SourceAndConverter<?>> sacs;

	public SourceAndConverterServiceSaver(File f, Context ctx) {
		this(f, ctx, SourceAndConverterServices.getSourceAndConverterService()
			.getSourceAndConverters());
	}

	public SourceAndConverterServiceSaver(File f, Context ctx,
		List<SourceAndConverter<?>> sacs)
	{
		super(ctx, f.getParentFile(), false);
		this.sacs = sacs;
		this.f = f;
		idToSac = new HashMap<>();
		sacToId = new HashMap<>();
		sourceToId = new HashMap<>();
		idToSource = new HashMap<>();
	}

	public SourceAndConverterServiceSaver(File f, Context ctx,
										  List<SourceAndConverter<?>> sacs, boolean useRelativePaths)
	{
		super(ctx, f.getParentFile(), useRelativePaths);
		this.sacs = sacs;
		this.f = f;
		idToSac = new HashMap<>();
		sacToId = new HashMap<>();
		sourceToId = new HashMap<>();
		idToSource = new HashMap<>();
	}

	final List<SourceAndConverter<?>> setOfSourcesNeedingSerialization =
		new ArrayList<>();

	@Override
	public void run() {
		synchronized (SourceAndConverterServiceSaver.class) {

			// Makes sure each source is associated to at least one sourceAndConverter
			// this happens via recursive source inspection

			sacs.forEach(sac -> setOfSourcesNeedingSerialization.addAll(
				SourceAndConverterInspector.appendInspectorResult(
					new DefaultMutableTreeNode(), sac, SourceAndConverterServices
						.getSourceAndConverterService(), true)));

			// Then let's get back all the sacs - they may have increase in number
			sacs = new ArrayList<>(setOfSourcesNeedingSerialization);

			sacs = SourceAndConverterHelper.sortDefaultGeneric(sacs);

			for (int i = 0; i < sacs.size(); i++) {
				idToSac.put(i, sacs.get(i));
				sacToId.put(sacs.get(i), i);
				idToSource.put(i, sacs.get(i).getSpimSource());
				sourceToId.put(sacs.get(i).getSpimSource(), i);
			}

			Gson gson = getGson();

			// Let's launch serialization of all SpimDatasets first
			// This forces a saving of all datasets before they can be required by
			// other sourceAdnConverters
			// Serializes datasets - required to avoid serialization issues
			Set<AbstractSpimData<?>> asds = SourceAndConverterServices
				.getSourceAndConverterService().getSpimDatasets();

			// Avoid unnecessary serialization of unneeded spimdata
			asds = asds.stream().filter(asd -> {
				List<SourceAndConverter<?>> sacs_in_asd = SourceAndConverterServices
					.getSourceAndConverterService().getSourceAndConverterFromSpimdata(
						asd);
				return sacs_in_asd.stream().anyMatch(sac -> sacs.contains(sac));
			}).collect(Collectors.toSet());

			Map<AbstractSpimData<?>, String> originalLocations = new HashMap<>();
			if (useRelativePaths) {
				// We need to reset where they were saved, and then maybe restore their location
				asds.forEach(asd -> {
					String dataLocation = (String) getScijavaContext()
							.getService(SourceAndConverterService.class).getMetadata(asd, SPIM_DATA_LOCATION);

					originalLocations.put(asd, dataLocation);

					getScijavaContext()
							.getService(SourceAndConverterService.class).setMetadata(asd, SPIM_DATA_LOCATION, ""); // reset -> enforce serialization to xml
				});

			}

			asds.forEach(gson::toJson);

			try {
				logger.info("Writing state file " + f.getAbsolutePath());
				FileWriter writer = new FileWriter(f.getAbsolutePath());
				gson.toJson(sacs, writer);
				writer.flush();
				writer.close();

				if (useRelativePaths) {
					// We need to reset where they were saved, and then maybe restore their location
					asds.forEach(asd -> {
						//String dataLocation = (String) getScijavaContext()
						//		.getService(SourceAndConverterService.class).getMetadata(asd, SPIM_DATA_LOCATION);
						//originalLocations.put(asd, dataLocation);

						getScijavaContext()
								.getService(SourceAndConverterService.class).setMetadata(asd, SPIM_DATA_LOCATION, originalLocations.get(asd)); // reset -> enforce serialization to xml
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
