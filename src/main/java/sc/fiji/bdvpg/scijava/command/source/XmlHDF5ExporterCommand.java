/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.exporter.XmlHDF5SpimdataExporter;

import java.io.File;
import java.util.Arrays;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Export Sources to XML/HDF5 Spimdataset")
public class XmlHDF5ExporterCommand implements Command {

    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sacs;

    @Parameter(label="# of Threads")
    int nThreads = 4;

    @Parameter(label="Timepoint (Beginning)")
    int timePointBegin = 0;

    @Parameter(label="Timepoint (End)")
    int timePointEnd = 1;

    @Parameter
    int scaleFactor = 4;

    @Parameter
    int blockSizeX = 64;

    @Parameter
    int blockSizeY = 64;

    @Parameter
    int blockSizeZ = 64;

    @Parameter(label = "Dimensions in pixel above which a new resolution level should be created")
    int thresholdForMipmap = 512;

    @Parameter(label="Output file (XML)")
    File xmlFile;

    @Override
    public void run() {
        new XmlHDF5SpimdataExporter(Arrays.asList(sacs),nThreads,timePointBegin,timePointEnd,scaleFactor,blockSizeX,blockSizeY,blockSizeZ, thresholdForMipmap,xmlFile).run();
    }

}
