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
package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

/**
 * ViewTransformLoggerCommand
 * <p>
 * <p>
 * <p>
 * Author: @haesleinhuepf
 * 12 2019
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Change view transform",
    description = "Applies a simple view transform (translation / rotation) to a BDV window")
public class ViewTransformatorCommand implements Command {

    @Parameter(label = "Select BDV Windows")
    BdvHandle bdvh;

    @Parameter(label="Translate in X")
    public Double translateX = 0.0;

    @Parameter(label="Translate in Y")
    public Double translateY = 0.0;

    @Parameter(label="Translate in Z")
    public Double translateZ = 0.0;

    @Parameter(label="Rotate around X")
    public Double rotateAroundX = 0.0;

    @Parameter(label="Rotate around Y")
    public Double rotateAroundY = 0.0;

    @Parameter(label="Rotate around Z")
    public Double rotateAroundZ = 0.0;

    @Override
    public void run() {
        AffineTransform3D affineTransform3D = new AffineTransform3D();
        affineTransform3D.translate(translateX, translateY, translateZ);
        affineTransform3D.rotate(0, rotateAroundX);
        affineTransform3D.rotate(1, rotateAroundY);
        affineTransform3D.rotate(2, rotateAroundZ);

        new ViewerTransformChanger(bdvh, affineTransform3D, true, 0).run();
    }
}
