package sc.fiji.bdvpg.scijava.services;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.util.LUTConverterSetup;
import bdv.viewer.BigWarpConverterSetupWrapper;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.SourceGroup;
import bdv.viewer.state.ViewerState;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorConverter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;
import bdv.util.ARGBColorConverterSetup;
import sc.fiji.bdvpg.services.IBdvSourceAndConverterDisplayService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Scijava Service which handles the Display of Bdv SourceAndConverters in one or multiple Bdv Windows
 * Pairs with BdvSourceAndConverterService, but this service is optional
 *
 * Handling multiple Sources displayed in potentially multiple Bdv Windows
 * Make its best to keep in synchronizations all of this, without creating errors nor memory leaks
 */

@Plugin(type= Service.class)
public class BdvSourceAndConverterDisplayService extends AbstractService implements SciJavaService, IBdvSourceAndConverterDisplayService {

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println(BdvSourceAndConverterDisplayService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println(BdvSourceAndConverterDisplayService.class.getSimpleName()+":"+str);

    /**
     * Used to add Aliases for BdvHandle objects
     **/
    @Parameter
    ScriptService scriptService;

    /**
     * Service containing all registered Bdv Sources
     **/
    @Parameter
    BdvSourceAndConverterService bss;

    /**
     * Used to create Bdv Windows when necessary
     **/
    @Parameter
    CommandService cs;

    /**
     * Used to retrieved the last active Bdv Windows (if the activated callback has been set right)
     **/
    @Parameter
    GuavaWeakCacheService cacheService;
    @Parameter
    ObjectService os;

    /**
     * Map linking a BdvHandle to the Sources it's been displaying
     * TODO : check whether this is useful
     **/
    Map<BdvHandle, List<SourceAndConverter>> sourcesDisplayedInBdvWindows;

    /**
     * Map linking a Source to the different locations where it's been displayed
     * BdvHandles displaying Source ( also storing the local index of the Source )
     **/
    Map<SourceAndConverter, List<BdvHandleRef>> locationsDisplayingSource;

    /**
     * Returns the last active Bdv or create a new one
     */
    public BdvHandle getActiveBdv() {
        List<BdvHandle> bdvhs = os.getObjects(BdvHandle.class);
        if ((bdvhs == null)||(bdvhs.size()==0)) {
            try
            {
                return (BdvHandle)
                        cs.run(BdvWindowCreatorCommand.class,
                                true,
                                "is2D", false,
                                "windowTitle", "Bdv",
                                "projector", "Sum Projector").get().getOutput("bdvh");//*/
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
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
     * Displays a Source, the last active bdv is chosen since none is specified in this method
     * @param src
     */
    public void show(SourceAndConverter src) {
         show(getActiveBdv(), src);
    }

    /**
     * Displays a Bdv sourceandconverter into the specified BdvHandle
     * This function really is the core of this service
     * It mimicks or copies the functions of BdvVisTools because it is responsible to
     * create converter, volatiles, convertersetups and so on
     * @param sac
     * @param bdvh
     */
    public void show(BdvHandle bdvh, SourceAndConverter sac) {
        // If the sourceandconverter is not registered, register it
        if (!bss.isRegistered(sac)) {
            bss.register(sac);
        }

        // Escape if the sourceandconverter is already shown is this BdvHandle
        if (locationsDisplayingSource.get(sac)!=null) {
            if (locationsDisplayingSource.get(sac).stream().anyMatch(bdvhr -> bdvhr.bdvh.equals(bdvh))) {
                return;
            }
        }

        // Stores in which BdvHandle the sourceandconverter will be displayed
        if (!sourcesDisplayedInBdvWindows.containsKey(bdvh)) {
            sourcesDisplayedInBdvWindows.put(bdvh, new ArrayList<>());
        }
        sourcesDisplayedInBdvWindows.get(bdvh).add(sac);

        bdvh.getViewerPanel().addSource(sac);
        bdvh.getSetupAssignments().addSetup(getConverterSetup(sac));

        // Stores where the sourceandconverter is displayed (BdvHandle and index)
        BdvHandleRef bhr = new BdvHandleRef(bdvh, bdvh.getViewerPanel().getState().numSources());
        if (!(locationsDisplayingSource.containsKey(sac))) {
            System.out.println("erase locations display sourceandconverter of "+sac.getSpimSource().getName());
            locationsDisplayingSource.put(sac, new ArrayList<>());
        }
        locationsDisplayingSource.get(sac).add(bhr);
    }

    /**
     * Removes a sourceandconverter from all BdvHandle displaying this sourceandconverter
     * Updates all references of other Sources present
     * @param source
     */
    public void removeFromAllBdvs(SourceAndConverter source) {
        if (locationsDisplayingSource.get(source)!=null) {
            while (locationsDisplayingSource.get(source).size() > 0) {
                remove(locationsDisplayingSource.get(source).get(0).bdvh, source);
            }
        }
    }

    /**
     * Removes a sourceandconverter from the active Bdv
     * Updates all references of other Sources present
     * @param source
     */
    public void removeFromActiveBdv(SourceAndConverter source) {
        // This condition avoids creating a window for nothing
        if (os.getObjects(BdvHandle.class).size()>0) {
            remove(getActiveBdv(), source);
        }
    }

    /**
     * Removes a sourceandconverter from a BdvHandle
     * Updates all references of other Sources present
     * @param bdvh
     * @param source
     */
    public void remove(BdvHandle bdvh, SourceAndConverter source) {
        // Needs to removeFromAllBdvs the sourceandconverter, if present
        while (locationsDisplayingSource.get(source).stream().anyMatch(
            bdvHandleRef -> bdvHandleRef.bdvh.equals(bdvh)
        )) {
            // It is displayed in this bdv
            BdvHandleRef bdvhr = locationsDisplayingSource
                    .get(source).stream().filter(
                            bdvHandleRef -> bdvHandleRef.bdvh.equals(bdvh)
                    ).findFirst().get();

            int index = bdvhr.indexInBdv;
            this.logLocationsDisplayingSource();
            log.accept("Remove source "+source+" indexed "+index+" in BdvHandle "+bdvh.getViewerPanel().getName());

            /**
             * A reflection forced access to ViewerState.removeSource(int index)
             * protected void removeSource( final int index )
             * is necessary because the SpimSource is not precise enough as a key : it can be displayed multiple times with different converters
             * all following calls fail:
             *  bdvh.getViewerPanel().getState().removeSource();//.removeGroup(sg);//remove(index);//.removeSource(source.getSpimSource()); // TODO : Check!!
             */
            removeSourceViaReflection(bdvh, index);

            if (bss.getAttachedSourceAndConverterData().get(source).get(CONVERTERSETUP)!=null) {
                log.accept("Removing converter setup...");
                bdvh.getSetupAssignments().removeSetup((ConverterSetup) bss.getAttachedSourceAndConverterData().get(source).get(CONVERTERSETUP));
            }

            // Removes reference to where the sourceandconverter is located
            locationsDisplayingSource.get(source).remove(bdvhr);

            // Updates reference of index location
            locationsDisplayingSource
                    .keySet()
                    .stream()
                    .forEach(src -> {
                        locationsDisplayingSource.get(src)
                                .stream()
                                .filter(ref -> ((ref.bdvh.equals(bdvh))&&(ref.indexInBdv>index)))
                                .forEach(bdvHandleRef ->
                                        bdvHandleRef.indexInBdv--);
                    });

        }
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
        if (!bss.isRegistered(sac)) {
            bss.register(sac);
        }

        // If no ConverterSetup is built then build it
        if (bss.data.get(sac).get(CONVERTERSETUP)== null) {
            Runnable converterSetupCallBack = () -> {
                if (locationsDisplayingSource.get(sac)!=null) {
                    locationsDisplayingSource.get(sac).forEach(bhr -> bhr.bdvh.getViewerPanel().requestRepaint());
                }
            };

            ConverterSetup setup = SourceAndConverterUtils.createConverterSetup(sac,converterSetupCallBack);
            bss.data.get(sac).put(CONVERTERSETUP,  setup );
        }

        return (ConverterSetup)bss.data.get(sac).get(CONVERTERSETUP);
    }

    /**
     * Updates converter and ConverterSetup of a Source, + updates display
     * TODO: This method currently modifies the order of the sources shown in the bdv window
     * While this is not important for most bdvhandle, this could affect the functionality
     * of BigWarp
     * LIMITATION : Cannot use LUT for ARGBType -> TODO check type and send an error
     * @param source
     * @param cvt
     */
    public void updateConverter(SourceAndConverter source, Converter cvt) {
        errlog.accept("Unsupported operation : a new SourceAndConverterObject should be built. (TODO) ");
        /*
        // Precaution : the sourceandconverter should be registered
        if (!bss.isRegistered(sourceandconverter)) {
            bss.register(sourceandconverter);
        }
        // Step 1 : build the proper objects
        // Build a new ConverterSetup from the converter
        ConverterSetup setup;
        if (cvt instanceof ColorConverter) {
            setup = new ARGBColorConverterSetup((ColorConverter) cvt);
        } else if (cvt instanceof RealLUTConverter) {
            setup = new LUTConverterSetup((RealLUTConverter) cvt);
        } else {
            errlog.accept("Cannot build convertersetup with converter of class "+cvt.getClass().getSimpleName());
            return;
        }

        // Callback when convertersetup is changed
        setup.setViewer(() -> {
            if (locationsDisplayingSource.get(sourceandconverter)!=null) {
                locationsDisplayingSource.get(sourceandconverter).forEach(bhr -> bhr.bdvh.getViewerPanel().requestRepaint());
            }
        });

        // Step 3 : store where the sources were displayed
        Set<BdvHandle> bdvhDisplayingSource= locationsDisplayingSource.get(sourceandconverter)
                .stream()
                .map(bdvHandleRef -> bdvHandleRef.bdvh)
                .collect(Collectors.toSet());

        // Step 4 : remove where the sourceandconverter was displayed
        removeFromAllBdvs(sourceandconverter);

        // Step 5 : updates cached objects
        bss.getAttachedSourceAndConverterData().get(sourceandconverter).put(CONVERTERSETUP, setup);

        // Step 6 : restore sourceandconverter display location
        bdvhDisplayingSource.forEach(bdvh -> show(bdvh, sourceandconverter));
        */
    }

    /**
     * Service initialization
     */
    @Override
    public void initialize() {
        scriptService.addAlias(BdvHandle.class);
        sourcesDisplayedInBdvWindows = new HashMap<>();
        locationsDisplayingSource = new HashMap<>();
        bss.setDisplayService(this);
        log.accept("Service initialized.");
    }

    /**
     * Closes appropriately a BdvHandle which means that it updates
     * the callbacks for ConverterSetups and updates the ObjectService
     * @param bdvh
     */
    public void closeBdv(BdvHandle bdvh) {
        // Programmatically or User action
        // Before closing the Bdv Handle, we need to keep up to date all objects:
        // 1 sourcesDisplayedInBdvWindows
        sourcesDisplayedInBdvWindows.remove(bdvh);
        // 2 locationsDisplayingSource
        locationsDisplayingSource.values().forEach(list ->
            list.removeIf(bdvhr -> bdvhr.bdvh.equals(bdvh))
        );
        os.removeObject(bdvh);

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
     * @param index
     */
    public void registerBdvSource(BdvHandle bdvh_in, int index) {
        SourceAndConverter sac = bdvh_in.getViewerPanel().getState().getSources().get(index);
        bss.register(sac);
        // Stores where the sourceandconverter is displayed (BdvHandle and index)
        BdvHandleRef bhr = new BdvHandleRef(bdvh_in, index);
        if (!locationsDisplayingSource.containsKey(sac)) {
            locationsDisplayingSource.put(sac, new ArrayList<>());
        }
        locationsDisplayingSource.get(sac).add(bhr);

        ConverterSetup cs = getConverterSetupsViaReflection(bdvh_in).get(index);

        // BigWarp Hack
        if (cs instanceof BigWarpConverterSetupWrapper) {
            BigWarpConverterSetupWrapper wcs = (BigWarpConverterSetupWrapper) cs;
            wcs.getSourceConverterSetup().setViewer(() -> {
                if (locationsDisplayingSource.get(sac) != null) {
                    locationsDisplayingSource.get(sac).forEach(bhref -> bhref.bdvh.getViewerPanel().requestRepaint());
                }
            });
        } else {
            cs.setViewer(() -> {
                if (locationsDisplayingSource.get(sac) != null) {
                    locationsDisplayingSource.get(sac).forEach(bhref -> bhref.bdvh.getViewerPanel().requestRepaint());
                }
            });
        }
        bss.data.get(sac).put(CONVERTERSETUP, cs);
    }

    /**
     * For debug purposes, check that all is in sync
     */
    public void logLocationsDisplayingSource() {
        locationsDisplayingSource.forEach((src, lbdvref) -> {
            log.accept(src.getSpimSource().getName()+":"+src.toString());
            lbdvref.forEach(bdvref -> {
                log.accept("\t bdv = "+bdvref.bdvh.toString()+"\t i = "+bdvref.indexInBdv);
            });
        });
    }

    /**
     * Class containing a BdvHandle and an index -> reference to where a Source is located
     * This could disappear if the indexing logic disappears
     */
    class BdvHandleRef {
        BdvHandle bdvh;
        int indexInBdv;

        public BdvHandleRef(BdvHandle bdvh, int idx) {
            this.bdvh = bdvh;
            this.indexInBdv = idx;
        }
    }

    /**
     * TODO : check if this access is necessary
     * @param bdvh
     * @return
     */
    public List< ConverterSetup > getConverterSetupsViaReflection(BdvHandle bdvh) {
        try {
            Field fConverterSetup = SetupAssignments.class.getDeclaredField("setups");

            fConverterSetup.setAccessible(true);

            return (ArrayList< ConverterSetup >) fConverterSetup.get(bdvh.getSetupAssignments());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * A reflection forced access to ViewerState.removeSource(int index)
     * protected void removeSource( final int index )
     * is necessary because the SpimSource is not precise enough as a key : it can be displayed multiple times with different converters
     * all following calls fail:
     *  bdvh.getViewerPanel().getState().removeSource();//.removeGroup(sg);//remove(index);//.removeSource(source.getSpimSource());
     *  Small issue : F6 tab is not updated
     */
    void removeSourceViaReflection(BdvHandle bdvh, int index) {
        try {
            // Two reflections because we need the state, and not its copy
            Field f = ViewerPanel.class.getDeclaredField("state");
            f.setAccessible(true);

            ViewerState state = (ViewerState) f.get(bdvh.getViewerPanel());

            Method m = ViewerState.class.getDeclaredMethod("removeSource", int.class);
            m.setAccessible(true);

            m.invoke(state, index-1); // 1 based index to zero based. don't ask

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
