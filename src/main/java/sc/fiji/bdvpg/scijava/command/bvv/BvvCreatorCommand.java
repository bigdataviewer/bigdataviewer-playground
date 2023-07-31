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

package sc.fiji.bdvpg.scijava.command.bvv;

import bvv.vistools.BvvHandle;
import bvv.vistools.BvvOptions;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bvv.BvvCreator;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu + "Viewer>BVV - Create Empty BVV Frame",
	description = "Creates an empty Bvv window")
public class BvvCreatorCommand implements BdvPlaygroundActionCommand {

	/**
	 * Title of the new BVV window
	 */
	@Parameter(label = "Title of the new BVV window")
	public String windowtitle = "BVV";

	/**
	 * TODO This triggers: BvvHandlePostprocessor
	 */
	@Parameter(type = ItemIO.OUTPUT)
	public BvvHandle bvvh;

	@Override
	public void run() {
		// ------------ BdvHandleFrame
		BvvOptions opts = BvvOptions.options().frameTitle(windowtitle);

		BvvCreator creator = new BvvCreator(opts, 1);
		creator.run();
		bvvh = creator.get();
	}
}