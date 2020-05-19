package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.*;
import bdv.viewer.render.AccumulateProjectorFactory;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.navigate.ViewerOrthoSyncStarter;
import sc.fiji.bdvpg.bdv.projector.AccumulateAverageProjectorARGB;
import sc.fiji.bdvpg.bdv.projector.AccumulateMixedProjectorARGBFactory;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

import javax.swing.*;
import java.awt.*;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Create Ortho BDV Frames",
        label = "Creates 3 Bdvs windows with synchronized orthogonal views")
public class BdvOrthoWindowCreatorCommand implements Command {

    @Parameter(label = "Title of Bdv windows")
    public String windowTitle = "Bdv";

    @Parameter(label = "Interpolate")
    public boolean interpolate = false;

    @Parameter(label = "Number of timepoints (1 for a single timepoint)")
    public int nTimepoints = 1;

    @Parameter(label = "Synchronize time")
    public boolean syncTime;

    /**
     * This triggers: BdvHandlePostprocessor
     */
    @Parameter(type = ItemIO.OUTPUT)
    public BdvHandle bdvhX;

    @Parameter(type = ItemIO.OUTPUT)
    public BdvHandle bdvhY;

    @Parameter(type = ItemIO.OUTPUT)
    public BdvHandle bdvhZ;

    @Parameter(label = "Source blending", choices = { Projection.MIXED_PROJECTOR, Projection.SUM_PROJECTOR, Projection.AVERAGE_PROJECTOR})
    public String projector;

    @Parameter(label = "Add cross overlay")
    public boolean drawCrosses;

    @Parameter(label = "Display (0 if you have one screen)")
    int screen = 0;

    @Parameter(label = "X Front Window location")
    int locationX = 150;

    @Parameter(label = "Y Front Window location")
    int locationY = 150;

    @Parameter(label = "Window Width")
    int sizeX = 500;

    @Parameter(label = "Window Height")
    int sizeY = 500;

    @Override
    public void run() {

        bdvhX = createBdv("-Front", locationX, locationY);

        bdvhY = createBdv("-Right", locationX+sizeX+10, locationY);

        bdvhZ = createBdv("-Bottom", locationX, locationY+sizeY+40);

        new ViewerOrthoSyncStarter(bdvhX, bdvhZ, bdvhY, syncTime).run();

       if (drawCrosses) {
           addCross(bdvhX);
           addCross(bdvhY);
           addCross(bdvhZ);
       }
    }

    BdvHandle createBdv(String suffix, double locX, double locY) {

        //------------ BdvHandleFrame
        BdvOptions opts = BdvOptions.options().frameTitle(windowTitle+suffix).preferredSize(sizeX,sizeY);

        // Create accumulate projector factory
        AccumulateProjectorFactory<ARGBType> factory = null;
        switch (projector) {
            case Projection.MIXED_PROJECTOR:
                factory = new AccumulateMixedProjectorARGBFactory(  );
                opts = opts.accumulateProjectorFactory(factory);
            case Projection.SUM_PROJECTOR:
                // Default projector
                break;
            case Projection.AVERAGE_PROJECTOR:
                factory = AccumulateAverageProjectorARGB.factory;
                opts = opts.accumulateProjectorFactory(factory);
                break;
            default:
        }

        BdvCreator creator = new BdvCreator(opts, interpolate, nTimepoints);
        creator.run();
        BdvHandle bdvh = creator.get();

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        JFrame frame = BdvHandleHelper.getJFrame(bdvh);
        if( screen > -1 && screen < gd.length ) {
            frame.setLocation(gd[screen].getDefaultConfiguration().getBounds().x+(int)locX, (int)locY);
        } else if( gd.length > 0 ) {
            frame.setLocation(gd[0].getDefaultConfiguration().getBounds().x+(int)locX, (int)locY);
        } else {
            throw new RuntimeException( "No Screens Found" );
        }

        return bdvh;
    }

    void addCross(BdvHandle bdvh) {
        final BdvOverlay overlay = new BdvOverlay()
        {
            @Override
            protected void draw( final Graphics2D g )
            {
                int colorCode = this.info.getColor().get();
                int w = bdvh.getViewerPanel().getWidth();
                int h = bdvh.getViewerPanel().getHeight();
                g.setColor(new Color(ARGBType.red(colorCode) , ARGBType.green(colorCode), ARGBType.blue(colorCode), ARGBType.alpha(colorCode) ));
                g.drawLine(w/2, h/2-h/4,w/2, h/2+h/4 );
                g.drawLine(w/2-w/4, h/2,w/2+w/4, h/2 );
            }

        };

        BdvFunctions.showOverlay( overlay, "cross_overlay", BdvOptions.options().addTo( bdvh ) );
        bdvh.getViewerPanel().setTimepoint(nTimepoints);
    }

}
