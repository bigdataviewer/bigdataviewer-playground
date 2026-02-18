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

package sc.fiji.bdvpg.command.source;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceService;
import sc.fiji.bdvpg.source.register.BigWarpLauncher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Nicolas Chiaruttini, EPFL 2020
 */

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu + "Sources>Apps>Source - Launch BigWarp",
	description = "Starts BigWarp from existing sources")

public class SourceLaunchBigWarpCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Window title for BigWarp",
			description = "Title for the BigWarp windows")
	String bigwarp_name;

	@Parameter(label = "Moving Source(s)",
			description = "The source(s) that will be warped to match the fixed sources")
	SourceAndConverter<?>[] moving_sources;

	@Parameter(label = "Fixed Source(s)",
			description = "The reference source(s) that stay fixed during registration")
	SourceAndConverter<?>[] fixed_sources;

	@Parameter(type = ItemIO.OUTPUT,
			label = "BigWarp Q Window",
			description = "The BigWarp Q (moving) window handle")
	BdvHandle bdvh_q;

	@Parameter(type = ItemIO.OUTPUT,
			label = "BigWarp P Window",
			description = "The BigWarp P (fixed) window handle")
	BdvHandle bdvh_p;

	@Parameter(type = ItemIO.OUTPUT,
			label = "Warped Sources",
			description = "The warped versions of the moving sources")
	SourceAndConverter<?>[] warped_sources;

	@Parameter(type = ItemIO.OUTPUT,
			label = "Grid Source",
			description = "A grid source showing the deformation field")
	SourceAndConverter<?> grid_source;

	@Parameter(type = ItemIO.OUTPUT,
			label = "Warp Magnitude Source",
			description = "A source showing the magnitude of the deformation")
	SourceAndConverter<?> warp_magnitude_source;

	@Parameter
	SourceBdvDisplayService bsds;

	@Parameter
	SourceService source_service;

	public void run() {
		List<SourceAndConverter<?>> movingSources = Arrays.stream(moving_sources)
			.collect(Collectors.toList());
		List<SourceAndConverter<?>> fixedSources = Arrays.stream(fixed_sources).collect(
			Collectors.toList());

		List<ConverterSetup> converterSetups = Arrays.stream(moving_sources).map(
			src -> source_service.getConverterSetup(src)).collect(Collectors.toList());
		converterSetups.addAll(Arrays.stream(fixed_sources).map(src -> source_service
			.getConverterSetup(src)).collect(Collectors.toList()));

		// Launch BigWarp
		BigWarpLauncher bwl = new BigWarpLauncher(movingSources, fixedSources,
				bigwarp_name, converterSetups);
		bwl.run();

		// Output bdvh handles -> will be put in the object service
		bdvh_q = bwl.getBdvHandleQ();
		bdvh_p = bwl.getBdvHandleP();

		bsds.pairClosing(bdvh_q, bdvh_p);

		grid_source = bwl.getGridSource();
		warp_magnitude_source = bwl.getWarpMagnitudeSource();
		warped_sources = bwl.getWarpedSources();

	}

}
