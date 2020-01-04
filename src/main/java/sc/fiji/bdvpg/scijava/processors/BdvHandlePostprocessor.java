package sc.fiji.bdvpg.scijava.processors;

import bdv.util.BdvHandle;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService;
import sc.fiji.bdvpg.scijava.services.BdvSourceService;
import sc.fiji.bdvpg.scijava.services.GuavaWeakCacheService;

import java.util.function.Consumer;

/**
 * Ensures BdvHandle is stored into ObjectService
 * and all containing Sources as well are stored into the BdvSourceDisplayService and
 * BdvSourceService
 * Also fix Bdv Close operation
 */

@Plugin(type = PostprocessorPlugin.class)
public class BdvHandlePostprocessor extends AbstractPostprocessorPlugin {

    @Parameter
    BdvSourceDisplayService bsds;

    @Parameter
    ObjectService os;

    @Parameter
    GuavaWeakCacheService cacheService;

    public static Consumer<String> log = (str) -> System.out.println(BdvHandlePostprocessor.class.getSimpleName()+":"+str);

    @Override
    public void process(Module module) {

        module.getOutputs().forEach((name, object)-> {
            if (object instanceof BdvHandle) {
                BdvHandle bdvh = (BdvHandle) object;
                log.accept("BdvHandle found.");
                //------------ Register BdvHandle in ObjectService
                os.addObject(bdvh);
                //------------ Allows to remove the BdvHandle from the objectService when closed by the user
                BdvHandleHelper.setBdvHandleCloseOperation(bdvh, cacheService,  bsds, true);
                //------------ Renames window to ensure unicity
                String windowTitle = BdvHandleHelper.getWindowTitle(bdvh);
                windowTitle = BdvHandleHelper.getUniqueWindowTitle(os, windowTitle);
                BdvHandleHelper.setWindowTitle(bdvh, windowTitle);
                for (int i=0;i<bdvh.getViewerPanel().getState().numSources();i++) {
                    bsds.registerBdvSource(bdvh,i);
                }
                module.resolveOutput(name);
            }
        });

    }

}
