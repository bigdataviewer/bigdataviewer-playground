package sc.fiji.bdvpg.scijava.services;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.BdvHandle;
import bdv.util.LUTConverterSetup;
import bdv.viewer.BigWarpConverterSetupWrapper;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
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
import sc.fiji.bdvpg.services.IBdvSourceDisplayService;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Scijava Service which handles the Display of Bdv Sources in one or multiple Bdv Windows
 * Pairs with BdvSourceService, but this ser BdvSourceDisplayService is optional
 *
 * Its functions are:
 * - For each source, creating objects needed for display:
 *     - Converter to ARGBType
 *     - Converter Setup
 *     - Volatile View
 *
 * Handling multiple Sources displayed in potentially multiple Bdv Windows
 * Make its best to keep in synchronizations all of this, without creating errors nor memory leaks
 */

@Plugin(type= Service.class)
public class BdvSourceDisplayService extends AbstractService implements SciJavaService, IBdvSourceDisplayService {

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println(BdvSourceDisplayService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println(BdvSourceDisplayService.class.getSimpleName()+":"+str);

    /**
     * Used to add Aliases for BdvHandle objects
     **/
    @Parameter
    ScriptService scriptService;

    /**
     * Service containing all registered Bdv Sources
     **/
    @Parameter
    BdvSourceService bss;

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
     * Set of BdvHandles currently opened
     **/
    Set<BdvHandle> bdvhs;

    /**
     * Map linking a BdvHandle to the Sources it's been displaying
     * TODO : check whether this is useful
     **/
    Map<BdvHandle, List<Source>> sourcesDisplayedInBdvWindows;

    /**
     * Map linking a Source to the different locations where it's been displayed
     * BdvHandles displaying Source ( also storing the local index of the Source )
     **/
    Map<Source, List<BdvHandleRef>> locationsDisplayingSource;

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
                                "windowTitle", "Bdv").get().getOutput("bdvh");//*/
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
    public void show(Source src) {
         show(getActiveBdv(), src);
    }

    /**
     * Displays a Bdv source into the specified BdvHandle
     * This function really is the core of this service
     * It mimicks or copies the functions of BdvVisTools because it is responsible to
     * create converter, volatiles, convertersetups and so on
     * @param src
     * @param bdvh
     */
    public void show(BdvHandle bdvh, Source src) {
        // If the source is not registered, register it
        if (!bss.isRegistered(src)) {
            bss.register(src);
        }

        // Stores in which BdvHandle the source will be displayed
        if (!sourcesDisplayedInBdvWindows.containsKey(bdvh)) {
            sourcesDisplayedInBdvWindows.put(bdvh, new ArrayList<>());
        }
        sourcesDisplayedInBdvWindows.get(bdvh).add(src);

        SourceAndConverter sac = getSourceAndConverter(src);

        bdvh.getViewerPanel().addSource(sac);
        bdvh.getSetupAssignments().addSetup((ConverterSetup)bss.data.get(src).get(CONVERTERSETUP));

        // Stores where the source is displayed (BdvHandle and index)
        BdvHandleRef bhr = new BdvHandleRef(bdvh, bdvh.getViewerPanel().getState().numSources());
        if (!(locationsDisplayingSource.containsKey(src))) {
            System.out.println("erase locations display source of "+src.getName());
            locationsDisplayingSource.put(src, new ArrayList<>());
        }
        locationsDisplayingSource.get(src).add(bhr);
    }

    /**
     * Removes a source from all BdvHandle displaying this source
     * Updates all references of other Sources present
     * @param source
     */
    public void removeFromAllBdvs(Source source) {
        while (locationsDisplayingSource.get(source).size()>0) {
            remove(locationsDisplayingSource.get(source).get(0).bdvh, source);
        }
    }

    /**
     * Removes a source from the active Bdv
     * Updates all references of other Sources present
     * @param source
     */
    public void removeFromActiveBdv(Source source) {
        // This condition avoids creating a window for nothing
        if (os.getObjects(BdvHandle.class).size()>0) {
            remove(getActiveBdv(), source);
        }
    }

