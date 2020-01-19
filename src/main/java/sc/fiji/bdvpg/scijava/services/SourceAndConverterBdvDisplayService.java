package sc.fiji.bdvpg.scijava.services;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.BdvHandle;
import bdv.viewer.BigWarpConverterSetupWrapper;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.state.ViewerState;
import net.imglib2.converter.Converter;
import net.imglib2.util.Pair;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Scijava Service which handles the Display of Bdv SourceAndConverters in one or multiple Bdv Windows
 * Pairs with BdvSourceAndConverterService, but this service is optional
 *
 * Handling multiple Sources displayed in potentially multiple Bdv Windows
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
     * Service containing all registered Bdv Sources
     **/
    @Parameter
    SourceAndConverterService bdvSourceAndConverterService;

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
    Map<BdvHandle, List<SourceAndConverter>> bdvToSacs;

    /**
     * Map linking a Source to the different locations where it's been displayed
     * BdvHandles displaying Source ( also storing the local index of the Source )
     **/
    Map<SourceAndConverter, List<BdvHandleRef>> sacToBdvRefs;

    public BdvHandle getNewBdv() {
        try
        {
            return (BdvHandle)
                    cs.run(BdvWindowCreatorCommand.class,
                            true,
                            "is2D", false,
                            "windowTitle", "Bdv").get().getOutput("bdvh");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns the last active Bdv or create a new one
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
     * Displays a Source, the last active bdv is chosen since none is specified in this method
     * @param src
     */
    public void show(SourceAndConverter src) {
         show(getActiveBdv(), src);
    }


    /**
     * Makes visible a source, makes it visible in all bdvs according to BdvhReferences
     * @param sac
     */
    public void makeVisible(SourceAndConverter sac) {
        if ( sacToBdvRefs.get(sac)!=null)
        sacToBdvRefs.get(sac).forEach( bdvhr -> bdvhr.bdv.getViewerPanel().getVisibilityAndGrouping().setSourceActive(bdvhr.indexInBdv-1, true));
    }

    /**
     * Makes invisible a source, makes it invisible in all bdvs according to BdvhReferences
     * @param sac
     */
    public void makeInvisible(SourceAndConverter sac) {
        if ( sacToBdvRefs.get(sac)!=null)
            sacToBdvRefs.get(sac).forEach( bdvhr -> bdvhr.bdv.getViewerPanel().getVisibilityAndGrouping().setSourceActive(bdvhr.indexInBdv-1, false));

    }

    /**
     * Displays a Bdv sourceandconverter into the specified BdvHandle
     * This function really is the core of this service
     * It mimicks or copies the functions of BdvVisTools because it is responsible to
     * create converter, volatiles, convertersetups and so on
     * @param sac
     * @param bdvHandle
     */
    public void show(BdvHandle bdvHandle, SourceAndConverter sac) {
        // If the sourceandconverter is not registered, register it
        if (!bdvSourceAndConverterService.isRegistered(sac)) {
            bdvSourceAndConverterService.register(sac);
        }

        // Escape if the sourceandconverter is already shown is this BdvHandle
        if ( sacToBdvRefs.get(sac)!=null) {
            if ( sacToBdvRefs.get(sac).stream().anyMatch( bdvhr -> bdvhr.bdv.equals(bdvHandle))) {
                return;
            }
        }

        // Stores in which BdvHandle the sourceandconverter will be displayed
        if (!bdvToSacs.containsKey(bdvHandle)) {
            bdvToSacs.put(bdvHandle, new ArrayList<>());
        }
        bdvToSacs.get(bdvHandle).add(sac);

        bdvHandle.getViewerPanel().addSource(sac);
        bdvHandle.getSetupAssignments().addSetup(getConverterSetup(sac));

        // Stores where the sourceandconverter is displayed (BdvHandle and index)
        BdvHandleRef bhr = new BdvHandleRef(bdvHandle, bdvHandle.getViewerPanel().getState().numSources());
        if (!( sacToBdvRefs.containsKey(sac))) {
            System.out.println("erase locations display sourceandconverter of "+sac.getSpimSource().getName());
            sacToBdvRefs.put(sac, new ArrayList<>());
        }
        sacToBdvRefs.get(sac).add(bhr);
    }

    /**
     * Removes a sourceandconverter from all BdvHandle displaying this sourceandconverter
     * Updates all references of other Sources present
     * @param source
     */
    public void removeFromAllBdvs(SourceAndConverter source) {
        if ( sacToBdvRefs.get(source)!=null) {
            while ( sacToBdvRefs.get(source).size() > 0) {
                remove( sacToBdvRefs.get(source).get(0).bdv, source);
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
        if ( sacToBdvRefs.get(source)!=null) {
            while ( sacToBdvRefs.get(source).stream().anyMatch(
                    bdvHandleRef -> bdvHandleRef.bdv.equals(bdvh)
            )) {
                // It is displayed in this bdv
                BdvHandleRef bdvhr = sacToBdvRefs
                        .get(source).stream().filter(
                                bdvHandleRef -> bdvHandleRef.bdv.equals(bdvh)
                        ).findFirst().get();

                int index = bdvhr.indexInBdv;
                this.logLocationsDisplayingSource();
                log.accept("Remove source " + source + " indexed " + index + " in BdvHandle " + bdvh.getViewerPanel().getName());

                /**
                 * A reflection forced access to ViewerState.removeSource(int index)
                 * protected void removeSource( final int index )
                 * is necessary because the SpimSource is not precise enough as a key : it can be displayed multiple times with different converters
                 * all following calls fail:
                 *  bdvh.getViewerPanel().getState().removeSource();//.removeGroup(sg);//remove(index);//.removeSource(source.getSpimSource()); // TODO : Check!!
                 */
                removeSourceViaReflection(bdvh, index);

                if ( bdvSourceAndConverterService.getSacToMetadata().get(source).get( CONVERTER_SETUP ) != null) {
                    log.accept("Removing converter setup...");
                    bdvh.getSetupAssignments().removeSetup((ConverterSetup) bdvSourceAndConverterService.getSacToMetadata().get(source).get( CONVERTER_SETUP ));
                }

                // Removes reference to where the sourceandconverter is located
                sacToBdvRefs.get(source).remove(bdvhr);

                // Updates reference of index location
                sacToBdvRefs
                        .keySet()
                        .stream()
                        .forEach(sac -> {
                            sacToBdvRefs.get(sac)
                                    .stream()
                                    .filter(ref -> ((ref.bdv.equals(bdvh)) && (ref.indexInBdv > index)))
                                    .forEach(bdvHandleRef ->
                                            bdvHandleRef.indexInBdv--);
                        });

            }
            bdvh.getViewerPanel().requestRepaint();
        }
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
        if ( bdvSourceAndConverterService.sacToMetadata.get(sac).get( CONVERTER_SETUP )== null) {
            Runnable converterSetupCallBack = () -> {
                if ( sacToBdvRefs.get(sac)!=null) {
                    sacToBdvRefs.get(sac).forEach( bhr -> bhr.bdv.getViewerPanel().requestRepaint());
                }
            };

            ConverterSetup setup = SourceAndConverterUtils.createConverterSetup(sac,converterSetupCallBack);
            bdvSourceAndConverterService.sacToMetadata.get(sac).put( CONVERTER_SETUP,  setup );
        }

        return (ConverterSetup) bdvSourceAndConverterService.sacToMetadata.get(sac).get( CONVERTER_SETUP );
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
        bdvToSacs = new HashMap<>();
        sacToBdvRefs = new HashMap<>();
        bdvSourceAndConverterService.setDisplayService(this);
        SourceAndConverterServices.sourceAndConverterBdvDisplayService = this;
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
        bdvToSacs.remove(bdvh);
        // 2 locationsDisplayingSource
        sacToBdvRefs.values().forEach( list ->
            list.removeIf(bdvhr -> bdvhr.bdv.equals(bdvh))
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
        bdvSourceAndConverterService.register(sac);
        // Stores where the sourceandconverter is displayed (BdvHandle and index)
        BdvHandleRef bhr = new BdvHandleRef(bdvh_in, index);
        if (!sacToBdvRefs.containsKey(sac)) {
            sacToBdvRefs.put(sac, new ArrayList<>());
        }
        sacToBdvRefs.get(sac).add(bhr);

        ConverterSetup cs = getConverterSetupsViaReflection(bdvh_in).get(index);

        // BigWarp Hack
        if (cs instanceof BigWarpConverterSetupWrapper) {
            BigWarpConverterSetupWrapper wcs = (BigWarpConverterSetupWrapper) cs;
            wcs.getSourceConverterSetup().setViewer(() -> {
                if ( sacToBdvRefs.get(sac) != null) {
                    sacToBdvRefs.get(sac).forEach( bhref -> bhref.bdv.getViewerPanel().requestRepaint());
                }
            });
        } else {
            cs.setViewer(() -> {
                if ( sacToBdvRefs.get(sac) != null) {
                    sacToBdvRefs.get(sac).forEach( bhref -> bhref.bdv.getViewerPanel().requestRepaint());
                }
            });
        }
        bdvSourceAndConverterService.sacToMetadata.get(sac).put( CONVERTER_SETUP, cs);
    }

    /**
     * For debug purposes, check that all is in sync
     */
    public void logLocationsDisplayingSource() {
        sacToBdvRefs.forEach(( sac, bdvHandleRefs) -> {
            log.accept(sac.getSpimSource().getName()+":"+sac.toString());
            bdvHandleRefs.forEach(bdvref -> {
                log.accept("\t bdv = "+bdvref.bdv.toString()+"\t i = "+bdvref.indexInBdv);
            });
        });
    }

    public void updateDisplays(SourceAndConverter[] sacs)
    {
        final Set< BdvHandle > bdvHandlesNeedingRepaint = new HashSet<>();
        for( SourceAndConverter sac : sacs )
        {
            if ( sacToBdvRefs.containsKey( sac ) )
                sacToBdvRefs.get( sac ).forEach( bdvRef -> bdvHandlesNeedingRepaint.add( bdvRef.bdv )  );
        }

       bdvHandlesNeedingRepaint.forEach( bdv -> bdv.getViewerPanel().requestRepaint() );
    }

    /**
     * Class containing a BdvHandle and an index -> reference to where a Source is located
     * TODO: This could disappear if the indexing logic disappears
     *
     */
    class BdvHandleRef {
        BdvHandle bdv;
        int indexInBdv;

        public BdvHandleRef( BdvHandle bdv, int idx) {
            this.bdv = bdv;
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

    public List< SourceAndConverter > getSourceAndConverters( BdvHandle bdv )
    {
        return bdvToSacs.get( bdv );
    }

}
