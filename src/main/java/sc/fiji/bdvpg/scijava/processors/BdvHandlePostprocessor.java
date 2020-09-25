package sc.fiji.bdvpg.scijava.processors;

import bdv.util.BdvHandle;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.GuavaWeakCacheService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.BdvHandleFilterNode;
import sc.fiji.bdvpg.scijava.services.ui.SourceFilterNode;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Ensures BdvHandle is stored into ObjectService
 * and all containing Sources as well are stored into the BdvSourceAndConverterDisplayService and
 * BdvSourceAndConverterService
 * Also fix BDV Close operation
 */

@Plugin(type = PostprocessorPlugin.class)
public class BdvHandlePostprocessor extends AbstractPostprocessorPlugin {

    @Parameter
    SourceAndConverterBdvDisplayService bsds;

    @Parameter
    SourceAndConverterService sacsService;

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
                if (!os.getObjects(BdvHandle.class).contains(bdvh)) { // adds it only if not already present in ObjectService
                    os.addObject(bdvh);

                    //------------ Renames window to ensure unicity
                    String windowTitle = BdvHandleHelper.getWindowTitle(bdvh);
                    windowTitle = BdvHandleHelper.getUniqueWindowTitle(os, windowTitle);
                    BdvHandleHelper.setWindowTitle(bdvh, windowTitle);

                    //------------ Event handling in bdv sourceandconverterserviceui
                    BdvHandleFilterNode node = new BdvHandleFilterNode(windowTitle, bdvh);

                    //------------ Allows to remove the BdvHandle from the objectService when closed by the user
                    BdvHandleHelper.setBdvHandleCloseOperation(bdvh, cacheService,  bsds, true,
                            () -> {
                                //bdvh.getViewerPanel().state().changeListeners().remove(vscl); // TODO : check no memory leak
                                sacsService.getUI().removeBdvHandleNodes(bdvh);
                            });

                    ((SourceFilterNode)sacsService.getUI().getTreeModel().getRoot()).insert(node,0);
                    SwingUtilities.invokeLater(()->sacsService.getUI().getTreeModel().reload());
                }

                module.resolveOutput(name);
            }
        });

    }

}
