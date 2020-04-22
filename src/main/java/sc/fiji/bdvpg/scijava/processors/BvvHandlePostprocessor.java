package sc.fiji.bdvpg.scijava.processors;

import bdv.util.BdvHandle;
import bvv.util.BvvHandle;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.BvvHandleHelper;
import sc.fiji.bdvpg.scijava.services.GuavaWeakCacheService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

import java.util.function.Consumer;

/**
 * Ensures BdvHandle is stored into ObjectService
 * and all containing Sources as well are stored into the BdvSourceAndConverterDisplayService and
 * BdvSourceAndConverterService
 * Also fix Bdv Close operation
 */

@Plugin(type = PostprocessorPlugin.class)
public class BvvHandlePostprocessor extends AbstractPostprocessorPlugin {

    @Parameter
    SourceAndConverterBdvDisplayService bsds;

    @Parameter
    ObjectService os;

    @Parameter
    GuavaWeakCacheService cacheService;

    public static Consumer<String> log = (str) -> System.out.println(BvvHandlePostprocessor.class.getSimpleName()+":"+str);

    @Override
    public void process(Module module) {

        module.getOutputs().forEach((name, object)-> {
            if (object instanceof BvvHandle) {
                BvvHandle bvvh = (BvvHandle) object;
                log.accept("BdvHandle found.");
                //------------ Register BdvHandle in ObjectService
                os.addObject(bvvh);
                //------------ Allows to remove the BdvHandle from the objectService when closed by the user
                BvvHandleHelper.setBvvHandleCloseOperation(bvvh, cacheService,  os, bsds, true);
                //------------ Renames window to ensure unicity
                String windowTitle = BvvHandleHelper.getWindowTitle(bvvh);
                windowTitle = BvvHandleHelper.getUniqueWindowTitle(os, windowTitle);
                BvvHandleHelper.setWindowTitle(bvvh, windowTitle);
                //for (int i=0;i<bdvh.getViewerPanel().getState().numSources();i++) {
                //    bsds.registerBdvSource(bdvh,i);
                //}
                module.resolveOutput(name);
            }
        });

    }

}
