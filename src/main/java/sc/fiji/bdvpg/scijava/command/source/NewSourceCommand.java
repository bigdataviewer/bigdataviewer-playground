/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;

/**
 * Command which creates an empty Source based on a model Source
 * The created source will cover the same portion of space as the model source,
 * but with the specified voxel size, and at a specific timepoint
 *
 * @author Nicolas Chiaruttini, EPFL 2020
 */

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>New Source Based on Model Source",
description = "Defines an empty source which occupied the same volume as a model source but with a potentially" +
        " different voxel size. Works with a single timepoint.")

public class NewSourceCommand implements BdvPlaygroundActionCommand {
    
    @Parameter(label = "Model Source", description = "Defines the portion of space covered by the new source")
    SourceAndConverter model;

    @Parameter(label = "Source name")
    String name;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter sac;
    
    @Parameter(label = "Voxel Size X")
    double voxsizex;

    @Parameter(label = "Voxel Size Y")
    double voxsizey;

    @Parameter(label = "Voxel Size Z")
    double voxsizez;

    @Parameter(label = "Timepoint (0 based index)")
    int timepoint;

    @Override
    public void run() {
        sac = new EmptySourceAndConverterCreator(name, model, timepoint, voxsizex, voxsizey, voxsizez).get();//, factory).get();
    }
}
