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

import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.ConverterChanger;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu +
		"Sources>Display>Create New Source (Set Color)",
	description = "Duplicate one or several sources and sets a new color for these sources")

public class ColorSourceCreatorCommand implements BdvPlaygroundActionCommand {

	@Parameter
	ColorRGB color = new ColorRGB(255, 255, 255);

	@Parameter(label = "Select Source(s)")
	SourceAndConverter<?>[] sacs;

	@Override
	public void run() {
		for (SourceAndConverter<?> source : sacs) {
			createAndChangeConverter(source);
		}
	}

	private <T> void createAndChangeConverter(SourceAndConverter<T> source) {
		ARGBType imglib2color = new ARGBType(ARGBType.rgba(color.getRed(), color
			.getGreen(), color.getBlue(), color.getAlpha()));

		Converter<T, ARGBType> c = SourceAndConverterHelper.createConverter(source
			.getSpimSource()); // TODO : Should it be Converter<?,ARGBType> ?
		assert c instanceof ColorConverter;
		((ColorConverter) c).setColor(imglib2color);

		Converter<? extends Volatile<T>, ARGBType> vc = null;
		if (source.asVolatile() != null) {
			vc = SourceAndConverterHelper.createConverter(source.asVolatile()
				.getSpimSource());
			assert vc != null;
			((ColorConverter) vc).setColor(imglib2color);
		}

		ConverterChanger<T> cc = new ConverterChanger<>(source, c, vc);
		cc.run();
		cc.get();
	}

}
