/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.scijava.command.bvv;

import bdv.viewer.SourceAndConverter;
import bvv.vistools.BvvHandle;
import ij.IJ;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.viewers.ViewerAdapter;

/**
 * Show sources in a BigVolumeViewer window - limited to 16 bit images
 */

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu + "BVV>BVV - Show Sources",
	description = "Show sources in a BigVolumeViewer window - limited to 16 bit images")
public class BvvSourcesAdderCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Select BVV Window(s)")
	BvvHandle bvvh;

	@Parameter(label = "Adjust View on Source")
	boolean adjustviewonsource;

	@Parameter(label = "Select source(s)")
	SourceAndConverter<?>[] sacs;

	@Override
	public void run() {

		for (SourceAndConverter<?> sac : sacs) {
			if (sac.getSpimSource().getType() instanceof UnsignedShortType) {

				bvvh.getConverterSetups().put(sac, SourceAndConverterServices
					.getSourceAndConverterService().getConverterSetup(sac));
				bvvh.getViewerPanel().state().addSource(sac);

				bvvh.getViewerPanel().state().setSourceActive(sac, true);
			}
			else {
				IJ.log("Source " + sac.getSpimSource().getName() +
					" is not an unsigned 16 bit image. Bvv does not support this kind of images (yet).");
			}
		}

		if ((adjustviewonsource) && (sacs.length > 0)) {
			new ViewerTransformAdjuster(new ViewerAdapter(bvvh), sacs).run();
		}

	}
}
