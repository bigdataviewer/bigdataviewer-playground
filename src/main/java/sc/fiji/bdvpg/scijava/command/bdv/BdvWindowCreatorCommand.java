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
package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.*;
import bdv.viewer.render.AccumulateProjectorFactory;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.projector.AccumulateAverageProjectorARGB;
import sc.fiji.bdvpg.bdv.projector.AccumulateMixedProjectorARGBFactory;
import sc.fiji.bdvpg.bdv.projector.Projector;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Create empty BDV window",
    description = "Creates an empty BDV window")
public class BdvWindowCreatorCommand implements BdvPlaygroundActionCommand {

    @Parameter(label = "Create a 2D BDV window")
    public boolean is2d = false;

    @Parameter(label = "Title of the new BDV window")
    public String windowtitle = "BDV";

    @Parameter(label = "Interpolate")
    public boolean interpolate = false;

    @Parameter(label = "Number of timepoints (1 for a single timepoint)")
    public int ntimepoints = 1;

    @Parameter(required = false, choices = { Projector.MIXED_PROJECTOR, Projector.SUM_PROJECTOR, Projector.AVERAGE_PROJECTOR})
    public String projector;

    /**
     * This triggers: {@link sc.fiji.bdvpg.scijava.processors.BdvHandlePostprocessor}
     */
    @Parameter(type = ItemIO.OUTPUT)
    public BdvHandle bdvh;

    @Override
    public void run() {
        if ((projector==null)||(projector.trim().equals(""))) projector = Projector.SUM_PROJECTOR; // Default mode if nothing is set

        //------------ BdvHandleFrame
        BdvOptions opts = BdvOptions.options().frameTitle(windowtitle);
        if (is2d) opts = opts.is2D();

        // Create accumulate projector factory
        AccumulateProjectorFactory< ARGBType > factory;
        switch (projector) {
            case Projector.MIXED_PROJECTOR:
                factory = new AccumulateMixedProjectorARGBFactory(  );
                opts = opts.accumulateProjectorFactory(factory);
            case Projector.SUM_PROJECTOR:
                // Default projector
                break;
            case Projector.AVERAGE_PROJECTOR:
                factory = AccumulateAverageProjectorARGB.factory;
                opts = opts.accumulateProjectorFactory(factory);
                break;
            default:
        }

        BdvCreator creator = new BdvCreator(opts, interpolate, ntimepoints);
        creator.run();
        bdvh = creator.get();

        final SourceAndConverterBdvDisplayService displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();

        switch (projector) {
            case Projector.MIXED_PROJECTOR:
                displayService.setDisplayMetadata( bdvh, Projector.PROJECTOR, Projector.MIXED_PROJECTOR );
                break;
            case Projector.SUM_PROJECTOR:
                displayService.setDisplayMetadata( bdvh, Projector.PROJECTOR, Projector.SUM_PROJECTOR );
                break;
            case Projector.AVERAGE_PROJECTOR:
                displayService.setDisplayMetadata( bdvh, Projector.PROJECTOR, Projector.AVERAGE_PROJECTOR );
                break;
            default:
        }
    }

}
