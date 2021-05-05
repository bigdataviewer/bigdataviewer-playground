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
package sc.fiji.bdvpg.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.viewer.Interpolation;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.ByteType;
import sc.fiji.bdvpg.bdv.projector.Projector;
import sc.fiji.bdvpg.bdv.projector.ProjectorFactory;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.function.Supplier;

/**
 * Creates a new {@link bdv.BigDataViewer} instance accessible through the {@link BdvHandle} interface
 *
 * In contrast to {@link BdvCreator} it does not add any Behaviours.
 */
public class MinimalBdvCreator implements Supplier< BdvHandle  >
{
	private String windowTitle;
	private boolean is2D;
	private String projector;
	private boolean interpolate;
	private int numTimePoints;

	public MinimalBdvCreator( String windowTitle, boolean is2D, String projector, boolean interpolate, int numTimePoints )
	{
		this.windowTitle = windowTitle;
		this.is2D = is2D;
		this.projector = projector;
		this.interpolate = interpolate;
		this.numTimePoints = numTimePoints;
	}

	@Override
	public BdvHandle get()
	{
		BdvOptions bdvOptions = createBdvOptions();
		BdvHandle bdvHandle = createBdv( bdvOptions, interpolate, numTimePoints );
		registerAtBdvDisplayService( bdvHandle );
		return bdvHandle;
	}

	private void registerAtBdvDisplayService( BdvHandle bdvHandle )
	{
		registerProjectionMode( bdvHandle );
	}

	private void registerProjectionMode( BdvHandle bdvHandle )
	{
		final SourceAndConverterBdvDisplayService displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
		displayService.setDisplayMetadata( bdvHandle, Projector.PROJECTOR, projector );
	}

	private BdvOptions createBdvOptions()
	{
		BdvOptions bdvOptions = BdvOptions.options().frameTitle( windowTitle );
		if ( is2D ) bdvOptions = bdvOptions.is2D();
		bdvOptions.accumulateProjectorFactory( new ProjectorFactory( projector ).get() );
		return bdvOptions;
	}

	private static BdvHandle createBdv( BdvOptions bdvOptions, boolean interpolate, int numTimePoints )
	{
		// create dummy image to instantiate the BDV
		ArrayImg< ByteType, ByteArray > dummyImg = ArrayImgs.bytes(2, 2, 2);
		bdvOptions = bdvOptions.sourceTransform( new AffineTransform3D() );
		BdvStackSource<ByteType> bss = BdvFunctions.show( dummyImg, "dummy", bdvOptions );
		BdvHandle bdv = bss.getBdvHandle();

		if ( interpolate ) bdv.getViewerPanel().setInterpolation( Interpolation.NLINEAR );

		// remove dummy image
		bdv.getViewerPanel().state().removeSource(bdv.getViewerPanel().state().getCurrentSource());

		bdv.getViewerPanel().setNumTimepoints( numTimePoints );

		return bdv;
	}
}
