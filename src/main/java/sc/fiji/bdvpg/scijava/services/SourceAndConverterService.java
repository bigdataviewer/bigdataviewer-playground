/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.bdvpg.scijava.services;

import bdv.SpimSource;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.VolatileSpimSource;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.scijava.InstantiableException;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.spimdata.EntityHandler;
import sc.fiji.bdvpg.spimdata.IEntityHandlerService;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService.CONVERTER_SETUP;

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
 * */

@Plugin(type=Service.class, headless = true)
public class SourceAndConverterService extends AbstractService implements SciJavaService, ISourceAndConverterService
{

    protected static Logger logger = LoggerFactory.getLogger(SourceAndConverterService.class);

    static {
        LegacyInjector.preinit();
    }

    /**
     * Standard logger
     */
    public Consumer<String> log = logger::debug;

    /**
     * Error logger
     */
    public Consumer<String> errlog = logger::error;

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
     * @param src source
     * @return true if a source is already registered
     */
    public boolean isRegistered(SourceAndConverter src) {
        return sacToMetadata.getIfPresent(src)!=null;
    }

    public void setDisplayService( SourceAndConverterBdvDisplayService bsds) {
        this.bsds = bsds;
    }

    @Override
    public void setMetadata( SourceAndConverter sac, String key, Object data )
    {
        if (sac == null) {
            logger.error("Error : sac is null in setMetadata function! ");
            return;
        }

        if (sacToMetadata.getIfPresent( sac ) == null) {
            logger.error("Error : sac has no associated metadata ! This should not happen. ");
            logger.error("Sac : "+sac.getSpimSource().getName());
            logger.error("SpimSource class: "+sac.getSpimSource().getClass().getSimpleName());
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
    public void removeMetadata(SourceAndConverter sac, String key) {
        Map<String,Object> metadata = sacToMetadata.getIfPresent(sac);
        if (metadata!=null) {
            metadata.remove(key);
        }
    }

    @Override
    public Collection<String> getMetadataKeys(SourceAndConverter sac) {
        Map<String, Object> map = sacToMetadata.getIfPresent(sac);
        if (map==null) {
            return new ArrayList<>();
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
     * @param sac source
     */
    public synchronized void register(SourceAndConverter sac) {
        if (objectService.getObjects(SourceAndConverter.class).contains(sac)) {
            log.accept("Source already registered");
            return;
        }
        if (sacToMetadata.getIfPresent(sac) == null) {
            Map<String, Object> sourceData = new HashMap<>();
            sacToMetadata.put(sac, sourceData);
        }
        /*
          TODO FIX
          Problematic behaviour... several thread deadlocks experienced in ABBA because
          of the line below, if not put in invokelater. Example deadlock:
          invokeLater may fix this, but this does not feel right...
            "ForkJoinPool.commonPool-worker-7" #156 daemon prio=6 os_prio=0 tid=0x00000171bd131800 nid=0x3b4c in Object.wait() [0x0000000a72afe000]
               java.lang.Thread.State: WAITING (on object monitor)
                    at java.lang.Object.wait(Native Method)
                    at java.lang.Object.wait(Object.java:502)
                    at java.awt.EventQueue.invokeAndWait(EventQueue.java:1343)
                    - locked <0x000000077aebb9b8> (a java.awt.EventQueue$1AWTInvocationLock)
                    at java.awt.EventQueue.invokeAndWait(EventQueue.java:1324)
                    at org.scijava.thread.DefaultThreadService.invoke(DefaultThreadService.java:115)
                    at org.scijava.event.DefaultEventBus.publishNow(DefaultEventBus.java:182)
                    at org.scijava.event.DefaultEventBus.publishNow(DefaultEventBus.java:73)
                    at org.scijava.event.DefaultEventService.publish(DefaultEventService.java:102)
                    at org.scijava.object.ObjectService.addObject(ObjectService.java:92)
                    at org.scijava.object.ObjectService.addObject(ObjectService.java:86)
                    at sc.fiji.bdvpg.scijava.services.SourceAndConverterService.register(SourceAndConverterService.java:235)
         */
        objectService.addObject(sac);
        /*AtomicBoolean flagPerformed2 = new AtomicBoolean();
        flagPerformed.set(false);
        EventQueue.invokeLater(()-> {

            flagPerformed.set(true);
        });
        while (!flagPerformed.get()) {
            // busy waiting
        }*/

        if (uiAvailable) ui.update(sac);
    }

    public synchronized void register(Collection<SourceAndConverter> sources) {
        for (SourceAndConverter sac:sources) {
            this.register(sac);
        }
    }

    @Parameter
    IEntityHandlerService entityHandlerService;

    /**
     * TODO nice documentation about {@link EntityHandler} and scijava extension mechanism
     * @param asd spimdata object to register
     */
    public synchronized void register(AbstractSpimData asd) {

        if (spimdataToMetadata.getIfPresent(asd)==null) {
            Map<String, Object> sourceData = new HashMap<>();
            spimdataToMetadata.put(asd, sourceData);
        }

        Map<Class<? extends Entity>, EntityHandler> entityClassToHandler = new HashMap<>();

        entityHandlerService.getHandlers(EntityHandler.class).forEach(pi -> {
            try {
                EntityHandler handler = pi.createInstance();
                entityClassToHandler.put(handler.getEntityType(), handler);
                log.accept("Plugin found for entity class "+handler.getEntityType().getSimpleName());
            } catch (InstantiableException e) {
                e.printStackTrace();
            }
        });

        boolean nonVolatile = WrapBasicImgLoader.wrapImgLoaderIfNecessary( asd );

        if ( nonVolatile )
        {
            logger.warn( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
        }

        final AbstractSequenceDescription< ?, ?, ? > seq = asd.getSequenceDescription();

        final ViewerImgLoader imgLoader = ( ViewerImgLoader ) seq.getImgLoader();

        final Map< Integer, SourceAndConverter > setupIdToSourceAndConverter = new HashMap<>();

        for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() ) {

            // Execute {@link EntityHandler}, if a compatible entity is found in the spimdata, compatible with an entity class handler
            entityClassToHandler.keySet().forEach(entityClass -> {
                Entity e = setup.getAttribute(entityClass);
                if (e!=null) {
                    entityClassToHandler.get(entityClass).loadEntity(asd, setup);
                }
            });

            final int setupId = setup.getId();

            ViewerSetupImgLoader vsil = imgLoader.getSetupImgLoader(setupId);

            String sourceName = createSetupName(setup);

            final Object type = vsil.getImageType();

            entityClassToHandler.keySet().forEach(entityClass -> {
                Entity e = setup.getAttribute(entityClass);
                if (e!=null) {
                    if (entityClassToHandler.get(entityClass).canCreateSourceAndConverter()) {
                        setupIdToSourceAndConverter.put(setupId, entityClassToHandler.get(entityClass).makeSourceAndConverter(asd, setup));
                    }
                }
            });

            if (!setupIdToSourceAndConverter.containsKey(setupId)) {
                if (type instanceof RealType) {

                    //createRealTypeSourceAndConverter( nonVolatile, setupId, sourceName );
                    final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

                    Converter nonVolatileConverter = SourceAndConverterHelper.createConverterRealType((RealType)s.getType()); // IN FACT THE CASTING IS NECESSARY!!

                    if (!nonVolatile ) {

                        final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );

                        Converter volatileConverter = SourceAndConverterHelper.createConverterRealType((RealType)vs.getType());

                        setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter, new SourceAndConverter<>(vs, volatileConverter)));

                    } else {

                        setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter));
                    }

                } else if (type instanceof ARGBType) {

                    //createARGBTypeSourceAndConverter( setupId, sourceName );
                    final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );
                    final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

                    Converter nonVolatileConverter = SourceAndConverterHelper.createConverterARGBType(s);

                    Converter volatileConverter = SourceAndConverterHelper.createConverterARGBType(vs);
                    setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter, new SourceAndConverter<>(vs, volatileConverter)));

                } else {
                    SourceAndConverterHelper.errlog.accept("Cannot open Spimdata with Source of type "+type.getClass().getSimpleName());
                }

            }

            // Execute {@link EntityHandler}, if a compatible entity is found
            entityClassToHandler.keySet().forEach(entityClass -> {
                Entity e = setup.getAttribute(entityClass);
                if (e!=null) {
                    entityClassToHandler.get(entityClass).loadEntity(asd, setup, setupIdToSourceAndConverter.get(setupId) );
                }
            });
        }

        setupIdToSourceAndConverter.keySet().forEach(id -> {
            register(setupIdToSourceAndConverter.get(id));
            linkToSpimData(setupIdToSourceAndConverter.get(id), asd, id);
            if (uiAvailable) ui.update(setupIdToSourceAndConverter.get(id));
        });

        WrapBasicImgLoader.removeWrapperIfPresent( asd );

    }

    private static String createSetupName( final BasicViewSetup setup ) {
        if ( setup.hasName() ) {
            if (!setup.getName().trim().equals("")) {
                return setup.getName();
            }
        }

        String name = "";

        final Angle angle = setup.getAttribute( Angle.class );
        if ( angle != null )
            name += "a " + angle.getName();

        final Channel channel = setup.getAttribute( Channel.class );
        if ( channel != null )
            name += ( name.isEmpty() ? "" : " " ) + "c " + channel.getName();

        if ((channel == null)&&(angle == null)) {
            name += "id "+setup.getId();
        }

        return name;
    }

    /**
     * Gets or create the associated ConverterSetup of a Source
     * While several converters can be associated to a Source (volatile and non-volatile),
     * only one ConverterSetup is associated to a Source
     * @param sac source to get the convertersetup from
     * @return the converter setup of the source
     */
    public ConverterSetup getConverterSetup(SourceAndConverter sac) {
        if (!isRegistered(sac)) {
            register(sac);
        }

        // If no ConverterSetup is built then build it
        if ( sacToMetadata.getIfPresent(sac).get( CONVERTER_SETUP ) == null) {
            ConverterSetup setup = SourceAndConverterHelper.createConverterSetup(sac);
            sacToMetadata.getIfPresent(sac).put( CONVERTER_SETUP,  setup );
        }

        return (ConverterSetup) sacToMetadata.getIfPresent(sac).get( CONVERTER_SETUP );
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
                            if (asd.getSequenceDescription().getImgLoader() instanceof Closeable) {
                                try {
                                    ((Closeable) asd.getSequenceDescription().getImgLoader()).close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    //----------------

                    sacToMetadata.invalidate(sac);
                } else {
                    errlog.accept(sac.getSpimSource().getName() + " has no associated metadata");
                }
                /*
                  TODO FIX
                  Problematic behaviour... several thread deadlocks experienced in ABBA because
                  of the line below, if not put in invokelater. Example deadlock:
                  invokeLater may fix this, but this does not feel right...
                    "ForkJoinPool.commonPool-worker-7" #156 daemon prio=6 os_prio=0 tid=0x00000171bd131800 nid=0x3b4c in Object.wait() [0x0000000a72afe000]
                       java.lang.Thread.State: WAITING (on object monitor)
                            at java.lang.Object.wait(Native Method)
                            at java.lang.Object.wait(Object.java:502)
                            at java.awt.EventQueue.invokeAndWait(EventQueue.java:1343)
                            - locked <0x000000077aebb9b8> (a java.awt.EventQueue$1AWTInvocationLock)
                            at java.awt.EventQueue.invokeAndWait(EventQueue.java:1324)
                            at org.scijava.thread.DefaultThreadService.invoke(DefaultThreadService.java:115)
                            at org.scijava.event.DefaultEventBus.publishNow(DefaultEventBus.java:182)
                            at org.scijava.event.DefaultEventBus.publishNow(DefaultEventBus.java:73)
                            at org.scijava.event.DefaultEventService.publish(DefaultEventService.java:102)
                            at org.scijava.object.ObjectService.addObject(ObjectService.java:92)
                            at org.scijava.object.ObjectService.addObject(ObjectService.java:86)
                            at sc.fiji.bdvpg.scijava.services.SourceAndConverterService.register(SourceAndConverterService.java:235)
                 */
                objectService.removeObject(sac);
                /*
                Does not work
                AtomicBoolean flagPerformed = new AtomicBoolean();
                flagPerformed.set(false);
                EventQueue.invokeLater(()-> {

                    flagPerformed.set(true);
                });
                while (!flagPerformed.get()) {
                    // busy waiting
                }*/

                if (uiAvailable) {
                    ui.remove(sac);
                }
            }
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
                .filter(s -> (sacToMetadata.getIfPresent(s).get(SPIM_DATA_INFO) !=null))
                .filter(s -> ((SpimDataInfo)sacToMetadata.getIfPresent(s).get(SPIM_DATA_INFO)).asd.equals(asd))
                .collect(Collectors.toList());
    }

    public void linkToSpimData( SourceAndConverter sac, AbstractSpimData asd, int idSetup) {
        sacToMetadata.getIfPresent( sac ).put( SPIM_DATA_INFO, new SpimDataInfo(asd, idSetup));
    }


    /**
     * Swing UI for this Service, exists only if a UI is available in the current execution context
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

        if (!context().getService(UIService.class).isHeadless()) {
            log.accept( "uiService detected : Constructing JPanel for BdvSourceAndConverterService" );
            ui = new SourceAndConverterServiceUI( this );
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
            logger.warn("Overriding action "+actionName);
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
     * @return a list of action name / keys / identifiers
     */
    public Set<String> getActionsKeys() {
        return actionMap.keySet();
    }

    public static String getCommandName(Class<? extends Command> c) {
        String menuPath = c.getDeclaredAnnotation(Plugin.class).menuPath();
        return menuPath.substring(menuPath.lastIndexOf(">")+1);
    }

    void registerDefaultActions() {
        this.registerAction("Display names", (srcs) -> {
            for (SourceAndConverter src:srcs){
                log.accept(src.getSpimSource().getName());
            }});

        context().getService(PluginService.class)
                .getPluginsOfType(BdvPlaygroundActionCommand.class)
                .forEach(ci ->
                    registerScijavaCommandInfo(commandService.getCommand(ci.getClassName()))
                );

    }

    @Parameter
    CommandService commandService;

    public void registerScijavaCommandInfo(CommandInfo ci) {
        int nCountSourceAndConverter = 0;
        int nCountSourceAndConverterList = 0;
        try {
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
                        registerAction(ci.getMenuPath().getLeaf().toString(),
                                (sacs) -> {
                                    // Todo : improve by sending the parameters all over again
                                    for (SourceAndConverter sac : sacs) {
                                        commandService.run(ci, true, input.getName(), sac);//.get(); TODO understand why get is impossible
                                    }
                                });
                        //log.accept("Registering action entitled " + ci.getMenuPath().getLeaf().toString() + " from command " + ci.getClassName());

                    }
                    if (input.getType().equals(SourceAndConverter[].class)) {
                        // It's an action which takes a SourceAndConverter List
                        registerAction(ci.getMenuPath().getLeaf().toString(),
                                (sacs) -> {
                                    commandService.run(ci, true, input.getName(), sacs);//.get();
                                });

                        //log.accept("Registering action entitled " + ci.getMenuPath().getLeaf().toString() + " from command " + ci.getClassName());
                    }
                }
            } else {
                registerAction(ci.getMenuPath().getLeaf().toString(),
                        (sacs) -> {
                            commandService.run(ci, true);
                        });
                log.accept("Registering action entitled " + ci.getMenuPath().getMenuString() + " from command " + ci.getClassName() + " sacs ignored");
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

    //------------------- SpimData specific information

   public static class SpimDataInfo {

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
            errlog.accept("Error : asd is null in setMetadata function! ");
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
            return new ArrayList<>();
        } else {
            return map.keySet();
        }
    }

    @Override
    public boolean containsMetadata(AbstractSpimData asd, String key) {
        return getMetadata(asd,key)!=null;
    }

    public static String getCommandName( Command command )
    {
        return command.getClass().getAnnotation( Plugin.class ).name();
    }
}
