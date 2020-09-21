package sc.fiji.bdvpg.scijava.services;

import bdv.ViewerImgLoader;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesShowCommand;
import sc.fiji.bdvpg.scijava.command.bdv.ScreenShotMakerCommand;
import sc.fiji.bdvpg.scijava.command.source.*;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Scijava Service which centralizes BDV Sources, independently of their display
 * BDV Sources can be registered to this Service.
 * This service adds the Source to the ObjectService, but on top of it,
 * It contains a Map which contains any object which can be linked to the sourceandconverter.
 *
 * It also handles SpimData object, but split all Sources into individual ones
 *
 * The most interesting of these objects are actually created by the BdvSourceAndConverterDisplayService:
 * - Converter to ARGBType, ConverterSetup, and Volatile view
 *
 * TODO : Think more carefully : maybe the volatile sourceandconverter should be done here...
 * Because when multiply wrapped sourceandconverter end up here, it maybe isn't possible to make the volatile view
 */

@Plugin(type=Service.class)
public class SourceAndConverterService extends AbstractService implements SciJavaService, ISourceAndConverterService
{

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> {};//System.out.println( SourceAndConverterService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println( SourceAndConverterService.class.getSimpleName()+":"+str);

    /**
     * Scijava Object Service : will contain all the sourceAndConverters
     */
    @Parameter
    ObjectService objectService;

    /**
     * Scriptservice : used for adding Source alias to help for scripting
     */
    @Parameter
    ScriptService scriptService;

    /**
     * uiService : used ot check if an UI is available to create a Swing Panel
     */
    @Parameter
    UIService uiService;

    /**
     * Display service : cannot be set through Parameter annotation due to 'circular dependency'
     */
    SourceAndConverterBdvDisplayService bsds = null;

    /**
     * Map containing objects that are 1 to 1 linked to a Source
     * Keys are Weakly referenced -> Metadata should be GCed if referenced only here
     */
    Cache<SourceAndConverter, Map<String, Object>> sacToMetadata;

    /**
     * Test if a Source is already registered in the Service
     * @param src
     * @return
     */
    public boolean isRegistered(SourceAndConverter src) {
        return sacToMetadata.getIfPresent(src)!=null;
    }

    public void setDisplayService( SourceAndConverterBdvDisplayService bsds) {
        assert bsds instanceof SourceAndConverterBdvDisplayService;
        this.bsds = bsds;
    }

    @Override
    public void setMetadata( SourceAndConverter sac, String key, Object data )
    {
        if (sac == null) {
            System.err.println("Error : sac is null in setMetadata function! ");
            //return;
        }
        if (sacToMetadata.getIfPresent( sac ) == null) {
            System.err.println("Error : sac has no associated metadata ! This should not happen. ");
            System.err.println("Sac : "+sac.getSpimSource().getName());
            System.err.println("SpimSource class: "+sac.getSpimSource().getClass().getSimpleName());
            //return;
        }
        sacToMetadata.getIfPresent( sac ).put( key, data );
    }

    @Override
    public Object getMetadata( SourceAndConverter sac, String key )
    {
        if (sacToMetadata.getIfPresent(sac)!=null) {
            return sacToMetadata.getIfPresent(sac).get(key);
        } else {
            return null;
        }
    }

    @Override
    public Collection<String> getMetadataKeys(SourceAndConverter sac) {
        Map<String, Object> map = sacToMetadata.getIfPresent(sac);
        if (map==null) {
            return new ArrayList<String>();
        } else {
            return map.keySet();
        }
    }

    @Override
    public boolean containsMetadata(SourceAndConverter sac, String key) {
        return getMetadata(sac,key)!=null;
    }

