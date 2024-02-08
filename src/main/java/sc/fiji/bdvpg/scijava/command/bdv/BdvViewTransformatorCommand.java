/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2024 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

/**
 * ViewTransformLoggerCommand Author: @haesleinhuepf 12 2019
 */

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu + "BDV>BDV - Change view transform",
	description = "Applies a simple view transform (translation / rotation) to a BDV window")
public class BdvViewTransformatorCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select BDV Windows")
	BdvHandle bdvh;

	@Parameter(label = "Translate in X")
	public Double translatex = 0.0;

	@Parameter(label = "Translate in Y")
	public Double translatey = 0.0;

	@Parameter(label = "Translate in Z")
	public Double translatez = 0.0;

	@Parameter(label = "Rotate around X")
	public Double rotatearoundx = 0.0;

	@Parameter(label = "Rotate around Y")
	public Double rotatearoundy = 0.0;

	@Parameter(label = "Rotate around Z")
	public Double rotatearoundz = 0.0;

	@Override
	public void run() {
		AffineTransform3D affineTransform3D = new AffineTransform3D();
		affineTransform3D.translate(translatex, translatey, translatez);
		affineTransform3D.rotate(0, rotatearoundx);
		affineTransform3D.rotate(1, rotatearoundy);
		affineTransform3D.rotate(2, rotatearoundz);

		new ViewerTransformChanger(bdvh, affineTransform3D, true, 0).run();
	}
}
