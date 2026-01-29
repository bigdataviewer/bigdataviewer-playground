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
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SpimDataTransformViewer;

import javax.swing.SwingUtilities;

/**
 * Command to open the SpimData Transform Viewer.
 *
 * This viewer displays the transform chain for SpimData sources in a
 * configurable table format. The 3D data (sources x timepoints x transforms)
 * can be viewed with any dimension as rows, columns, or slider.
 *
 * Sources without an associated SpimData object are excluded with a warning.
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
@SuppressWarnings({ "CanBeFinal", "unused" })
@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu +
		"Sources>Transform>View SpimData Transforms",
	description = "Opens a viewer to explore SpimData transforms with " +
		"configurable dimensions (sources, timepoints, transform chain)")
public class SpimDataTransformViewerCommand implements BdvPlaygroundActionCommand
{

	protected static final Logger logger = LoggerFactory.getLogger(
		SpimDataTransformViewerCommand.class);

	@Parameter(label = "Select source(s)",
		description = "Select sources to view their SpimData transforms. " +
			"Sources without SpimData will be excluded.")
	SourceAndConverter<?>[] sacs;

	@Parameter
	SourceAndConverterService sacService;
	@Override
	public void run() {
		if (sacs == null || sacs.length == 0) {
			logger.error("No sources selected!");
			return;
		}

		logger.info("Opening SpimData Transform Viewer for {} source(s)",
			sacs.length);

		SwingUtilities.invokeLater(() -> {
			SpimDataTransformViewer viewer = new SpimDataTransformViewer(sacs,
				sacService);
			viewer.showViewer();
		});
	}
}