    /**
     * Register a BDV Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param sac
     */
    public synchronized void register(SourceAndConverter sac) {
        if (objectService.getObjects(SourceAndConverter.class).contains(sac)) {
            log.accept("Source already registered");
            return;
        }
        if (!(sacToMetadata.getIfPresent(sac)!=null)) {
            Map<String, Object> sourceData = new HashMap<>();
            sacToMetadata.put(sac, sourceData);
        }
        objectService.addObject(sac);
        if (uiAvailable) ui.update(sac);
    }

    public synchronized void register(Collection<SourceAndConverter> sources) {
        for (SourceAndConverter sac:sources) {
            this.register(sac);
        }
    }

    public synchronized void register(AbstractSpimData asd) {

        if (spimdataToMetadata.getIfPresent(asd)==null) {
            Map<String, Object> sourceData = new HashMap<>();
            spimdataToMetadata.put(asd, sourceData);
        }

        Map<Integer, SourceAndConverter> sacs = SourceAndConverterUtils.createSourceAndConverters(asd);
        this.register(sacs.values());
        sacs.forEach((id,sac) -> {
            this.linkToSpimData(sac, asd, id);
            if (uiAvailable) ui.update(sac);
        });
    }

    @Override
    public synchronized void remove(SourceAndConverter... sacs ) {
        // Remove displays
        if (sacs != null) {
            if (bsds!=null) {
                bsds.removeFromAllBdvs( sacs );
            }
            for (SourceAndConverter sac : sacs) {
                // Checks if it's the last of a spimdataset -> should shutdown cache
                // ----------------------------
                AbstractSpimData asd = null;
                if (sacToMetadata.getIfPresent(sac)!=null) {
                    if (sacToMetadata.getIfPresent(sac).get(SPIM_DATA_INFO) != null) {
                        asd = ((SpimDataInfo) (sacToMetadata.getIfPresent(sac).get(SPIM_DATA_INFO))).asd;
                    }

                    if (asd != null) {
                        if (this.getSourceAndConverterFromSpimdata(asd).size() == 1) {
                            // Last one! Time to invalidate the cache (if there's one, meaning, if the image loader
                            // is a ViewerImageLoader)

                            if (asd.getSequenceDescription().getImgLoader() instanceof ViewerImgLoader) {
                                ViewerImgLoader imgLoader = (ViewerImgLoader) (asd.getSequenceDescription().getImgLoader());
                                if (imgLoader.getCacheControl() instanceof VolatileGlobalCellCache) {
                                    ((VolatileGlobalCellCache) (imgLoader.getCacheControl())).clearCache();
                                }
                            }
                        }
                    }
                    //----------------

                    sacToMetadata.invalidate(sac);
                } else {
                    errlog.accept(sac.getSpimSource().getName() + " has no associated metadata");
                }
                objectService.removeObject(sac);
                if (uiAvailable) {
                    ui.remove(sac);
                }
            }
            //System.out.println("Sources left = "+this.getSourceAndConverters().size());
        }
    }

    @Override
    public List<SourceAndConverter> getSourceAndConverters() {
        return objectService.getObjects(SourceAndConverter.class);
    }

    @Override
    public List<SourceAndConverter> getSourceAndConverterFromSpimdata(AbstractSpimData asd) {
        return objectService.getObjects(SourceAndConverter.class)
                .stream()
                .filter(s -> ((SpimDataInfo)sacToMetadata.getIfPresent(s).get(SPIM_DATA_INFO)!=null))
                .filter(s -> ((SpimDataInfo)sacToMetadata.getIfPresent(s).get(SPIM_DATA_INFO)).asd.equals(asd))
                .collect(Collectors.toList());
    }

    public void linkToSpimData( SourceAndConverter sac, AbstractSpimData asd, int idSetup) {
        sacToMetadata.getIfPresent( sac ).put( SPIM_DATA_INFO, new SpimDataInfo(asd,idSetup));
    }


    /**
     * Swing UI for this Service, exists only if an UI is available in the current execution context
     */
    SourceAndConverterServiceUI ui;

    /**
     * Flags if the Inner UI exists
     */
    boolean uiAvailable = false;

