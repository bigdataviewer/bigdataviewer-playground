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
package sc.fiji.bdvpg.io;

import bdv.util.BdvHandle;
import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

/**
 * Demonstrates visualisation of two spimData sources.
 *
 */
public class SpimDataDisplayDemo
{

	static ImageJ ij;

	public static void main( String[] args )
	{
		// Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
		ij = new ImageJ();
		TestHelper.startFiji(ij);//ij.ui().showUI();

		// Gets active BdvHandle instance
		BdvHandle bdvHandle = SourceAndConverterServices.getBdvDisplayService().getActiveBdv();

		// Import SpimData
		new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").run();
		new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();

		// Show all SourceAndConverter associated with above SpimData
		SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters().forEach( sac -> {
			SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sac);
			new ViewerTransformAdjuster(bdvHandle, sac).run();
			new BrightnessAutoAdjuster<>(sac, 0).run();
		});
	}

	@Test
	public void demoRunOk() {
		main(new String[]{""});
	}

	@After
	public void closeFiji() {
		TestHelper.closeFijiAndBdvs(ij);
	}
}
