package sc.fiji.bdvpg;

import loci.common.DebugTools;
import net.imagej.ImageJ;
import org.junit.Test;
import org.scijava.command.DefaultCommandService;
import org.scijava.plugin.PTService;
import org.scijava.plugin.PluginService;
import sc.fiji.bdvpg.services.serializers.plugins.*;

public class SimpleIJLaunch {

    static public void main(String... args) {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        DebugTools.setRootLevel("INFO");
    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }

}
