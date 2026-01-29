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

package sc.fiji.bdvpg.scijava.command.spimdata;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransformAffine;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.Set;

/**
 * Command to add (prepend or append) a transform to SpimData ViewRegistrations.
 *
 * - Prepend: adds the transform at the beginning of the chain (applied last, most recent)
 * - Append: adds the transform at the end of the chain (applied first, oldest)
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
@SuppressWarnings({ "CanBeFinal", "unused" })
@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu +
		"Sources>Transform>Add SpimData Transform",
	description = "Adds a transform to the chain at specified timepoints (prepend or append)")
public class AddSpimDataTransformCommand implements BdvPlaygroundActionCommand {

	protected static final Logger logger = LoggerFactory.getLogger(
		AddSpimDataTransformCommand.class);

	@Parameter(label = "Select source(s)",
		description = "Select sources whose SpimData transforms will be modified")
	SourceAndConverter<?>[] sacs;

	@Parameter(label = "Timepoint range",
		description = "Timepoints to modify. Examples: '0', '0:10', '0,5,10', '0:5,10:15'",
		persist = false)
	String timepoint_range = "0";

	@Parameter(label = "Position",
		description = "Where to add the transform in the chain",
		choices = { "Prepend (most recent, applied last)",
			"Append (oldest, applied first)" },
		persist = false)
	String position = "Prepend (most recent, applied last)";

	@Parameter(label = "Transform matrix (3 rows, 4 values each)",
		description = "Affine transform matrix: 3 rows of 4 comma-separated values",
		style = TextWidget.AREA_STYLE,
		persist = false)
	String transform_matrix = SpimDataTransformUtils.IDENTITY_MATRIX;

	@Parameter(label = "Transform name",
		description = "Name for the new transform",
		persist = false)
	String transform_name = "Manual transform";

	@Parameter
	SourceAndConverterBdvDisplayService displayService;

	@Override
	public void run() {
		if (sacs == null || sacs.length == 0) {
			logger.error("No sources selected!");
			return;
		}

		// Parse the transform matrix
		AffineTransform3D transform;
		try {
			transform = SpimDataTransformUtils.parseTransformMatrix(transform_matrix);
		}
		catch (IllegalArgumentException e) {
			logger.error("Invalid transform matrix: {}", e.getMessage());
			return;
		}

		// Parse timepoint range
		Set<Integer> timepoints;
		try {
			timepoints = SpimDataTransformUtils.parseRange(timepoint_range);
		}
		catch (IllegalArgumentException e) {
			logger.error("Invalid range specification: {}", e.getMessage());
			return;
		}

		boolean prepend = position.startsWith("Prepend");

		logger.info("Adding transform ({}) for {} source(s), {} timepoint(s)",
			prepend ? "prepend" : "append", sacs.length, timepoints.size());

		int addedCount = 0;

		for (SourceAndConverter<?> sac : sacs) {
			// Get SpimData info
			Object info = SourceAndConverterServices.getSourceAndConverterService()
				.getMetadata(sac, SourceAndConverterService.SPIM_DATA_INFO);

			if (info == null) {
				logger.warn("Source '{}' has no associated SpimData, skipping",
					sac.getSpimSource().getName());
				continue;
			}

			SourceAndConverterService.SpimDataInfo spimDataInfo =
				(SourceAndConverterService.SpimDataInfo) info;
			AbstractSpimData<?> spimData = spimDataInfo.asd;
			int setupId = spimDataInfo.setupId;

			ViewRegistrations vrs = spimData.getViewRegistrations();

			for (int tp : timepoints) {
				ViewRegistration vr = vrs.getViewRegistration(tp, setupId);
				if (vr == null) {
					logger.warn("No ViewRegistration for timepoint {} setup {}, skipping",
						tp, setupId);
					continue;
				}

				// Create the new transform
				ViewTransformAffine newTransform = new ViewTransformAffine(
                        transform_name, transform.copy());

				if (prepend) {
					// Prepend: add at index 0 (beginning of list = most recent)
					vr.getTransformList().add(0, newTransform);
				}
				else {
					// Append: add at end of list (oldest)
					vr.getTransformList().add(newTransform);
				}

				addedCount++;

				// Update the concatenated transform
				vr.updateModel();

				// Force the BDV source to reload
				SpimDataTransformUtils.reloadSourceTransform(sac, tp);
			}
		}

		displayService.updateDisplays(sacs);
		logger.info("Added {} transform(s)", addedCount);
	}
}
