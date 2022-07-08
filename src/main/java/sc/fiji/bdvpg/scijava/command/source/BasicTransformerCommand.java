/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

/**
 * Not clever but consistent : always append transform which acts as if it was inserted at first position
 * Maybe not good numerically speaking - but at least it's consistent and there's
 * no special case depending on the type of the SourceAndConverter
 *
 * @author Nicolas Chiaruttini, EPFL 2020
 */

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Transform>Basic Transformation",
description = "Performs basic transformation (rotate / flip) along X Y Z axis for several sources. " +
        "If global is selected, the transformation is performed relative to the global origin (0,0,0). " +
        "If global is not selected, the center of each source is unchanged.")

public class BasicTransformerCommand implements BdvPlaygroundActionCommand {
    @Parameter(label = "Select source(s)")
    SourceAndConverter<?>[] sacs;

    @Parameter(choices = {"Flip", "Rot90", "Rot180", "Rot270"})
    String type;

    @Parameter(choices = {"X", "Y", "Z"})
    String axis;

    @Parameter(label = "Initial timepoint (0 based)")
    int initimepoint;

    @Parameter(label = "Number of timepoints (min 1)", min = "1")
    int ntimepoints;

    @Parameter(label = "Global transform (relative to the origin of the world)")
    boolean globalchange;

    @Override
    public void run() {
        for (SourceAndConverter<?> sac : sacs) {
            {
                AffineTransform3D at3D_global = new AffineTransform3D();
                at3D_global.identity();
                switch (type) {
                    case "Flip": flip(at3D_global);
                    break;
                    case "Rot90": rot(1, at3D_global );
                    break;
                    case "Rot180": rot(2, at3D_global );
                    break;
                    case "Rot270": rot(3, at3D_global );
                    break;
                }
                if (globalchange) {
                    if (sac.getSpimSource() instanceof TransformedSource) {
                        SourceTransformHelper.mutate(at3D_global, new SourceAndConverterAndTimeRange(sac, initimepoint, initimepoint+ntimepoints));
                    } else {
                        SourceTransformHelper.append(at3D_global, new SourceAndConverterAndTimeRange(sac, initimepoint, initimepoint+ntimepoints));
                    }
                } else {
                    for (int timepoint = initimepoint; timepoint< initimepoint+ntimepoints; timepoint++){
                        // Maintain center of box constant
                        AffineTransform3D at3D = new AffineTransform3D();
                        at3D.identity();
                        //double[] m = at3D.getRowPackedCopy();
                        sac.getSpimSource().getSourceTransform(timepoint, 0, at3D);
                        long[] dims = new long[3];
                        sac.getSpimSource().getSource(timepoint, 0).dimensions(dims);

                        RealPoint ptCenterGlobalBefore = new RealPoint(3);
                        RealPoint ptCenterPixel = new RealPoint((dims[0] - 1.0) / 2.0, (dims[1] - 1.0) / 2.0, (dims[2] - 1.0) / 2.0);

                        at3D.apply(ptCenterPixel, ptCenterGlobalBefore);

                        RealPoint ptCenterGlobalAfter = new RealPoint(3);

                        at3D_global.apply(ptCenterGlobalBefore, ptCenterGlobalAfter);

                        // Just shifting
                        double[] m = at3D_global.getRowPackedCopy();

                        m[3] -= ptCenterGlobalAfter.getDoublePosition(0) - ptCenterGlobalBefore.getDoublePosition(0);

                        m[7] -= ptCenterGlobalAfter.getDoublePosition(1) - ptCenterGlobalBefore.getDoublePosition(1);

                        m[11] -= ptCenterGlobalAfter.getDoublePosition(2) - ptCenterGlobalBefore.getDoublePosition(2);

                        at3D_global.set(m);

                        if (sac.getSpimSource() instanceof TransformedSource) {
                            SourceTransformHelper.mutate(at3D_global, new SourceAndConverterAndTimeRange(sac, timepoint));
                        } else {
                            SourceTransformHelper.append(at3D_global, new SourceAndConverterAndTimeRange(sac, timepoint));
                        }
                        //SourceTransformHelper.append(at3D_global, new SourceAndConverterAndTimeRange(sac, timepoint));
                    }
                }
            }
        }
        SourceAndConverterServices.getBdvDisplayService()
                .updateDisplays(sacs);
    }

    private void flip(AffineTransform3D at3D) {
        switch (axis) {
            case "X":
                at3D.set(-1,0,0);
                break;
            case "Y":
                at3D.set(-1,1,1);
                break;
            case "Z":
                at3D.set(-1,2,2);
                break;
        }
    }

    private void rot(int quarterTurn, AffineTransform3D at3D) {
        switch (axis) {
            case "X":
                at3D.rotate(0,((double)quarterTurn)*Math.PI/2.0);
                break;
            case "Y":
                at3D.rotate(1,((double)quarterTurn)*Math.PI/2.0);
                break;
            case "Z":
                at3D.rotate(2,((double)quarterTurn)*Math.PI/2.0);
                break;
        }
    }
}
