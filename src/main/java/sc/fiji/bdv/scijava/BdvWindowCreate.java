package sc.fiji.bdv.scijava;

import bdv.util.*;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.realtransform.AffineTransform3D;
import net.miginfocom.swing.MigLayout;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Create Empty BDV Frame",
    label = "Creates an empty Bdv window")
public class BdvWindowCreate implements Command {

    @Parameter(label = "Create a 2D Bdv window")
    public boolean is2D = false;

    @Parameter(label = "Title of the new Bdv window")
    public String windowTitle = "Bdv";

    @Parameter(type = ItemIO.OUTPUT)
    public BdvHandle bdvh;

    @Parameter(label = "Location of the view of the new Bdv window")
    public double px = 0, py = 0, pz = 0;

    @Parameter(label = "Field of view size of the new Bdv window")
    public double s = 100;

    @Parameter
    GuavaWeakCacheService cacheService;

    @Parameter
    ObjectService os;

    //@Parameter(choices = {"BdvHandleFrame","BdvHandlePanel"})
    String type = "BdvHandleFrame";

    @Override
    public void run() {
        BdvOptions opts = BdvOptions.options();
        if (is2D) {
            opts = opts.is2D();
        }

        //------------ BdvHandleFrame
        if (type.equals("BdvHandleFrame")) {
            ArrayImg dummyImg = ArrayImgs.bytes(2, 2, 2);
            BdvStackSource bss = BdvFunctions.show(dummyImg, "dummy", opts.frameTitle(windowTitle).sourceTransform(new AffineTransform3D()));
            bdvh = bss.getBdvHandle();
            AffineTransform3D at3D = new AffineTransform3D();
            at3D.translate(-px, -py, -pz);
            double scale = bdvh.getViewerPanel().getWidth() / s;
            at3D.scale(scale, scale, 1);
            bdvh.getViewerPanel().setCurrentViewerTransform(at3D);
            bdvh.getViewerPanel().requestRepaint();
            bss.removeFromBdv();
        }


        //------------ BdvHandlePanel

        if (type.equals("BdvHandlePanel")) {
            JFrame frame = new JFrame(windowTitle);
            JPanel viewer = new JPanel(new MigLayout());
            bdvh = new BdvHandlePanel(frame, opts);
            viewer.add( bdvh.getViewerPanel(), "span, grow, push" );

            // Hack for 3d
            ArrayImg dummyImg = ArrayImgs.bytes(2, 2, 2);
            BdvStackSource bss = BdvFunctions.show(dummyImg, "dummy", opts.frameTitle(windowTitle).sourceTransform(new AffineTransform3D()).addTo(bdvh));
            //bdvh = bss.getBdvHandle();
            AffineTransform3D at3D = new AffineTransform3D();
            at3D.translate(-px, -py, -pz);
            //double scale = bdvh.getViewerPanel().getWidth() / s;
            //at3D.scale(scale, scale, 1);
            //bdvh.getViewerPanel().setCurrentViewerTransform(at3D);
            //bdvh.getViewerPanel().requestRepaint();
            //bss.removeFromBdv();
            //

            frame.setMinimumSize(new Dimension(500,500));
            frame.setContentPane(viewer);
            frame.pack();
            frame.setVisible(true);

            double scale = bdvh.getViewerPanel().getWidth() / s;
            at3D.scale(scale, scale, 1);
            bdvh.getViewerPanel().setCurrentViewerTransform(at3D);
            bdvh.getViewerPanel().requestRepaint();
            bss.removeFromBdv();
        }

        BdvHandleHelper.setBdvHandleCloseOperation(bdvh,os,cacheService, true);
        windowTitle = BdvHandleHelper.getUniqueWindowTitle(os, windowTitle);
        BdvHandleHelper.setWindowTitle(bdvh, windowTitle);
    }
}
