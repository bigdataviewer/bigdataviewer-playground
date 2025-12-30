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
package sc.fiji.bdvpg.demos.bdv;

import bdv.util.BdvHandle;
import org.scijava.Context;
import org.scijava.ui.UIService;
import org.scijava.ui.swing.SwingUI;
import sc.fiji.bdvpg.DemoHelper;
import sc.fiji.bdvpg.scijava.BdvScijavaHelper;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

/**
 * Demos a way to create a Bdv window which will be registered by the {@link SourceAndConverterBdvDisplayService}
 * It is thus discoverable by all the other components of BigDataViewer-playground, in particular
 * as scijava parameters typed {@link bdv.util.BdvHandle} or {@link bdv.util.BdvHandle[]} in
 * {@link org.scijava.command.Command}.
 */
public class BdvCreatorDemo {
	public static void main( String[] args ) {
		// Create the application context with all available services a runtime
		final Context ctx = new Context();

		// Show UI
		ctx.service(UIService.class).showUI(SwingUI.NAME);

		// Creates a new BDV
		ctx.getService(SourceAndConverterBdvDisplayService.class).getNewBdv();

		// Returns the previous BDV or the one "on top", meaning the one clicked by the user
		BdvHandle bdvh = ctx.getService(SourceAndConverterBdvDisplayService.class).getActiveBdv();

		BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"File>SubMenu1>SubMenu2>Say Hello 1!",0, () -> bdvh.getViewerPanel().showMessage("Hello!"));
		BdvScijavaHelper.addSeparator(bdvh,"File>SubMenu1");

		BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"File>SubMenu1>SubMenu3>Say Hello 2!",0, () -> bdvh.getViewerPanel().showMessage("Hello!"));
		BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Greetings>Menu1>Menu2>Say Hello 3!",0, () -> bdvh.getViewerPanel().showMessage("Hello!"));
		BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Greetings>Menu1>Menu2>Say Hello 4!",0, () -> bdvh.getViewerPanel().showMessage("Hello!"));

		// Capture screenshots of all visible windows for documentation
		DemoHelper.shot("BdvCreatorDemo");
	}

}
