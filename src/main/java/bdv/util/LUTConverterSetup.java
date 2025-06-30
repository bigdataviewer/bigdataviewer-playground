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

package bdv.util;

import bdv.tools.brightness.ConverterSetup;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.type.numeric.ARGBType;
import java.util.Arrays;
import java.util.List;

import net.imglib2.type.numeric.RealType;
import org.scijava.listeners.Listeners;

/**
 * {@link ConverterSetup} used to control a {@link RealLUTConverter} Note that
 * if the setup of the converter is changed (min max, color...), all the
 * listeners are called, which usually triggers repainting if the source is
 * displayed TODO : create a more generic convertersetup which can handle
 * RealLUTConverter and ColorConverter
 */

public class LUTConverterSetup<R extends RealType<R>> implements
	ConverterSetup
{

	protected final List<RealLUTConverter<R>> converters;

	private final Listeners.List<SetupChangeListener> listeners =
		new Listeners.SynchronizedList<>();

	@SafeVarargs
	public LUTConverterSetup(final RealLUTConverter<R>... converters) {
		this(Arrays.asList(converters));
	}

	public LUTConverterSetup(final List<RealLUTConverter<R>> converters) {
		this.converters = converters;
	}

	@Override
	public void setDisplayRange(final double min, final double max) {
		for (final RealLUTConverter<R> converter : converters) {
			converter.setMin(min);
			converter.setMax(max);
		}

		listeners.list.forEach(scl -> scl.setupParametersChanged(this));
	}

	@Override
	public void setColor(final ARGBType color) {
		// Do nothing : unsupported
	}

	@Override
	public boolean supportsColor() {
		return false;
	}

	@Override
	public Listeners<SetupChangeListener> setupChangeListeners() {
		return listeners;
	}

	@Override
	public int getSetupId() {
		return 0;
	}

	@Override
	public double getDisplayRangeMin() {
		return converters.get(0).getMin();
	}

	@Override
	public double getDisplayRangeMax() {
		return converters.get(0).getMax();
	}

	@Override
	public ARGBType getColor() {
		return null;
	}

}
