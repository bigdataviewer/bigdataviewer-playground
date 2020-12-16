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
package bdv.util;

import bdv.tools.brightness.ConverterSetup;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.listeners.Listeners;

import java.util.Arrays;
import java.util.List;

/**
 *
 * {@link ConverterSetup} used to control a {@link ColorConverter}
 * {@link ConverterSetup} used for colored Sources in bigdataviewer-playground  - Why is this necessary ? TODO : ask Christian because I don't remember
 *
 * Note that if the setup of the converter is changed (min max, color...),
 * all the listeners are called, which usually triggers repainting if the source is displayed
 *
 *
 */


public class ARGBColorConverterSetup implements ConverterSetup
{

    protected final List<ColorConverter> converters;

    private final Listeners.List< SetupChangeListener > listeners = new Listeners.SynchronizedList<>();

    public ARGBColorConverterSetup( final ColorConverter ... converters )
    {
        this( Arrays.asList( converters ) );
    }

    public ARGBColorConverterSetup( final List< ColorConverter > converters )
    {
        this.converters = converters;
    }

    @Override
    public void setDisplayRange( final double min, final double max )
    {
        for ( final ColorConverter converter : converters )
        {
            converter.setMin( min );
            converter.setMax( max );
        }

        listeners.list.forEach(scl -> scl.setupParametersChanged(this));
    }

    @Override
    public void setColor( final ARGBType color )
    {
        for ( final ColorConverter converter : converters )
            converter.setColor( color );

        listeners.list.forEach(scl -> scl.setupParametersChanged(this));
    }

    @Override
    public boolean supportsColor()
    {
        return converters.get( 0 ).supportsColor();
    }

    @Override
    public int getSetupId()
    {
        return 0;
    }

    @Override
    public double getDisplayRangeMin()
    {
        return converters.get( 0 ).getMin();
    }

    @Override
    public double getDisplayRangeMax()
    {
        return converters.get( 0 ).getMax();
    }

    @Override
    public ARGBType getColor()
    {
        return converters.get( 0 ).getColor();
    }

    @Override
    public Listeners<SetupChangeListener> setupChangeListeners() {
        return listeners;
    }
}
