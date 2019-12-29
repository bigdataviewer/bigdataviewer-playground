package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.*;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;
import sc.fiji.bdvpg.scijava.services.GuavaWeakCacheService;


@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Create Empty BDV Frame",
    label = "Creates an empty Bdv window")
public class BdvWindowCreatorCommand implements Command {

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

    @Parameter
    BdvSourceDisplayService bdvsds;

    @Override
    public void run() {
        //------------ BdvHandleFrame
        BdvCreator creator = new BdvCreator(is2D, windowTitle);
        creator.run();
        bdvh = creator.getBdvHandle();
        //------------ Allows to remove the BdvHandle from the objectService when closed by the user
        BdvHandleHelper.setBdvHandleCloseOperation(bdvh, cacheService,  bdvsds, true);
        //------------ Renames window to ensure unicity
        windowTitle = BdvHandleHelper.getUniqueWindowTitle(os, windowTitle);
        BdvHandleHelper.setWindowTitle(bdvh, windowTitle);
        os.addObject(bdvh);
    }
}
