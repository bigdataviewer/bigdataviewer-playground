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

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.IBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

import java.util.Arrays;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu + "BDV>BDV - Set BDV window (default)",
	description = "Sets the default preferences for newly created BDV windows")
public class BdvDefaultViewerSetterCommand implements
	BdvPlaygroundActionCommand
{

	@Parameter(
		label = "Reset to default",
		description = "If checked, ignores all other parameters and resets to default settings",
		persist = false)
	boolean resetToDefault = false;

	@Parameter(label = "Window width",
			description = "Default width in pixels for new BDV windows")
	int width = 640;

	@Parameter(label = "Window height",
			description = "Default height in pixels for new BDV windows")
	int height = 480;

	@Parameter(label = "Screen scales",
			description = "Comma-separated list of scale factors for multi-resolution rendering (e.g., 1, 0.5, 0.25)")
	String screenscales = "1, 0.75, 0.5, 0.25, 0.125";

	@Parameter(label = "Target render time (ms)",
			description = "Target time in milliseconds for rendering a single frame")
	long targetrenderms = 30;// * 1000000l;

	@Parameter(label = "Number of rendering threads",
			description = "Number of threads used for rendering")
	int numrenderingthreads = 3;

	@Parameter(label = "Number of source groups",
			description = "Number of source groups available in the BDV window")
	int numsourcegroups = 10;

	@Parameter(label = "Window title",
			description = "Default title for new BDV windows")
	String frametitle = "BigDataViewer";

	@Parameter(label = "2D mode",
			description = "If enabled, restricts navigation to 2D (only Z-rotations)")
	boolean is2d = false;

	@Parameter(label = "Interpolate",
			description = "Enables interpolation for smoother rendering")
	boolean interpolate = false;

	@Parameter(label = "Number of timepoints",
			description = "Default number of timepoints for new BDV windows")
	int numtimepoints = 1;

	@Parameter
	SourceAndConverterBdvDisplayService sacDisplayService;

	@Override
	public void run() {
		if (resetToDefault) {
			IBdvSupplier bdvSupplier = new DefaultBdvSupplier(
				new SerializableBdvOptions());
			sacDisplayService.setDefaultBdvSupplier(bdvSupplier);
		}
		else {
			SerializableBdvOptions options = new SerializableBdvOptions();
			options.frameTitle = frametitle;
			options.is2D = is2d;
			options.numRenderingThreads = numrenderingthreads;
			options.screenScales = Arrays.stream(screenscales.split(",")).mapToDouble(
				Double::parseDouble).toArray();
			options.height = height;
			options.width = width;
			options.numSourceGroups = numsourcegroups;
			options.numTimePoints = numtimepoints;
			options.interpolate = interpolate;
			IBdvSupplier bdvSupplier = new DefaultBdvSupplier(options);
			sacDisplayService.setDefaultBdvSupplier(bdvSupplier);
		}

	}
}
