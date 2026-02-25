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

package sc.fiji.bdvpg.command.view.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.viewers.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.BdvPgMenus;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceBdvDisplayService;
import sc.fiji.bdvpg.source.display.BrightnessAutoAdjuster;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menu = {
			@Menu(label = BdvPgMenus.L1),
			@Menu(label = BdvPgMenus.L2),
			@Menu(label = BdvPgMenus.ViewMenu, weight = BdvPgMenus.ViewW),
			@Menu(label = BdvPgMenus.BDVMenu, weight = BdvPgMenus.BDVW),
			@Menu(label = "BDV - Show Sources", weight = 3)
	},
	description = "Displays one or several sources into a new BDV window")
public class BdvSourcesShowCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select Source(s)",
			description = "The source(s) to display in the new BDV window")
	SourceAndConverter<?>[] sources;

	@Parameter(label = "Auto Contrast",
			description = "Automatically adjusts brightness and contrast based on the current timepoint")
	boolean auto_contrast;

	@Parameter(label = "Adjust View on Sources",
			description = "Centers and zooms the view to fit the displayed sources")
	boolean adjust_view;

	@Parameter(label = "Open In New Window",
			description = "Force creation of a new window")
	boolean make_new_window;

	@Parameter(label = "Interpolate",
			description = "Enables interpolation for smoother rendering")
	public boolean interpolate = false;

	/**
	 * This triggers: BdvHandlePostprocessor
	 */
	@Parameter(type = ItemIO.OUTPUT,
			label = "Created BDV Window",
			description = "The newly created BigDataViewer window containing the sources")
	public BdvHandle bdvh;

	@Parameter
    SourceBdvDisplayService bdvDisplayService;

	@Override
	public void run() {
		if (make_new_window) {
			bdvh = bdvDisplayService.getNewBdv();
		} else {
			bdvh = bdvDisplayService.getActiveBdv();
		}

		bdvDisplayService.show(bdvh, sources);
		if (auto_contrast) {
			for (SourceAndConverter<?> source : sources) {
				int timepoint = bdvh.getViewerPanel().state().getCurrentTimepoint();
				new BrightnessAutoAdjuster<>(source, timepoint).run();
			}
		}

		if ((adjust_view) && (sources.length > 0)) {
			new ViewerTransformAdjuster(bdvh, sources).run();
		}
	}
}
