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
package sc.fiji.bdvpg.demos;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.sourceandconverter.SourceAdder;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.importer.VoronoiSourceGetter;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;


/**
 *
 * Demo of opening two sources, adjusting the location of the Bdv Window + adjusting brightness and contrast
 *
 * TODO : solves the brightness adjuster which does not work with the voronoi source, see {@link BrightnessAutoAdjuster}
 *
 */

public class BrightnessAutoAdjusterDemo
{

    static BdvHandle bdvHandle;

	static ImageJ ij;

	public static void main( String[] args ) {
        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ij = new ImageJ();
		TestHelper.startFiji(ij);//ij.ui().showUI();

		// Creates a BdvHandle
		bdvHandle = ij.get(SourceAndConverterBdvDisplayService.class).getActiveBdv();

        AbstractSpimData<?> asd = new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").get();

        List<SourceAndConverter<?>> sourcesFromSpimData =
				ij.get(SourceAndConverterService.class)
                .getSourceAndConverterFromSpimdata(asd);

        addSource(bdvHandle, sourcesFromSpimData.get(0));

		// Voronoi
		final SourceAndConverter<?> voronoiSource = new VoronoiSourceGetter( new long[]{ 512, 512, 1 }, 256, true ).get();
		addSource( bdvHandle, voronoiSource );

	}

	public static void addSource(BdvHandle bdvHandle, SourceAndConverter<?> sac )
	{
		new SourceAdder( bdvHandle, sac ).run();
		new ViewerTransformAdjuster( bdvHandle, sac ).run();
		new BrightnessAutoAdjuster<>( sac,0 ).run();
	}

}
