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

package sc.fiji.bdvpg.scijava.command.source;

import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu +
	"Sources>Resample Source Based on Model Source",
	description = "Resamples sources to match the voxel grid of a model source")
public class SourcesResamplerCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select Source(s)",
			description = "The source(s) to resample")
	SourceAndConverter<?>[] sacs;

	@Parameter(label = "Model Source",
			description = "The source whose voxel grid will be used as reference")
	SourceAndConverter<?> model;

	@Parameter(label = "Re-use MipMaps",
			description = "If checked, reuses the MipMap levels of the original source")
	boolean reusemipmaps;

	@Parameter(label = "MipMap level if not re-used",
			description = "Resolution level to use when not reusing MipMaps (0 = highest resolution)")
	int defaultmipmaplevel;

	@Parameter(label = "Interpolate",
			description = "If checked, uses interpolation when resampling")
	boolean interpolate;

	@Parameter(label = "Cache",
			description = "If checked, caches the resampled data in memory")
	boolean cache;

	@Parameter(label = "Name(s) of the resampled source(s)",
			description = "Name(s) for the resampled source(s), comma-separated for multiple sources")
	String name; // CSV separate for multiple sources

	@Parameter(type = ItemIO.OUTPUT,
			label = "Resampled Sources",
			description = "The newly created resampled sources")
	SourceAndConverter<?>[] sacs_out;

	@Override
	public void run() {
		// Should not be parallel
		sacs_out = new SourceAndConverter<?>[sacs.length];
		final String[] names = name.split(",");
		for (int i = 0; i < sacs.length; i++) {
			SourceAndConverter<?> sac = sacs[i];
			sacs_out[i] = new SourceResampler(sac, model, names[i], reusemipmaps,
				cache, interpolate, defaultmipmaplevel).get();
		}
	}

}
