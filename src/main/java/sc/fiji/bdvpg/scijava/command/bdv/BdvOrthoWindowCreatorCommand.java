/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import bdv.util.*;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.viewers.ViewerAdapter;
import sc.fiji.bdvpg.viewers.ViewerOrthoSyncStarter;

import javax.swing.*;
import java.awt.*;

@Plugin(type = BdvPlaygroundActionCommand.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Create Orthogonal Views",
        description = "Creates 3 BDV windows with synchronized orthogonal views")
public class BdvOrthoWindowCreatorCommand implements BdvPlaygroundActionCommand {

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

    //@Parameter(label = "Synchronize time") // honestly no reason not to synchronize the time
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
    SourceAndConverterBdvDisplayService sacDisplayService;

    @Override
    public void run() {

        bdvhx = createBdv("-Front", locationx, locationy);
        bdvhy = createBdv("-Right", locationx + sizex +10, locationy);
        bdvhz = createBdv("-Bottom", locationx, locationy + sizey +40);

        new ViewerOrthoSyncStarter(new ViewerAdapter(bdvhx), new ViewerAdapter(bdvhz), new ViewerAdapter(bdvhy), synctime).run();

        if (drawcrosses) {
            BdvHandleHelper.addCenterCross(bdvhx);
            BdvHandleHelper.addCenterCross(bdvhy);
            BdvHandleHelper.addCenterCross(bdvhz);
        }

        bdvhx.getViewerPanel().state().setNumTimepoints(ntimepoints);
        bdvhy.getViewerPanel().state().setNumTimepoints(ntimepoints);
        bdvhz.getViewerPanel().state().setNumTimepoints(ntimepoints);

    }

    BdvHandle createBdv(String suffix, double locX, double locY) {

        BdvHandle bdvh = sacDisplayService.getNewBdv();
        BdvHandleHelper.setWindowTitle(bdvh, BdvHandleHelper.getWindowTitle(bdvh)+suffix);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        JFrame frame = BdvHandleHelper.getJFrame(bdvh);
        SwingUtilities.invokeLater(() -> {
            if( screen > -1 && screen < gd.length ) {
                frame.setLocation(gd[screen].getDefaultConfiguration().getBounds().x+(int)locX, (int)locY);
            } else if( gd.length > 0 ) {
                frame.setLocation(gd[0].getDefaultConfiguration().getBounds().x+(int)locX, (int)locY);
            } else {
                throw new RuntimeException( "No Screens Found" );
            }
            frame.setSize(sizex, sizey);
        });

        return bdvh;
    }

}
