package sc.fiji.bdvpg.scijava.services;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.converter.Converter;
import net.imglib2.util.Pair;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.options.OptionsService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;
import sc.fiji.bdvpg.scijava.services.ui.SourceFilterNode;
import sc.fiji.bdvpg.scijava.services.ui.SpimDataFilterNode;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Scijava Service which handles the Display of BDV SourceAndConverters in one or multiple BDV Windows
 * Pairs with BdvSourceAndConverterService, but this service is optional
 *
 * Handling multiple Sources displayed in potentially multiple BDV Windows
 * Make its best to keep in synchronizations all of this, without creating errors nor memory leaks
 */

@Plugin(type= Service.class)
public class SourceAndConverterBdvDisplayService extends AbstractService implements SciJavaService  {

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println( SourceAndConverterBdvDisplayService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println( SourceAndConverterBdvDisplayService.class.getSimpleName()+":"+str);

    public static String CONVERTER_SETUP = "ConverterSetup";

    /**
     * Used to add Aliases for BdvHandle objects
     **/
    @Parameter
    ScriptService scriptService;

    /**
     * Service containing all registered BDV Sources
     **/
    @Parameter
    SourceAndConverterService bdvSourceAndConverterService;

    /**
     * Used to create BDV Windows when necessary
     **/
    @Parameter
    CommandService cs;

    /**
     * Used to retrieved the last active BDV Windows (if the activated callback has been set right)
     **/
    @Parameter
    GuavaWeakCacheService cacheService;

    @Parameter
    ObjectService os;

    public BdvHandle getNewBdv() {
        try
        {
            return (BdvHandle)
                    cs.run(BdvWindowCreatorCommand.class,
                            true,
                            "is2D", false,
                            "windowTitle", "Bdv",
                            "nTimepoints", 1,
                            "interpolate",false,
                            "projector", Projection.SUM_PROJECTOR).get().getOutput("bdvh");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the last active BDV or create a new one
     */
    public BdvHandle getActiveBdv() {
        List<BdvHandle> bdvhs = os.getObjects(BdvHandle.class);
        if ((bdvhs == null)||(bdvhs.size()==0)) {
            return getNewBdv();
        }

        if (bdvhs.size()==1) {
            return bdvhs.get(0);
        } else {
            // Get the one with the most recent focus ?
            Optional<BdvHandle> bdvh = bdvhs.stream().filter(b -> b.getViewerPanel().hasFocus()).findFirst();
            if (bdvh.isPresent()) {
                return bdvh.get();
            } else {
                if (cacheService.get("LAST_ACTIVE_BDVH")!=null) {
                    WeakReference<BdvHandle> wr_bdv_h = (WeakReference<BdvHandle>) cacheService.get("LAST_ACTIVE_BDVH");
                    return wr_bdv_h.get();
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Displays a Source, the last active bdvh is chosen since none is specified in this method
     * @param sacs
     */
    public void show(SourceAndConverter... sacs) {
         show(getActiveBdv(), sacs);
    }

    /**
     * Makes visible a source, makes it visible in all bdvs according to BdvhReferences
     * @param sac
     */
    public void makeVisible(SourceAndConverter sac) {
        getDisplaysOf(sac).forEach(bdvhr -> bdvhr.getViewerPanel().state().setSourceActive(sac, true));
    }

    /**
     * Makes visible a source, makes it visible in all BDVs according to BdvhReferences
     * @param sac
     */
    public boolean isVisible(SourceAndConverter sac, BdvHandle bdvh) {
        return bdvh.getViewerPanel().state().isSourceActive(sac);
    }

    /**
     * Makes invisible a source, makes it invisible in all BDVs according to BdvhReferences
     * @param sac
     */
    public void makeInvisible(SourceAndConverter sac) {
        getDisplaysOf(sac).forEach(bdvhr -> bdvhr.getViewerPanel().state().setSourceActive(sac, false));
    }

    /**
     * Displays a BDV sourceandconverter into the specified BdvHandle
     * This function really is the core of this service
     * It mimicks or copies the functions of BdvVisTools because it is responsible to
     * create converter, volatiles, convertersetups and so on
     * @param sacs
     * @param bdvh
     */
    public void show(BdvHandle bdvh, SourceAndConverter... sacs) {

        List<SourceAndConverter<?>> sacsToDisplay = new ArrayList<>();

        for (SourceAndConverter sac:sacs) {
            if (!bdvSourceAndConverterService.isRegistered(sac)) {
                bdvSourceAndConverterService.register(sac);
            }

            boolean escape = false;

            if (bdvh.getViewerPanel().state().getSources().contains(sac)) {
                escape = true;
            }

            // Do not display 2 times the same source and converter
            if (sacsToDisplay.contains(sac)) {
                escape = true;
            }

            if (!escape) {
                sacsToDisplay.add(sac);
                bdvh.getConverterSetups().put(sac,getConverterSetup(sac));
            }
        }

        // Actually display the sources -> repaint called only once!
        bdvh.getViewerPanel().state().addSources(sacsToDisplay);
        // And make them active
        bdvh.getViewerPanel().state().setSourcesActive(sacsToDisplay, true);
    }

    /**
     * Removes a sourceandconverter from all BdvHandle displaying this sourceandconverter
     * Updates all references of other Sources present
     * @param sacs
     */
    public void removeFromAllBdvs(SourceAndConverter<?>... sacs) {
        getDisplaysOf(sacs).forEach(bdv -> bdv.getViewerPanel().state().removeSources(Arrays.asList(sacs)));
    }

    /**
     * Removes a sourceandconverter from the active Bdv
     * Updates all references of other Sources present
     * @param sacs
     */
    public void removeFromActiveBdv(SourceAndConverter... sacs) {
        // This condition avoids creating a window for nothing
        if (os.getObjects(BdvHandle.class).size()>0) {
            remove(getActiveBdv(), sacs);
        }
    }

    /**
     * Removes a sourceandconverter from a BdvHandle
     * Updates all references of other Sources present
     * @param bdvh
     * @param sacs Array of SourceAndConverter
     */
    public void remove(BdvHandle bdvh, SourceAndConverter<?>... sacs) {
        bdvh.getViewerPanel().state().removeSources(Arrays.asList(sacs));
        bdvh.getViewerPanel().requestRepaint();
    }

    /**
     * Gets or create the associated ConverterSetup of a Source
     * While several converters can be associated to a Source (volatile and non volatile),
     * only one ConverterSetup is associated to a Source
     * @param sac
     * @return
     */
    public ConverterSetup getConverterSetup(SourceAndConverter sac) {
        if (!bdvSourceAndConverterService.isRegistered(sac)) {
            bdvSourceAndConverterService.register(sac);
        }

        // If no ConverterSetup is built then build it
        if ( bdvSourceAndConverterService.sacToMetadata.getIfPresent(sac).get( CONVERTER_SETUP ) == null) {
            ConverterSetup setup = SourceAndConverterUtils.createConverterSetup(sac);
            bdvSourceAndConverterService.sacToMetadata.getIfPresent(sac).put( CONVERTER_SETUP,  setup );
        }

        return (ConverterSetup) bdvSourceAndConverterService.sacToMetadata.getIfPresent(sac).get( CONVERTER_SETUP );
    }

    /**
     * Updates converter and ConverterSetup of a Source, + updates display
     * TODO: This method currently modifies the order of the sources shown in the bdvh window
     * While this is not important for most bdvhandle, this could affect the functionality
     * of BigWarp
     * LIMITATION : Cannot use LUT for ARGBType -> TODO check type and send an error
     * @param source
     * @param cvt
     */
    public void updateConverter(SourceAndConverter source, Converter cvt) {
        errlog.accept("Unsupported operation : a new SourceAndConverterObject should be built. (TODO) ");
    }

    /**
     * Service initialization
     */
    @Override
    public void initialize() {
        scriptService.addAlias(BdvHandle.class);
        displayToMetadata = CacheBuilder.newBuilder().weakKeys().build();//new HashMap<>();
        bdvSourceAndConverterService.setDisplayService(this);
        SourceAndConverterServices.setSourceAndConverterDisplayService(this);
        log.accept("Service initialized.");
    }

    /**
     * Closes appropriately a BdvHandle which means that it updates
     * the callbacks for ConverterSetups and updates the ObjectService
     * @param bdvh
     */
    public void closeBdv(BdvHandle bdvh) {
        os.removeObject(bdvh);
        displayToMetadata.invalidate(bdvh); // enables memory release on GC - even if it bdv was weekly referenced

        // Fix BigWarp closing issue
        boolean isPaired = pairedBdvs.stream().filter(p -> (p.getA()==bdvh)||(p.getB()==bdvh)).findFirst().isPresent();
        if (isPaired) {
            Pair<BdvHandle, BdvHandle> pair = pairedBdvs.stream().filter(p -> (p.getA()==bdvh)||(p.getB()==bdvh)).findFirst().get();
            pairedBdvs.remove(pair);
            if (pair.getA()==bdvh) {
                closeBdv(pair.getB());
            } else {
                closeBdv(pair.getA());
            }
        }
    }

    /**
     * Enables proper closing of Big Warp paired BdvHandles
     */
    List<Pair<BdvHandle, BdvHandle>> pairedBdvs = new ArrayList<>();
    public void pairClosing(BdvHandle bdv1, BdvHandle bdv2) {
        pairedBdvs.add(new Pair<BdvHandle, BdvHandle>() {
            @Override
            public BdvHandle getA() {
                return bdv1;
            }

            @Override
            public BdvHandle getB() {
                return bdv2;
            }
        });
    }

    /**
     * Registers a sourceandconverter which has originated from a BdvHandle
     * Useful for BigWarp where the grid and the deformation magnitude sourceandconverter are created
     * into bigwarp
     * @param bdvh_in
     */
    public void registerBdvSource(BdvHandle bdvh_in) {
        bdvh_in.getViewerPanel().state().getSources().forEach(sac -> {
            if (!bdvSourceAndConverterService.isRegistered(sac)) {
                bdvSourceAndConverterService.register(sac);
                //     bdvSourceAndConverterService.sacToMetadata.get(sac).put(CONVERTER_SETUP, bdvh_in.getConverterSetups().getConverterSetup(sac));
            }
            // TODO : if convertersetup is already present, check that it respond to this bdv,
            // otherwise build it, or get it from bdvh_in
        });
    }

    /**
     * Updates bdvHandles which are displaying at least one of this sacs
     * Potentially improvement is to check whether the timepoint need an update ?
     * @param sacs
     */
    public void updateDisplays(SourceAndConverter... sacs)
    {
        getDisplaysOf(sacs).forEach(bdvHandle -> bdvHandle.getViewerPanel().requestRepaint());
    }

    /**
     * Returns the list of sacs held within a BdvHandle ( whether they are visible or not )
     * List is ordered by index in the BdvHandle -> complexification to implement
     * the mixed projector
     * TODO : Avoid duplicates by returning a Set
     * @param bdvHandle
     * @return
     */
    public List<SourceAndConverter<?>> getSourceAndConverterOf(BdvHandle bdvHandle) {
        return bdvHandle.getViewerPanel().state().getSources();
    }

    /**
     * Returns a List of BdvHandle which are currently displaying a sac
     * Returns an empty set in case the sac is not displayed
     * @param sacs
     * @return
     */
    public Set<BdvHandle> getDisplaysOf(SourceAndConverter... sacs) {
        if (sacs == null) {
            return new HashSet<>();
        }

        List<SourceAndConverter<?>> sacList = Arrays.asList(sacs);

        return os.getObjects(BdvHandle.class)
                .stream()
                .filter(bdv -> bdv.getViewerPanel().state()
                        .getSources().stream()
                        .anyMatch(sac -> sacList.contains(sac)))
                .collect(Collectors.toSet());

    }

    public List<BdvHandle> getDisplays() {
        return os.getObjects(BdvHandle.class);
    }

    /**
     * Map containing objects that are 1 to 1 linked to a Display ( a BdvHandle object )
     * Keys are Weakly referenced -> Metadata should be GCed if referenced only here
     */
    Cache<BdvHandle, Map<String, Object>> displayToMetadata;

    public void setDisplayMetadata( BdvHandle bdvh, String key, Object data )
    {
        if (bdvh == null) {
            System.err.println("Error : bdvh is null in setMetadata function! ");
            //return;
        }
        if (displayToMetadata.getIfPresent( bdvh ) == null) {
            // Create Metadata
            displayToMetadata.put(bdvh, new HashMap<>());
        }
        displayToMetadata.getIfPresent( bdvh ).put( key, data );
    }

    public Object getDisplayMetadata( BdvHandle bdvh, String key )
    {
        if (displayToMetadata.getIfPresent(bdvh)!=null) {
            return displayToMetadata.getIfPresent(bdvh).get(key);
        } else {
            return null;
        }
    }

    /**
     * SourceAndConverter filter node : Selects SpimData and allow for duplicate
     */

    public static class BdvHandleFilterNode extends SourceFilterNode {

        public BdvHandle bdvh;
        String name;

        public boolean filter(SourceAndConverter sac) {
            return bdvh.getViewerPanel().state().getSources().contains(sac);
        }

        public BdvHandleFilterNode(String name, BdvHandle bdvh) {
            super(name,null, true);
            this.name = name;
            this.filter = this::filter;
            this.bdvh = bdvh;
        }

        public String toString() {
            return getName();
        }

        @Override
        public Object clone() {
            return new BdvHandleFilterNode(name, bdvh);
        }

    }

}
