/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.viewer.SourceAndConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.log.SystemLogger;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Consumer;

/**
 * In contrast to ConverterChanger, this action do not create a new
 * SourceAndConverter to change the color of the displayed source. However, this
 * means that the converter has to be of instance ColorConverter One way around
 * this is to create more generic converters from the beginning but this would
 * have drawbacks: - how to save the converter in spimdata ?
 */
public class ColorChanger implements Runnable, Consumer<SourceAndConverter<?>> {

	final SourceAndConverter<?> sac;
	final ARGBType color;

	public ColorChanger(SourceAndConverter<?> sac, ARGBType color) {
		this.sac = sac;
		this.color = color;
	}

	@Override
	public void run() {
		accept(sac);
	}

	@Override
	public void accept(SourceAndConverter sourceAndConverter) {
		if (sourceAndConverter.getConverter() instanceof ColorConverter) {
			((ColorConverter) sourceAndConverter.getConverter()).setColor(color);
			if (sourceAndConverter.asVolatile() != null) {
				((ColorConverter) sourceAndConverter.asVolatile().getConverter())
					.setColor(color);
			}
			// Updates display, if any
			if (SourceAndConverterServices.getBdvDisplayService() != null)
				SourceAndConverterServices.getSourceAndConverterService()
					.getConverterSetup(sourceAndConverter).setColor(color);
		}
		else {
			new SystemLogger().err(
				"sourceAndConverter Converter is not an instance of Color Converter");
		}
	}
}
