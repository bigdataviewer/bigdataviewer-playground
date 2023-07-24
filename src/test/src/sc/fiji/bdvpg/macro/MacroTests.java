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
package sc.fiji.bdvpg.macro;

import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.concurrent.ExecutionException;

public class MacroTests
{

	ImageJ ij;

	@Before
	public void startFiji( )
	{
		// Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
		ij = new ImageJ();
		TestHelper.startFiji(ij);//ij.ui().showUI();
	}

	@Test
	public void createBdv() {
		try {
			ij.script().run("dummy.ijm",
					"run(\"BDV - Create empty BDV window\");", true).get();
			Assert.assertEquals(
			SourceAndConverterServices
					.getBdvDisplayService()
					.getViewers().size(),1);
		} catch (InterruptedException e) {
			System.out.println("INterrupt");
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			System.out.println("ExeExcep");
			throw new RuntimeException(e);
		}
	}
	@After
	public void closeFiji() {
		TestHelper.closeFijiAndBdvs(ij);
	}

	public static void main(String... args) {
		ImageJ ij = new ImageJ();
		TestHelper.startFiji(ij);//ij.ui().showUI();
	}
}
