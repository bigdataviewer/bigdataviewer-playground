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

package sc.fiji.bdvpg.command.io.exporter;

import bdv.viewer.SourceAndConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.source.exporter.XmlHDF5SpimdataExporter;

import java.io.File;
import java.util.Arrays;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu +
		"Export>Source - Export Sources To XML/HDF5 Dataset",
	description = "Exports sources to an XML/HDF5 BigDataViewer dataset")
public class SourceExportXmlHDF5Command implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select Source(s)",
			description = "The source(s) to export")
	SourceAndConverter<?>[] sources;

	@Parameter(label = "Each source is an independent",
			description = "How to treat each source in the exported dataset",
			choices = { "Channel", "Tile" })
	String entity_type;

	@Parameter(label = "Number of Threads",
			description = "Number of parallel threads for export")
	int n_threads = 4;

	@Parameter(label = "Timepoint start",
			description = "First timepoint to export (0-based)")
	int timepoint_begin = 0;

	@Parameter(label = "Number of timepoints",
			description = "Number of timepoints to export",
			min = "1")
	int number_of_timepoints_to_export = 1;
	int timepointend = -1;

	@Parameter(label = "Scale factor",
			description = "Downsampling factor between pyramid levels")
	int scale_factor = 4;

	@Parameter(label = "Block size X",
			description = "HDF5 block size in X dimension")
	int block_size_x = 64;

	@Parameter(label = "Block size Y",
			description = "HDF5 block size in Y dimension")
	int block_size_y = 64;

	@Parameter(label = "Block size Z",
			description = "HDF5 block size in Z dimension")
	int block_size_z = 64;

	@Parameter(label = "MipMap threshold",
		description = "Minimum dimension size (in pixels) above which a new resolution level is created")
	int threshold_mipmap = 512;

	@Parameter(label = "Output file (XML)",
			description = "Path to the output XML file",
			style = "save")
	File xml_file;

	@Override
	public void run() {
		timepointend = timepoint_begin + number_of_timepoints_to_export;
		new XmlHDF5SpimdataExporter(Arrays.asList(sources), entity_type, n_threads,
				timepoint_begin, timepointend, scale_factor, block_size_x, block_size_y,
				block_size_z, threshold_mipmap, xml_file).run();
	}

}
