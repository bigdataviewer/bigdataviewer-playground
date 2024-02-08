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

package sc.fiji.bdvpg.scijava.command.bvv;

import bvv.vistools.BvvHandle;
import bvv.vistools.BvvOptions;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bvv.BvvCreator;
import sc.fiji.bdvpg.bvv.BvvHandleHelper;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.viewers.ViewerAdapter;
import sc.fiji.bdvpg.viewers.ViewerOrthoSyncStarter;
import sc.fiji.bdvpg.viewers.ViewerStateSyncStarter;

import javax.swing.JFrame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu + "BVV>BVV - Create Orthogonal Views",
	description = "Creates 3 BVV windows with synchronized orthogonal views")
public class BvvOrthoWindowCreatorCommand implements
	BdvPlaygroundActionCommand
{

	@Parameter(label = "Interpolate")
	public boolean interpolate = false;

	@Parameter(label = "Number of timepoints (1 for a single timepoint)")
	public int ntimepoints = 1;

	@Parameter(label = "Display (0 if you have one screen)")
	int screen = 0;

	@Parameter(label = "X Front Window location")
	int locationx = 150;

	@Parameter(label = "Y Front Window location")
	int locationy = 150;

	@Parameter(label = "Window Width")
	int sizex = 500;

	@Parameter(label = "Window Height")
	int sizey = 500;

	// @Parameter(label = "Synchronize time") // honestly no reason not to
	// synchronize the time
	public boolean synctime = true;

	@Parameter
	boolean synchronize_sources = true;

	/**
	 * This triggers: BdvHandlePostprocessor
	 */
	@Parameter(type = ItemIO.OUTPUT)
	public BvvHandle bvvhx;

	@Parameter(type = ItemIO.OUTPUT)
	public BvvHandle bvvhy;

	@Parameter(type = ItemIO.OUTPUT)
	public BvvHandle bvvhz;

	@Override
	public void run() {

		bvvhx = createBvv("-Front", locationx, locationy);
		bvvhx.getViewerPanel().state().setNumTimepoints(ntimepoints);

		bvvhy = createBvv("-Right", locationx + sizex + 10, locationy);
		bvvhy.getViewerPanel().state().setNumTimepoints(ntimepoints);

		bvvhz = createBvv("-Bottom", locationx, locationy + sizey + 40);
		bvvhz.getViewerPanel().state().setNumTimepoints(ntimepoints);

		new ViewerOrthoSyncStarter(new ViewerAdapter(bvvhx), new ViewerAdapter(
			bvvhz), new ViewerAdapter(bvvhy), synctime).run();

		if (synchronize_sources) {
			new ViewerStateSyncStarter(new ViewerAdapter(bvvhx), new ViewerAdapter(
				bvvhy), new ViewerAdapter(bvvhz)).run();
		}

	}

	BvvHandle createBvv(String suffix, double locX, double locY) {
		BvvOptions opts = BvvOptions.options();// .frameTitle("BVV-"+suffix);
		BvvHandle bvvh = new BvvCreator(opts, ntimepoints).get();// sacDisplayService.getNewBdv();
		BvvHandleHelper.setWindowTitle(bvvh, BvvHandleHelper.getWindowTitle(bvvh) +
			suffix);

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gd = ge.getScreenDevices();
		JFrame frame = BvvHandleHelper.getJFrame(bvvh);
		if (screen > -1 && screen < gd.length) {
			frame.setLocation(gd[screen].getDefaultConfiguration().getBounds().x +
				(int) locX, (int) locY);
		}
		else if (gd.length > 0) {
			frame.setLocation(gd[0].getDefaultConfiguration().getBounds().x +
				(int) locX, (int) locY);
		}
		else {
			throw new RuntimeException("No Screens Found");
		}

		return bvvh;
	}

}
