package sc.fiji.bdvpg.scijava.services;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
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

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

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
public class BdvSourceDisplayService extends AbstractService implements SciJavaService {

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println(BdvSourceDisplayService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println(BdvSourceDisplayService.class.getSimpleName()+":"+str);


    /**
     * String keys for data stored in the BdvSourceService
     **/
    final public static String  VOLATILESOURCE = "VOLATILESOURCE";
    final public static String  CONVERTER = "CONVERTER";
    final public static String  CONVERTERSETUP = "CONVERTERSETUP";

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
                                "windowTitle", "Bdv",
                                "px",0,
                                "py",0,
                                "pz",0,
                                "s",1).get().getOutput("bdvh");//*/
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

        // Does it already have additional objects necessary for display ?
        // - Converter to ARGBType
        // - ConverterSetup
        if (!bss.data.get(src).containsKey(CONVERTER)) {
            createConverterAndConverterSetup(src);
        }

        // - Volatile view
        if (!bss.data.get(src).containsKey(VOLATILESOURCE)) {
            createVolatile(src);
        }

        // Stores in which BdvHandle the source will be displayed
        if (!sourcesDisplayedInBdvWindows.containsKey(bdvh)) {
            sourcesDisplayedInBdvWindows.put(bdvh, new ArrayList<>());
        }
        sourcesDisplayedInBdvWindows.get(bdvh).add(src);

        // Construct SourceAndConverter Object
        // TODO : Volatile
        //SourceAndConverter vsac = null;
        //if (bss.data.get(src).volatileSource!=null) {
        //    vsac = new SourceAndConverter(bss.data.get(src).volatileSource, bss.data.get(src).converter);
        //}

        SourceAndConverter sac = new SourceAndConverter(src, (Converter) bss.data.get(src).get(CONVERTER));
        bdvh.getViewerPanel().addSource(sac);
        bdvh.getSetupAssignments().addSetup((ConverterSetup)bss.data.get(src).get(CONVERTERSETUP));

        // Stores where the source is displayed (BdvHandle and index)
        BdvHandleRef bhr = new BdvHandleRef(bdvh, bdvh.getViewerPanel().getState().numSources());
        if (!locationsDisplayingSource.containsKey(src)) {
            locationsDisplayingSource.put(src, new ArrayList<>());
        }
        locationsDisplayingSource.get(src).add(bhr);

    }

    /**
     * Creates converters and convertersetup for a source
     * TODO : release constrains on type
     * @param source
     * @param <T>
     */
    public < T extends RealType< T >> void createConverterAndConverterSetup(Source<T> source) {
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
        bss.data.get(source).put(CONVERTERSETUP, setup);
    }

    /**
     * TODO and also maybe move it to BdvSourceService
     * @param source
     */
    public void createVolatile(Source source) {
        // TODO
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
        // Actually close the bdv window
        // bdvh.close();
        log.accept("bvdh:"+bdvh.toString()+" closed");
        os.removeObject(bdvh);
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
}
