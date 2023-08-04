package sc.fiji.bdvpg.scijava.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.imglib2.util.Pair;
import org.scijava.Context;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.services.IViewerService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class ViewerService<T> extends AbstractService
        implements SciJavaService, IViewerService<T> {

    @Parameter
    ObjectService os;

    /**
     * Used to add Aliases for BvvHandle objects
     **/
    @Parameter
    ScriptService scriptService;


    /**
     * Used to retrieve the last active BDV Windows (if the activated callback has
     * been set right)
     **/
    @Parameter
    GuavaWeakCacheService cacheService;

    @Parameter
    Context ctx;

    /**
     * Service containing all registered BDV Sources
     **/
    @Parameter
    SourceAndConverterService sourceAndConverterService;

    protected static final Logger logger = LoggerFactory.getLogger(
            ViewerService.class);
    @Override
    public void initialize() {
        scriptService.addAlias(getViewerType());
        displayToMetadata = CacheBuilder.newBuilder().weakKeys().build();// new
        // HashMap<>();
        sourceAndConverterService.addViewerService(this);
        // Catching bdv supplier from Prefs
        logger.debug("Bdv Playground Viewer Service initialized ("+getViewerType().getSimpleName()+")");
    }


    /**
     * Enables proper closing of Big Warp paired BvvHandles
     */
    final List<Pair<T, T>> pairedViewers = new ArrayList<>();

    @Override
    public void bindClosing(T handle1, T handle2) {
        pairedViewers.add(new Pair<T, T>() {

            @Override
            public T getA() {
                return handle1;
            }

            @Override
            public T getB() {
                return handle2;
            }
        });
    }

    @Override
    public List<T> getViewers() {
        return os.getObjects(getViewerType());
    }

    /**
     * Map containing objects that are 1 to 1 linked to a Display ( a BvvHandle
     * object ) Keys are Weakly referenced -> Metadata should be GCed if
     * referenced only here
     */
    Cache<T, Map<String, Object>> displayToMetadata;

    @Override
    public void setViewerMetadata(T viewer, String key, Object data) {
        if (viewer == null) {
            logger.error("Error : viewer is null in setMetadata function! ");
            return;
        }
        if (displayToMetadata.getIfPresent(viewer) == null) {
            // Create Metadata
            displayToMetadata.put(viewer, new HashMap<>());
        }
        displayToMetadata.getIfPresent(viewer).put(key, data);
    }

    @Override
    public Object getViewerMetadata(T viewer, String key) {
        if (displayToMetadata.getIfPresent(viewer) != null) {
            return displayToMetadata.getIfPresent(viewer).get(key);
        }
        else {
            return null;
        }
    }

}