    /**
     * Removes a source from a BdvHandle
     * Updates all references of other Sources present
     * @param bdvh
     * @param source
     */
    public void remove(BdvHandle bdvh, Source source) {
        // Needs to removeFromAllBdvs the source, if present
        while (locationsDisplayingSource.get(source).stream().anyMatch(
            bdvHandleRef -> bdvHandleRef.bdvh.equals(bdvh)
        )) {
            // It is displayed in this bdv
            BdvHandleRef bdvhr = locationsDisplayingSource
                    .get(source).stream().filter(
                            bdvHandleRef -> bdvHandleRef.bdvh.equals(bdvh)
                    ).findFirst().get();

            int index = bdvhr.indexInBdv;

            bdvh.getViewerPanel().removeSource(source);
            if (bss.getAttachedSourceData().get(source).get(CONVERTERSETUP)!=null) {
                log.accept("Removing converter setup...");
                bdvh.getSetupAssignments().removeSetup((ConverterSetup) bss.getAttachedSourceData().get(source).get(CONVERTERSETUP));
            }

            // Removes reference to where the source is located
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

    public ConverterSetup getConverterSetup(Source src) {
        if (!bss.isRegistered(src)) {
            bss.register(src);
        }
        if (bss.data.get(src).get(CONVERTERSETUP)== null) {
            if (src.getType() instanceof RealType) {
                createConverterAndConverterSetupRealType(src);
            } else if (src.getType() instanceof ARGBType) {
                createConverterAndConverterSetupARGBType(src);
            } else {
                errlog.accept("Cannot create converter setup for source of type "+src.getType());
                return null;
            }
        }
        return (ConverterSetup)bss.data.get(src).get(CONVERTERSETUP);
    }

    public SourceAndConverter getSourceAndConverter(Source src) {
        // Does it already have additional objects necessary for display ?
        // - Converter to ARGBType
        // - ConverterSetup
        if (!bss.data.get(src).containsKey(CONVERTER)) {
            if (src.getType() instanceof RealType) {
                createConverterAndConverterSetupRealType(src);
            } else if (src.getType() instanceof ARGBType) {
                createConverterAndConverterSetupARGBType(src);
            } else {
                errlog.accept("Cannot create converter setup for source of type "+src.getType());
                return null;
            }
        }

        // - Volatile view
        if (!bss.data.get(src).containsKey(VOLATILESOURCE)) {
            createVolatile(src);
        }

        // Construct SourceAndConverter Object
        SourceAndConverter vsac = null;
        if (bss.data.get(src).get(VOLATILESOURCE)!=null) {
            log.accept("The source has a volatile view!");
            vsac = new SourceAndConverter((Source)bss.data.get(src).get(VOLATILESOURCE),(Converter) bss.data.get(src).get(VOLATILECONVERTER));
        } else {
            log.accept("The source has no volatile view");
        }

        SourceAndConverter sac = new SourceAndConverter(src, (Converter) bss.data.get(src).get(CONVERTER), vsac);
        return sac;
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
    public void updateConverter(Source source, Converter cvt) {
        // Precaution : the source should be registered
        if (!bss.isRegistered(source)) {
            bss.register(source);
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
            if (locationsDisplayingSource.get(source)!=null) {
                locationsDisplayingSource.get(source).forEach(bhr -> bhr.bdvh.getViewerPanel().requestRepaint());
            }
        });

        // Step 3 : store where the sources were displayed
        Set<BdvHandle> bdvhDisplayingSource= locationsDisplayingSource.get(source)
                .stream()
                .map(bdvHandleRef -> bdvHandleRef.bdvh)
                .collect(Collectors.toSet());

        // Step 4 : remove where the source was displayed
        removeFromAllBdvs(source);

        // Step 5 : updates cached objects
        bss.getAttachedSourceData().get(source).put(CONVERTER, cvt);
        bss.getAttachedSourceData().get(source).put(VOLATILECONVERTER, cvt);
        bss.getAttachedSourceData().get(source).put(CONVERTERSETUP, setup);

        // Step 6 : restore source display location
        bdvhDisplayingSource.forEach(bdvh -> show(bdvh, source));
    }

    /**
     * Creates converters and convertersetup for a real typed source
     * @param source
     * @param <T>
     */
    public < T extends RealType< T >> void createConverterAndConverterSetupRealType(Source<T> source) {
        log.accept("Real Typed Source Registration...");
        final T type = Util.getTypeFromInterval( source.getSource( 0, 0 ) );
        final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
        final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
        final RealARGBColorConverter< T > converter ;
        if ( source.getType() instanceof Volatile)
            converter = new RealARGBColorConverter.Imp0<>( typeMin, typeMax );
        else
            converter = new RealARGBColorConverter.Imp1<>( typeMin, typeMax );

        converter.setColor( new ARGBType( 0xffffffff ) );

        final ARGBColorConverterSetup setup = new ARGBColorConverterSetup( converter );

        // Callback when convertersetup is changed
        setup.setViewer(() -> {
            if (locationsDisplayingSource.get(source)!=null) {
                locationsDisplayingSource.get(source).forEach(bhr -> bhr.bdvh.getViewerPanel().requestRepaint());
            }
        });

        bss.data.get(source).put(CONVERTER, converter);
        bss.data.get(source).put(VOLATILECONVERTER, converter);
        bss.data.get(source).put(CONVERTERSETUP, setup);
    }

    /**
     * Creates converters and convertersetup for a ARGB typed source
     * @param source
     * @param <T>
     */
    public < T extends ARGBType> void createConverterAndConverterSetupARGBType(Source<T> source) {
        final ScaledARGBConverter.VolatileARGB vconverter = new ScaledARGBConverter.VolatileARGB( 0, 255 );
        final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB( 0, 255 );

        bss.data.get(source).put(CONVERTER, converter);
        bss.data.get(source).put(VOLATILECONVERTER, vconverter);
        bss.data.get(source).put(CONVERTERSETUP, new ARGBColorConverterSetup( converter, vconverter ));
    }

        /**
         * TODO and also maybe move it to BdvSourceService
         * @param source
         */
    public void createVolatile(Source source) {
        if (bss.getAttachedSourceData().get(source).get(VOLATILESOURCE)==null) {
            if (source.getType() instanceof Volatile) {
                log.accept("Source is already volatile. No need to create a volatile source");
            }
            // TODO


        } else {
            log.accept("Source already has a volatile view");
        }
    }

    /**
     * Service initialization
     */
    @Override
    public void initialize() {
        scriptService.addAlias(BdvHandle.class);
        bdvhs = new HashSet<>();
        sourcesDisplayedInBdvWindows = new HashMap<>();
        locationsDisplayingSource = new HashMap<>();
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
        // 1 The set of opened BdvHandle
        bdvhs.remove(bdvh);
        // 2 sourcesDisplayedInBdvWindows
        sourcesDisplayedInBdvWindows.remove(bdvh);
        // 3 locationsDisplayingSource
        locationsDisplayingSource.values().forEach(list -> {
            list.removeIf(bdvhr -> bdvhr.bdvh.equals(bdvh));
        });
        log.accept("bvdh:"+bdvh.toString()+" closed");
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

    // Enables closing of BigWarp BdvHandles
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
     * Registers into the BdvSourceService a SourceAndConverter object
     * @param sac
     */
    public void registerSourceAndConverter(SourceAndConverter sac) {
        if (!bss.isRegistered(sac.getSpimSource())) {
            log.accept("Unregistered source and converter object");
            if (sac.asVolatile()==null) {
                bss.register(sac.getSpimSource());
                bss.data.get(sac.getSpimSource()).put(CONVERTER, sac.getConverter());
            } else {
                bss.register(sac.getSpimSource(), sac.asVolatile().getSpimSource());
                bss.data.get(sac.getSpimSource()).put(CONVERTER, sac.getConverter());
            }
            if (sac.getConverter() instanceof ColorConverter) {
                log.accept("The converter is a color converter");
            }
        }
    }

    /**
     * Registers a source which has originated from a BdvHandle
     * Useful for BigWarp where the grid and the deformation magnitude source are created
     * into bigwarp
     * @param bdvh_in
     * @param index
     */
    public void registerBdvSource(BdvHandle bdvh_in, int index) {
        SourceAndConverter sac = bdvh_in.getViewerPanel().getState().getSources().get(index);

        Source key = sac.getSpimSource();
        //if (
        registerSourceAndConverter(sac);//) {
        // Stores where the source is displayed (BdvHandle and index)
        BdvHandleRef bhr = new BdvHandleRef(bdvh_in, index);
        if (!locationsDisplayingSource.containsKey(key)) {
            locationsDisplayingSource.put(key, new ArrayList<>());
        }
        locationsDisplayingSource.get(key).add(bhr);
        // Updates converter setup callback to handle multiple displays of sources

        //ConverterSetup cs = bdvh_in.getSetupAssignments().getConverterSetups().get(index);//
        ConverterSetup cs = getConverterSetupsViaReflection(bdvh_in).get(index);

        // BigWarp Hack
        if (cs instanceof BigWarpConverterSetupWrapper) {
            BigWarpConverterSetupWrapper wcs = (BigWarpConverterSetupWrapper) cs;
            wcs.getSourceConverterSetup().setViewer(() -> {
                if (locationsDisplayingSource.get(key) != null) {
                    locationsDisplayingSource.get(key).forEach(bhref -> bhref.bdvh.getViewerPanel().requestRepaint());
                }
            });
        } else {
            cs.setViewer(() -> {
                if (locationsDisplayingSource.get(key) != null) {
                    locationsDisplayingSource.get(key).forEach(bhref -> bhref.bdvh.getViewerPanel().requestRepaint());
                }
            });
        }
        bss.data.get(key).put(CONVERTERSETUP, cs);
    }

    public void logLocationsDisplayingSource() {
        locationsDisplayingSource.forEach((src, lbdvref) -> {
            log.accept(src.getName()+":"+src.toString());
            lbdvref.forEach(bdvref -> {
                log.accept("\t bdv = "+bdvref.bdvh.toString()+"\t i = "+bdvref.indexInBdv);
            });
        });
    }

    /**
     * Class containing a BdvHandle and an index -> reference to where a Source is located
     */
    class BdvHandleRef {
        BdvHandle bdvh;
        int indexInBdv;

        public BdvHandleRef(BdvHandle bdvh, int idx) {
            this.bdvh = bdvh;
            this.indexInBdv = idx;
        }
    }

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

}