    public SourceAndConverterServiceUI getUI() {
        return ui;
    }

    /**
     * Service initialization
     */
    @Override
    public void initialize() {
        scriptService.addAlias(SourceAndConverter.class);
        // -- TODO Check whether it's a good idea to add this here
        scriptService.addAlias(RealTransform.class);
        scriptService.addAlias(AffineTransform3D.class);
        scriptService.addAlias(AbstractSpimData.class);
        // -- TODO End of to check
        sacToMetadata = CacheBuilder.newBuilder().weakKeys().build();//new HashMap<>();
        spimdataToMetadata = CacheBuilder.newBuilder().weakKeys().build();

        registerDefaultActions();
        if (uiService!=null) {
            log.accept("uiService detected : Constructing JPanel for BdvSourceAndConverterService");
            ui = new SourceAndConverterServiceUI(this);
            uiAvailable = true;
        }

        SourceAndConverterServices.setSourceAndConverterService(this);
        log.accept("Service initialized.");
    }

    public List<SourceAndConverter> getSourceAndConvertersFromSource(Source src) {
        return getSourceAndConverters().stream().filter( sac -> sac.getSpimSource().equals(src)).collect(Collectors.toList());
    }


    Map<String, Consumer<SourceAndConverter[]>> actionMap = new ConcurrentHashMap<>();

    public void registerAction(String actionName, Consumer<SourceAndConverter[]> action) {
        if (actionMap.containsKey(actionName)) {
            System.err.println("Overriding action "+actionName);
        }
        actionMap.put(actionName, action);
    }

    public void removeAction(String actionName) {
        actionMap.remove(actionName);
    }

    public Consumer<SourceAndConverter[]> getAction(String actionName) {
        return actionMap.get(actionName);
    }

    @Override
    public Set<AbstractSpimData> getSpimDatasets() {
        Set<AbstractSpimData> asds = new HashSet<>();
        this.getSourceAndConverters().forEach(sac -> {
            if (containsMetadata(sac, SPIM_DATA_INFO)) {
                asds.add(((SpimDataInfo)getMetadata(sac, SPIM_DATA_INFO)).asd);
            }
        });
        return asds;
    }

    /**
     * @return a list of of action name / keys / identifiers
     */
    public Set<String> getActionsKeys() {
        return actionMap.keySet();
    }

    final public static String getCommandName(Class<? extends Command> c) {
        String menuPath = c.getDeclaredAnnotation(Plugin.class).menuPath();
        return menuPath.substring(menuPath.lastIndexOf(">")+1);
    }

    void registerDefaultActions() {
        this.registerAction("Display names", (srcs) -> {
            for (SourceAndConverter src:srcs){
                System.out.println(src.getSpimSource().getName());
            }});
        commandService.getCommands().forEach(ci -> {
            registerScijavaCommandInfo(ci);
        });

        // BDV add and remove
        /*registerScijavaCommand(BdvSourcesAdderCommand.class);
        registerScijavaCommand(BdvSourcesShowCommand.class);
        registerScijavaCommand(BdvSourcesRemoverCommand.class);
        registerScijavaCommand(SourcesInvisibleMakerCommand.class);
        registerScijavaCommand(SourcesVisibleMakerCommand.class);
        registerScijavaCommand(BrightnessAdjusterCommand.class);
        registerScijavaCommand(SourceColorChangerCommand.class);
        registerScijavaCommand(SourceAndConverterProjectionModeChangerCommand.class);
        registerScijavaCommand(SourcesDuplicatorCommand.class);
        registerScijavaCommand(ManualTransformCommand.class);
        registerScijavaCommand(TransformedSourceWrapperCommand.class);
        registerScijavaCommand(ColorSourceCreatorCommand.class);
        registerScijavaCommand(LUTSourceCreatorCommand.class);
        registerScijavaCommand(SourcesRemoverCommand.class);
        registerScijavaCommand(XmlHDF5ExporterCommand.class);
        registerScijavaCommand(ScreenShotMakerCommand.class);
        registerScijavaCommand(BasicTransformerCommand.class);*/

        // registerScijavaCommand(SourcesResamplerCommand.class); Too many arguments -> need to define which one is used
        registerAction(getCommandName(SourcesResamplerCommand.class),
                (sacs) -> {
                    //try {
                    commandService.run(SourcesResamplerCommand.class, true, "sourcesToResample", sacs);//.get();
                    //} catch (InterruptedException e) {
                    //    e.printStackTrace();
                    //} catch (ExecutionException e) {
                    //    e.printStackTrace();
                    //}
                });

        /*File f = new File("bdvpgsettings"+File.separator+"ActionPackages.json");
        if (f.exists()) {
            try {
                Gson gson = new Gson();
                String[] extraActions = gson.fromJson(new FileReader(f.getAbsoluteFile()), String[].class);

                for (String actionClass : extraActions) {
                    registerScijavaCommandInfo(commandService.getCommand(actionClass));
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }*/

    }

