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
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.log.SystemLogger;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.importer.MandelbrotSourceGetter;
import sc.fiji.bdvpg.sourceandconverter.importer.VoronoiSourceGetter;
import sc.fiji.bdvpg.sourceandconverter.importer.Wave3DSourceGetter;

/**
 *
 * @author Nicolas Chiaruttini, EPFL 2020
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Create Sample Source")
public class SampleSourceCreatorCommand implements Command {

    @Parameter(label="Sample name", choices = {"Mandelbrot", "Wave3D", "Voronoi", "Big Voronoi"})
    String sampleName;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter sampleSource;

    @Override
    public void run() {
        switch(sampleName) {

            case "Mandelbrot":
                sampleSource = (new MandelbrotSourceGetter()).get();
                break;

            case "Wave3D":
                sampleSource = (new Wave3DSourceGetter()).get();
                break;

            case "Voronoi":
                sampleSource = (new VoronoiSourceGetter(new long[]{512, 512, 1}, 256, true).get());
                break;

            case "Big Voronoi":
                sampleSource = (new VoronoiSourceGetter(new long[]{2048, 2048, 2048}, 65536, false).get());
                break;

            default:
                new SystemLogger().err("Invalid sample name");
                return;
        }
    }
}
