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

package sc.fiji.bdvpg.command.viewer.bvv;

import bdv.viewer.SourceAndConverter;
import bvv.vistools.BvvHandle;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.viewers.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceServices;
import sc.fiji.bdvpg.viewers.ViewerAdapter;

/**
 * Show sources in a BigVolumeViewer window - limited to 16 bit images
 */

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu + "Viewers>BVV>BVV - Show Sources",
	description = "Show sources in a BigVolumeViewer window")
public class BvvSourcesAddCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select BVV Window",
			description = "The BigVolumeViewer window where sources will be displayed")
	BvvHandle bvvh;

	@Parameter(label = "Adjust View on Source",
			description = "Centers and zooms the view to fit the added sources")
	boolean adjust_view;

	@Parameter(label = "Select source(s)",
			description = "The source(s) to add")
	SourceAndConverter<?>[] sources;

	@Override
	public void run() {

		for (SourceAndConverter<?> source : sources) {
            bvvh.getConverterSetups().put(source, SourceServices
                .getSourceAndConverterService().getConverterSetup(source));
            bvvh.getViewerPanel().state().addSource(source);

            bvvh.getViewerPanel().state().setSourceActive(source, true);
		}

		if ((adjust_view) && (sources.length > 0)) {
			new ViewerTransformAdjuster(new ViewerAdapter(bvvh), sources).run();
		}

	}
}
