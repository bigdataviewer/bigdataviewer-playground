/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2024 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.time.Duration;
import java.time.Instant;

/**
 * Test of opening multiple datasets
 *
 * The limiting performance factor is the construction of the tree UI which scales
 * very badly. For now, its speed it user-ok for a number of sources below ~600
 *
 * 300 datasets ~ 3 secs
 *
 * 3000 sources takes ~ 120 secs
 *
 * TODO : fix performance issue if several thousands of sources are necessary (could be coming sooner than expected)
 *
 */

public class PerfOpenMultipleSpimDataTest
{
	static ImageJ ij;

	public static void main( String[] args )
	{
		// Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
		ij = new ImageJ();
		TestHelper.startFiji(ij);//ij.ui().showUI();

		// Gets an active BdvHandle instance
		// SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

		tic();
		for (int i=0;i<100;i++) {
			// Import SpimData
			new SpimDataFromXmlImporter( "src/test/resources/mri-stack.xml" ).run();
			new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();
			new SpimDataFromXmlImporter( "src/test/resources/mri-stack-shiftedY.xml" ).run();
			System.out.println(i);
		}
		toc();
	}

	static Instant start;

	public static void tic() {
		start = Instant.now();
	}

	static long timeElapsedInS;

	public static void toc() {
		Instant finish = Instant.now();
		timeElapsedInS = Duration.between(start, finish).toMillis()/1000;

		System.out.println("It took "+timeElapsedInS+" s to open 300 datasets");
	}

	@Test // Takes too much memory when all tests run at the same time
	public void demoRunOk() {
		main(new String[]{""});
		Assert.assertTrue(timeElapsedInS<10);
	}

	@After
	public void closeFiji() {
		TestHelper.closeFijiAndBdvs(ij);
	}

}
