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

package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.BdvService;
import sc.fiji.bdvpg.viewer.ViewerHelper;
import sc.fiji.bdvpg.viewer.ViewerOrthoSyncStarter;
import sc.fiji.bdvpg.viewer.ViewerStateSyncStarter;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu + "Viewer>BDV - Create Orthogonal Views",
	description = "Creates 3 BDV windows with synchronized orthogonal views")
public class BdvOrthoCreatorCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Interpolate")
	public boolean interpolate = false;

	@Parameter(label = "Number of timepoints (1 for a single timepoint)")
	public int ntimepoints = 1;

	@Parameter(label = "Add cross overlay to show view plane locations")
	public boolean drawcrosses = true;

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

	/**
	 * This triggers: BdvHandlePostprocessor
	 */
	@Parameter(type = ItemIO.OUTPUT)
	public BdvHandle bdvhx;

	@Parameter(type = ItemIO.OUTPUT)
	public BdvHandle bdvhy;

	@Parameter(type = ItemIO.OUTPUT)
	public BdvHandle bdvhz;

	@Parameter
	BdvService sacDisplayService;

	@Parameter
	boolean synchronize_sources = true;

	@Override
	public void run() {

		bdvhx = createBdv("-Front", locationx, locationy);
		bdvhy = createBdv("-Right", locationx + sizex + 10, locationy);
		bdvhz = createBdv("-Bottom", locationx, locationy + sizey + 40);

		if (drawcrosses) {
			BdvHandleHelper.addCenterCross(bdvhx);
			BdvHandleHelper.addCenterCross(bdvhy);
			BdvHandleHelper.addCenterCross(bdvhz);
		}

		bdvhx.getViewerPanel().state().setNumTimepoints(ntimepoints);
		bdvhy.getViewerPanel().state().setNumTimepoints(ntimepoints);
		bdvhz.getViewerPanel().state().setNumTimepoints(ntimepoints);

		ViewerOrthoSyncStarter starter = new ViewerOrthoSyncStarter(
			bdvhx.getViewerPanel(), bdvhy.getViewerPanel(), bdvhz.getViewerPanel(), synctime);
		starter.run();

		if (synchronize_sources) {
			new ViewerStateSyncStarter(bdvhx.getViewerPanel(),
				bdvhy.getViewerPanel(), bdvhz.getViewerPanel()).run();
		}

	}

	BdvHandle createBdv(String suffix, double locX, double locY) {

		BdvHandle bdvh = sacDisplayService.getNewViewer();
		ViewerHelper.setViewerTitle(bdvh.getViewerPanel(), ViewerHelper.getViewerTitle(bdvh.getViewerPanel()) +
			suffix);

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gd = ge.getScreenDevices();
		JFrame frame = ViewerHelper.getJFrame(bdvh.getViewerPanel());
		SwingUtilities.invokeLater(() -> {
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
			frame.setSize(sizex, sizey);
		});

		return bdvh;
	}

}
