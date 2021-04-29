/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.converter;

import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO : documentation - why is this here ? Is it for the transparency ?
 * @param <R> RealType
 */

public abstract class RealARGBColorConverter< R extends RealType< ? > > implements ColorConverter, Converter< R, ARGBType >
{
	protected double min;

	protected double max;

	protected final ARGBType color = new ARGBType( ARGBType.rgba( 255, 255, 255, 255 ) );

	protected int A;

	protected double scaleR;

	protected double scaleG;

	protected double scaleB;

	public RealARGBColorConverter( final double min, final double max )
	{
		this.min = min;
		this.max = max;
		update();
	}


	protected int black;

	// specify special colors for special values
	protected Map< Double, Integer > valueToColor = new HashMap<>(  );

	public Map< Double, Integer > getValueToColor()
	{
		return valueToColor;
	}

	@Override
	public ARGBType getColor()
	{
		return color.copy();
	}

	@Override
	public void setColor( final ARGBType c )
	{
		color.set( c );
		update();
	}

	@Override
	public boolean supportsColor()
	{
		return true;
	}

	@Override
	public double getMin()
	{
		return min;
	}

	@Override
	public double getMax()
	{
		return max;
	}

	@Override
	public void setMax( final double max )
	{
		this.max = max;
		update();
	}

	@Override
	public void setMin( final double min )
	{
		this.min = min;
		update();
	}

	private void update()
	{
		final double scale = 1.0 / ( max - min );
		final int value = color.get();
		A = ARGBType.alpha( value );
		scaleR = ARGBType.red( value ) * scale;
		scaleG = ARGBType.green( value ) * scale;
		scaleB = ARGBType.blue( value ) * scale;
		black = ARGBType.rgba( 0, 0, 0, A );
	}

	public static class Imp0< R extends RealType< ? > > extends RealARGBColorConverter< R >
	{
		public Imp0( final double min, final double max )
		{
			super( min, max );
		}

		@Override
		public void convert( final R input, final ARGBType output )
		{
			final double value = input.getRealDouble();

			if ( valueToColor.containsKey( value ) )
			{
				output.set( valueToColor.get( value ) );
				return;
			}

			final double valueMinusMin = value - min;
			if ( valueMinusMin < 0 )
			{
				output.set( black );
			}
			else
			{
				final int r0 = ( int ) ( scaleR * valueMinusMin + 0.5 );
				final int g0 = ( int ) ( scaleG * valueMinusMin + 0.5 );
				final int b0 = ( int ) ( scaleB * valueMinusMin + 0.5 );
				final int r = Math.min( 255, r0 );
				final int g = Math.min( 255, g0 );
				final int b = Math.min( 255, b0 );
				output.set( ARGBType.rgba( r, g, b, A) );
			}
		}
	}

	public static class Imp1< R extends RealType< ? > > extends RealARGBColorConverter< R >
	{
		public Imp1( final double min, final double max )
		{
			super( min, max );
		}

		@Override
		public void convert( final R input, final ARGBType output )
		{
			final double value = input.getRealDouble();

			if ( valueToColor.containsKey( value ) )
			{
				output.set( valueToColor.get( value ) );
				return;
			}

			final double valueMinusMin = value - min;
			if ( valueMinusMin < 0 )
			{
				output.set( black );
			}
			else
			{
				final int r0 = ( int ) ( scaleR * valueMinusMin + 0.5 );
				final int g0 = ( int ) ( scaleG * valueMinusMin + 0.5 );
				final int b0 = ( int ) ( scaleB * valueMinusMin + 0.5 );
				final int r = Math.min( 255, r0 );
				final int g = Math.min( 255, g0 );
				final int b = Math.min( 255, b0 );
				output.set( ARGBType.rgba( r, g, b, A) );
			}
		}
	}
}
