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
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvPgMenus;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceBdvDisplayService;
import sc.fiji.bdvpg.source.SourceAndTimeRange;
import sc.fiji.bdvpg.source.transform.SourceTransformHelper;

/**
 * Not clever but consistent : always append transform which acts as if it was
 * inserted at first position Maybe not good numerically speaking - but at least
 * it's consistent and there's no special case depending on the type of the
 * SourceAndConverter
 *
 * @author Nicolas Chiaruttini, EPFL 2020
 */

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menu = {
			@Menu(label = BdvPgMenus.L1),
			@Menu(label = BdvPgMenus.L2),
			@Menu(label = BdvPgMenus.ProcessMenu, weight = BdvPgMenus.ProcessW),
			@Menu(label = "Transform", weight = -2),
			@Menu(label = "Source - Basic Transformation", weight = 1)
	},
	description = "Performs basic transformation (rotate / flip) along X Y Z axis for several sources. " +
		"If global is selected, the transformation is performed relative to the global origin (0,0,0). " +
		"If global is not selected, the center of each source is unchanged.")

public class SourceTransformSimpleCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select source(s)",
			description = "The source(s) to transform")
	SourceAndConverter<?>[] sources;

	@Parameter(label = "Transformation type",
			description = "Type of transformation: Flip mirrors the source, Rot rotates by 90/180/270 degrees",
			choices = { "Flip", "Rot90", "Rot180", "Rot270" })
	String type;

	@Parameter(label = "Axis",
			description = "Axis along which to perform the transformation",
			choices = { "X", "Y", "Z" })
	String axis;

	@Parameter(label = "Initial timepoint",
			description = "First timepoint to apply the transformation (0-based)")
	int ini_timepoint;

	@Parameter(label = "Number of timepoints",
			description = "Number of timepoints to apply the transformation to",
			min = "1")
	int n_timepoints;

	@Parameter(label = "Global transform",
			description = "If checked, transforms relative to world origin (0,0,0). Otherwise, keeps each source center unchanged")
	boolean global_change;

	@Parameter
    SourceBdvDisplayService bdvDisplayService;

	@Override
	public void run() {
		for (SourceAndConverter<?> source : sources) {
			{
				AffineTransform3D at3D_global = new AffineTransform3D();
				at3D_global.identity();
				switch (type) {
					case "Flip":
						flip(at3D_global);
						break;
					case "Rot90":
						rot(1, at3D_global);
						break;
					case "Rot180":
						rot(2, at3D_global);
						break;
					case "Rot270":
						rot(3, at3D_global);
						break;
				}
				if (global_change) {
					if (source.getSpimSource() instanceof TransformedSource) {
						SourceTransformHelper.mutate(at3D_global,
							new SourceAndTimeRange<>(source, ini_timepoint,
								ini_timepoint + n_timepoints));
					}
					else {
						SourceTransformHelper.append(at3D_global,
							new SourceAndTimeRange<>(source, ini_timepoint,
								ini_timepoint + n_timepoints));
					}
				}
				else {
					for (int timepoint = ini_timepoint; timepoint < ini_timepoint +
							n_timepoints; timepoint++)
					{
						// Maintain center of box constant
						AffineTransform3D at3D = new AffineTransform3D();
						at3D.identity();
						// double[] m = at3D.getRowPackedCopy();
						source.getSpimSource().getSourceTransform(timepoint, 0, at3D);
						long[] dims = new long[3];
						source.getSpimSource().getSource(timepoint, 0).dimensions(dims);

						RealPoint ptCenterGlobalBefore = new RealPoint(3);
						RealPoint ptCenterPixel = new RealPoint((dims[0] - 1.0) / 2.0,
							(dims[1] - 1.0) / 2.0, (dims[2] - 1.0) / 2.0);

						at3D.apply(ptCenterPixel, ptCenterGlobalBefore);

						RealPoint ptCenterGlobalAfter = new RealPoint(3);

						at3D_global.apply(ptCenterGlobalBefore, ptCenterGlobalAfter);

						// Just shifting
						double[] m = at3D_global.getRowPackedCopy();

						m[3] -= ptCenterGlobalAfter.getDoublePosition(0) -
							ptCenterGlobalBefore.getDoublePosition(0);

						m[7] -= ptCenterGlobalAfter.getDoublePosition(1) -
							ptCenterGlobalBefore.getDoublePosition(1);

						m[11] -= ptCenterGlobalAfter.getDoublePosition(2) -
							ptCenterGlobalBefore.getDoublePosition(2);

						at3D_global.set(m);

						if (source.getSpimSource() instanceof TransformedSource) {
							SourceTransformHelper.mutate(at3D_global,
								new SourceAndTimeRange<>(source, timepoint));
						}
						else {
							SourceTransformHelper.append(at3D_global,
								new SourceAndTimeRange<>(source, timepoint));
						}
					}
				}
			}
		}
		bdvDisplayService.updateDisplays(sources);
	}

	private void flip(AffineTransform3D at3D) {
		switch (axis) {
			case "X":
				at3D.set(-1, 0, 0);
				break;
			case "Y":
				at3D.set(-1, 1, 1);
				break;
			case "Z":
				at3D.set(-1, 2, 2);
				break;
		}
	}

	private void rot(int quarterTurn, AffineTransform3D at3D) {
		switch (axis) {
			case "X":
				at3D.rotate(0, ((double) quarterTurn) * Math.PI / 2.0);
				break;
			case "Y":
				at3D.rotate(1, ((double) quarterTurn) * Math.PI / 2.0);
				break;
			case "Z":
				at3D.rotate(2, ((double) quarterTurn) * Math.PI / 2.0);
				break;
		}
	}
}
