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

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

/**
 *
 * @author Nicolas Chiaruttini, EPFL 2021
 */

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Transform>Sources Affine Transformation",
description = "Applies an affine transformation on several sources.")

public class SourceTransformerCommand implements BdvPlaygroundActionCommand {
    @Parameter(label = "Select source(s)")
    SourceAndConverter[] sacs;

    @Parameter
    double m00 = 1, m01 = 0, m02 = 0, tx = 0, m10 = 0, m11 = 1, m12 = 0, ty = 0, m20 = 0, m21 = 0, m22 = 1, tz = 0;

    @Parameter(label = "Initial timepoint (0 based)")
    int initimepoint;

    @Parameter(label = "Number of timepoints (min 1)", min = "1")
    int ntimepoints;

    @Override
    public void run() {

        for (SourceAndConverter sac : sacs) {
            AffineTransform3D at3D_global = new AffineTransform3D();

            at3D_global.set(m00, m01,m02,tx,m10,m11,m12,ty,m20,m21,m22,tz);

            if (sac.getSpimSource() instanceof TransformedSource) {
                SourceTransformHelper.mutate(at3D_global, new SourceAndConverterAndTimeRange(sac, initimepoint));
            } else {

                SourceTransformHelper.append(at3D_global, new SourceAndConverterAndTimeRange(sac, initimepoint, initimepoint+ntimepoints));
            }
        }

        SourceAndConverterServices.getBdvDisplayService()
                .updateDisplays(sacs);
    }
}
