/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu +
		"BDV>BDV - Show Sources (new Bdv window)",
	description = "Displays one or several sources into a new BDV window")
public class BdvSourcesShowCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select Source(s)")
	SourceAndConverter<?>[] sacs;

	@Parameter(label = "Auto Contrast")
	boolean autocontrast;

	@Parameter(label = "Adjust View on Source")
	boolean adjustviewonsource;

	@Parameter(label = "Interpolate")
	public boolean interpolate = false;

	/**
	 * This triggers: BdvHandlePostprocessor
	 */
	@Parameter(type = ItemIO.OUTPUT)
	public BdvHandle bdvh;

	@Parameter
	SourceAndConverterBdvDisplayService bdvDisplayService;

	@Override
	public void run() {
		bdvh = bdvDisplayService.getNewBdv();

		bdvDisplayService.show(bdvh, sacs);
		if (autocontrast) {
			for (SourceAndConverter<?> sac : sacs) {
				int timepoint = bdvh.getViewerPanel().state().getCurrentTimepoint();
				new BrightnessAutoAdjuster<>(sac, timepoint).run();
			}
		}

		if ((adjustviewonsource) && (sacs.length > 0)) {
			new ViewerTransformAdjuster(bdvh, sacs).run();
		}
	}
}
