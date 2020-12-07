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

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Nicolas Chiaruttini, EPFL 2020
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Register>Launch BigWarp",
        description = "Starts BigWarp from existing sources")

public class BigWarpLauncherCommand implements Command {

    @Parameter(label = "Window title for BigWarp")
    String bigWarpName;

    @Parameter(label = "Moving Source(s)")
    SourceAndConverter[] movingSources;

    @Parameter(label = "Fixed Source(s)")
    SourceAndConverter[] fixedSources;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvhQ;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvhP;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter[] warpedSources;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter gridSource;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter warpMagnitudeSource;

    @Parameter
	SourceAndConverterBdvDisplayService bsds;

    public void run() {
        List<SourceAndConverter> movingSacs = Arrays.stream(movingSources).collect(Collectors.toList());
        List<SourceAndConverter> fixedSacs = Arrays.stream(fixedSources).collect(Collectors.toList());

        List<ConverterSetup> converterSetups = Arrays.stream(movingSources).map(src -> bsds.getConverterSetup(src)).collect(Collectors.toList());
        converterSetups.addAll(Arrays.stream(fixedSources).map(src -> bsds.getConverterSetup(src)).collect(Collectors.toList()));

        // Launch BigWarp
        BigWarpLauncher bwl = new BigWarpLauncher(movingSacs, fixedSacs, bigWarpName, converterSetups);
        bwl.run();

        // Output bdvh handles -> will be put in the object service
        bdvhQ = bwl.getBdvHandleQ();
        bdvhP = bwl.getBdvHandleP();

        bsds.pairClosing(bdvhQ,bdvhP);

        gridSource = bwl.getGridSource();
        warpMagnitudeSource = bwl.getWarpMagnitudeSource();
        warpedSources = bwl.getWarpedSources();

    }

}
