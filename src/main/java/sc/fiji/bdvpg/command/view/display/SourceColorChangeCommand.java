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

package sc.fiji.bdvpg.command.view.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.scijava.BdvPgMenus;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceServices;
import sc.fiji.bdvpg.source.display.ColorChanger;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menu = {
			@Menu(label = BdvPgMenus.L1),
			@Menu(label = BdvPgMenus.L2),
			@Menu(label = BdvPgMenus.ViewMenu, weight = BdvPgMenus.ViewW),
			@Menu(label = "Source", weight = 3),
			@Menu(label = "Source - Set Color", weight = 1)
	},
	description = "Changes the display color of one or more sources")
public class SourceColorChangeCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Color",
			description = "The new display color for the selected sources")
	ColorRGB color = new ColorRGB(255, 255, 255);

	@Parameter(label = "Select Source(s)",
			description = "The source(s) whose color will be changed")
	SourceAndConverter<?>[] sources;

	@Override
	public void run() {
		ARGBType imglib2color = new ARGBType(ARGBType.rgba(color.getRed(), color
			.getGreen(), color.getBlue(), 255));// Fully opaque color.getAlpha()));
		for (SourceAndConverter<?> source : sources) {
			new ColorChanger(source, imglib2color).run();
		}
		SourceServices.getBdvDisplayService().updateDisplays(sources);
	}

}
