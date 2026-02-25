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

package sc.fiji.bdvpg.command.process.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvPgMenus;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceServices;
import sc.fiji.bdvpg.source.SourceAndTimeRange;
import sc.fiji.bdvpg.source.transform.SourceTransformHelper;

/**
 * @author Nicolas Chiaruttini, EPFL 2021
 */

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menu = {
			@Menu(label = BdvPgMenus.L1),
			@Menu(label = BdvPgMenus.L2),
			@Menu(label = BdvPgMenus.ProcessMenu, weight = BdvPgMenus.ProcessW),
			@Menu(label = "Transform"),
			@Menu(label = "Source - Affine Transformation", weight = 1)
	},
	description = "Applies an affine transformation on several sources.")

public class SourceTransformCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select source(s)",
			description = "The source(s) to transform")
	SourceAndConverter<?>[] sources;

	@Parameter(label = "Matrix as comma separated numbers", required = false,
		callback = "parseInput",
		description = "Optional: paste a 3x4 affine matrix as 12 comma-separated values (row-major order)")
	String matrix_csv;

	@Parameter(label = "Affine matrix coefficients",
			description = "3x4 affine transformation matrix coefficients",
			style = "format:0.#####E0")
	double m00 = 1, m01 = 0, m02 = 0, tx = 0, m10 = 0, m11 = 1, m12 = 0, ty = 0,
			m20 = 0, m21 = 0, m22 = 1, tz = 0;

	@Parameter(label = "Initial timepoint",
			description = "First timepoint to apply the transformation (0-based)")
	int ini_timepoint;

	@Parameter(label = "Number of timepoints",
			description = "Number of timepoints to apply the transformation to",
			min = "1")
	int n_timepoints;

	@Override
	public void run() {

		for (SourceAndConverter<?> source : sources) {
			AffineTransform3D at3D_global = new AffineTransform3D();

			at3D_global.set(m00, m01, m02, tx, m10, m11, m12, ty, m20, m21, m22, tz);

			if (source.getSpimSource() instanceof TransformedSource) {
				SourceTransformHelper.mutate(at3D_global,
					new SourceAndTimeRange(source, ini_timepoint));
			}
			else {

				SourceTransformHelper.append(at3D_global,
					new SourceAndTimeRange(source, ini_timepoint, ini_timepoint +
							n_timepoints));
			}
		}

		SourceServices.getBdvDisplayService().updateDisplays(sources);
	}

	public void parseInput() {
		// "1.0, 0.0, 0.0, -1.9866937698669376, 0.0, 1.0, 0.0, -2.5403625254036246,
		// 0.0, 0.0, 1.0, 0.0"
		if (matrix_csv != null) {
			String[] inputs = matrix_csv.split(",");
			if (inputs.length == 12) {
				try {
					m00 = Double.parseDouble(inputs[0]);
					m01 = Double.parseDouble(inputs[1]);
					m02 = Double.parseDouble(inputs[2]);
					tx = Double.parseDouble(inputs[3]);
					m10 = Double.parseDouble(inputs[4]);
					m11 = Double.parseDouble(inputs[5]);
					m12 = Double.parseDouble(inputs[6]);
					ty = Double.parseDouble(inputs[7]);
					m20 = Double.parseDouble(inputs[8]);
					m21 = Double.parseDouble(inputs[9]);
					m22 = Double.parseDouble(inputs[10]);
					tz = Double.parseDouble(inputs[11]);
				}
				catch (Exception e) {
					IJ.log("Number parsing exception: " + e.getMessage());
				}
			}
			else {
				IJ.log("A matrix should have 12 elements");
			}
		}
	}
}