    @Parameter
    CommandService commandService;

    static String[] allowedPackagesPaths = null;
    static String[] defaultAllowedPackagesPaths = {"command:sc.fiji.bdvpg"};//, "command:ch.epfl.biop"};

    static {
        File f = new File("bdvpgsettings"+File.separator+"ActionPackages.json");
        if (f.exists()) {
            try {
                Gson gson = new Gson();
                allowedPackagesPaths = gson.fromJson(new FileReader(f.getAbsoluteFile()), String[].class);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isContainedInPackagesToRegister(String commandClass) {

        for (String packagePath : defaultAllowedPackagesPaths) {
            if (commandClass.startsWith(packagePath)) {
                //System.out.println("is "+commandClass+" contained ? Yes");
                return true;
            }
        }

        if (allowedPackagesPaths!=null) {
            for (String packagePath : allowedPackagesPaths) {
                if (commandClass.startsWith(packagePath)) {
                    //System.out.println(" Yes");
                    return true;
                }
            }
        }

        //System.out.println(" No");
        return false;
    }

    public void registerScijavaCommandInfo(CommandInfo ci) {
        int nCountSourceAndConverter = 0;
        int nCountSourceAndConverterList = 0;
        // ci.getDelegateClassName();
        //System.out.println(ci.getIdentifier());
        //isContainedInPackagesToRegister(ci.getIdentifier());
        // System.out.println(ci.getTitle());
        try {
            if (isContainedInPackagesToRegister(ci.getIdentifier()) && (ci.inputs() != null)) {
                for (ModuleItem input : ci.inputs()) {
                    if (input.getType().equals(SourceAndConverter.class)) {
                        nCountSourceAndConverter++;
                    }
                    if (input.getType().equals(SourceAndConverter[].class)) {
                        nCountSourceAndConverterList++;
                    }
                }
                if (nCountSourceAndConverter + nCountSourceAndConverterList == 1) {
                    // Can be automatically mapped to popup action
                    for (ModuleItem input : ci.inputs()) {
                        if (input.getType().equals(SourceAndConverter.class)) {
                            // It's an action which takes a SourceAndConverter
                            registerAction(ci.getTitle(),
                                    (sacs) -> {
                                        // Todo : improve by sending the parameters all over again
                                        //try {
                                        for (SourceAndConverter sac : sacs) {
                                            commandService.run(ci, true, input.getName(), sac);//.get(); TODO understand why get is impossible
                                        }
                                        //} catch (InterruptedException e) {
                                        //    e.printStackTrace();
                                        //} catch (ExecutionException e) {
                                        //    e.printStackTrace();
                                        //}
                                    });

                            log.accept("Registering action entitled " + ci.getTitle() + " from command " + ci.getClassName());

                        }
                        if (input.getType().equals(SourceAndConverter[].class)) {
                            // It's an action which takes a SourceAndConverter List
                            registerAction(ci.getTitle(),
                                    (sacs) -> {
                                        //try {
                                        commandService.run(ci, true, input.getName(), sacs);//.get();
                                        //} catch (InterruptedException e) {
                                        //    e.printStackTrace();
                                        //} catch (ExecutionException e) {
                                        //    e.printStackTrace();
                                        //}
                                    });
                            log.accept("Registering action entitled " + ci.getTitle() + " from command " + ci.getClassName());
                        }
                    }
                } else {
                    registerAction(ci.getTitle(),
                            (sacs) -> {
                                //try {
                                commandService.run(ci, true);//.get();
                                //} catch (InterruptedException e) {
                                //    e.printStackTrace();
                                //} catch (ExecutionException e) {
                                //    e.printStackTrace();
                                //}
                            });
                    log.accept("Registering action entitled " + ci.getTitle() + " from command " + ci.getClassName() + " sacs ignored");
                }
            }
        } catch (NullPointerException npe) {
            errlog.accept("Error on scijava commands registrations");
            errlog.accept("Null Pointer Exception for command '"+ci.getTitle()+"'");
            errlog.accept("Try to exclude this command by modifying ActionPackages.json file");
            errlog.accept("class : "+ci.getClassName());
        } catch (Exception e) {
            errlog.accept("Error on scijava commands registrations");
            errlog.accept("Exception for command "+ci.getTitle());
            errlog.accept("Try to exclude this command by modifying ActionPackages.json file");
            errlog.accept("class : "+ci.getClassName());
            e.printStackTrace();
        }
    }

    public void registerScijavaCommand(Class<? extends Command> commandClass ) {
        registerScijavaCommandInfo(commandService.getCommand(commandClass));
    }

    //------------------- SpimData specific informations

   public class SpimDataInfo {

        public final AbstractSpimData asd;
        public int setupId;

        public SpimDataInfo(AbstractSpimData asd, int setupId) {
            this.asd = asd;
            this.setupId =setupId;
        }

        public String toString() {
            return asd.toString()+": setupId = "+setupId;
        }
   }

    /**
     * Map containing objects that are 1 to 1 linked to a Source
     * Keys are Weakly referenced -> Metadata should be GCed if referenced only here
     */
    Cache<AbstractSpimData, Map<String, Object>> spimdataToMetadata;

    // Key to the string linking to the SpimData (either last save or last loading)
    // {@link XmlFromSpimDataExporter}

    public synchronized void setSpimDataName(AbstractSpimData asd, String name) {
        spimdataToMetadata.getIfPresent(asd).put("NAME", name);
        if (uiAvailable) ui.updateSpimDataName(asd, name);
    }

    @Override
    public void setMetadata( AbstractSpimData asd, String key, Object data )
    {
        if (asd == null) {
            System.err.println("Error : asd is null in setMetadata function! ");
            return;
        }
        if (spimdataToMetadata.getIfPresent(asd)==null) {
            Map<String, Object> sourceData = new HashMap<>();
            spimdataToMetadata.put(asd, sourceData);
        }
        spimdataToMetadata.getIfPresent( asd ).put( key, data );
    }

    @Override
    public Object getMetadata( AbstractSpimData asd, String key )
    {
        if (spimdataToMetadata.getIfPresent(asd)!=null) {
            return spimdataToMetadata.getIfPresent(asd).get(key);
        } else {
            return null;
        }
    }

    @Override
    public Collection<String> getMetadataKeys(AbstractSpimData asd) {
        Map<String, Object> map = spimdataToMetadata.getIfPresent(asd);
        if (map==null) {
            return new ArrayList<String>();
        } else {
            return map.keySet();
        }
    }

    @Override
    public boolean containsMetadata(AbstractSpimData asd, String key) {
        return getMetadata(asd,key)!=null;
    }

}
