package sc.fiji.bdvpg.scijava.services;

import bdv.SpimSource;
import bdv.ViewerImgLoader;
import bdv.VolatileSpimSource;
import bdv.viewer.Source;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import sc.fiji.bdvpg.services.IBdvSourceService;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.scijava.services.BdvSourceDisplayService.VOLATILESOURCE;

/**
 * Scijava Service which centralizes Bdv Sources, independently of their display
 * Bdv Sources can be registered to this Service.
 * This service adds the Source to the ObjectService, but on top of it,
 * It contains a Map which contains any object which can be linked to the source.
 *
 * It also handles SpimData object, but split all Sources into individual ones
 *
 * The most interesting of these objects are actually created by the BdvSourceDisplayService:
 * - Converter to ARGBType, ConverterSetup, and Volatile view
 *
 * TODO : Think more carefully : maybe the volatile source should be done here...
 * Because when multiply wrapped source end up here, it maybe isn't possible to make the volatile view
 */

@Plugin(type=Service.class)
public class BdvSourceService extends AbstractService implements SciJavaService, IBdvSourceService {

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println(BdvSourceService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println(BdvSourceService.class.getSimpleName()+":"+str);

    /**
     * Scijava Object Service : will contain all the sources
     */
    @Parameter
    private ObjectService objectService;

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
     * Map containing objects that are 1 to 1 linked to a Source
     * TODO : ask if it should contain a WeakReference to Source keys (Potential Memory leak ?)
     */
    Map<Source, Map<String, Object>> data;

    /**
     * Reserved key for the data map. data.get(source).get(SETSPIMDATA)
     * is expected to return a List of Spimdata Objects which refer to this source
     * whether a list of necessary is not obvious at the moment
     * TODO : make an example
     */
    final public static String SETSPIMDATA = "SETSPIMDATA";

    /**
     * Test if a Source is already registered in the Service
     * @param src
     * @return
     */
    public boolean isRegistered(Source src) {
        return data.containsKey(src);
    }


    /**
     * Gets lists of associated objects and data attached to a Bdv Source
     * @return
     */
    public Map<Source, Map<String, Object>> getAttachedSourceData() {
        return data;
    }

    /**
     * Register a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    public void register(Source src) {
        if (data.containsKey(src)) {
            log.accept("Source already registered");
            return;
        }
        final int numTimepoints = 1;
        Map<String, Object> sourceData = new HashMap<>();
        data.put(src, sourceData);
        objectService.addObject(src);
        if (uiAvailable) ui.update(src);
    }

    public void register(AbstractSpimData asd) {
        final AbstractSequenceDescription< ?, ?, ? > seq = asd.getSequenceDescription();
        final ViewerImgLoader imgLoader = ( ViewerImgLoader ) seq.getImgLoader();
        for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() )
        {
            final int setupId = setup.getId();
            final Object type = imgLoader.getSetupImgLoader( setupId ).getImageType();
            if ( RealType.class.isInstance( type ) ) {
                String sourceName = createSetupName(setup);
                final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );
                final SpimSource s = vs.nonVolatile();
                register(s,vs);
                linkToSpimData(s,asd);
            } else if ( ARGBType.class.isInstance( type ) ) {
                //TODO
                errlog.accept("Cannot open Spimdata with Source of Type ARGBType");
            } else {
                errlog.accept("Cannot open Spimdata with Source of type "+type.getClass().getSimpleName());
            }
        }
    }

    private static String createSetupName( final BasicViewSetup setup )
    {
        if ( setup.hasName() )
            return setup.getName();

        String name = "";

        final Angle angle = setup.getAttribute( Angle.class );
        if ( angle != null )
            name += ( name.isEmpty() ? "" : " " ) + "a " + angle.getName();

        final Channel channel = setup.getAttribute( Channel.class );
        if ( channel != null )
            name += ( name.isEmpty() ? "" : " " ) + "c " + channel.getName();

        return name;
    }

    @Override
    public void remove(Source src) {
        // TODO!
        errlog.accept("Removal of Source unsupported yet");
    }

    /**
     * Register a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    public void register(Source src, Source vsrc) {
        if (data.containsKey(src)) {
            log.accept("Source already registered");
            return;
        }
        final int numTimepoints = 1;
        Map<String, Object> sourceData = new HashMap<>();
        data.put(src, sourceData);
        sourceData.put(VOLATILESOURCE, vsrc);
        objectService.addObject(src);
        if (uiAvailable) ui.update(src);
    }

    @Override
    public List<Source> getSources() {
        return objectService.getObjects(Source.class);
    }

    @Override
    public List<Source> getSourcesFromSpimdata(AbstractSpimData asd) {
        return objectService.getObjects(Source.class)
                .stream()
                .filter(s -> ((HashSet<AbstractSpimData>)data.get(s).get(SETSPIMDATA)).contains(asd))
                .collect(Collectors.toList());
    }

    public void linkToSpimData(Source src, AbstractSpimData asd) {
        // TODO
        if (data.get(src).get(SETSPIMDATA)==null) {
            data.get(src).put(SETSPIMDATA, new HashSet<>());
        }
        ((Set)data.get(src).get(SETSPIMDATA)).add(asd);
    }


    /**
     * Inner Swing UI for this Service, exists only if an UI is available in the current execution context
     */
    UIBdvSourceService ui;

    /**
     * Flags if the Inner UI exists
     */
    boolean uiAvailable = false;

    public UIBdvSourceService getUI() {
        return ui;
    }

    /**
     * Service initialization
     */
    @Override
    public void initialize() {
        scriptService.addAlias(Source.class);
        // -- TODO Check whether it's a good idea to add this here
        scriptService.addAlias(RealTransform.class);
        scriptService.addAlias(AffineTransform3D.class);
        scriptService.addAlias(AbstractSpimData.class);
        // -- End of to check$

        data = new HashMap<>();
        if (uiService!=null) {
            log.accept("uiService detected : Constructing JPanel for BdvSourceService");
            ui = new UIBdvSourceService(this);
            uiAvailable = true;
        }
        log.accept("Service initialized.");
    }

    /**
     * Inner Swing UI for Bdv Source
     * Really really basic at the moment
     * TODO : improve UI
     */
    public class UIBdvSourceService {

        BdvSourceService bss;
        JFrame frame;
        JPanel panel;
        /**
         * Swing JTree used for displaying Sources object
         */
        JTree tree;
        DefaultMutableTreeNode top;
        JScrollPane treeView;
        DefaultTreeModel model;
        Set<Source> displayedSource = new HashSet<>();

        public UIBdvSourceService(BdvSourceService bss) {
                this.bss = bss;
                frame = new JFrame("Bdv Sources");
                panel = new JPanel(new BorderLayout());

                //textArea = new JTextArea();
                //textArea.setText("Bdv Sources will be displayed here.");
                //panel.add(textArea);


                // Tree view of Spimdata
                top = new DefaultMutableTreeNode("Sources");
                tree = new JTree(top);

                //tree.setRootVisible(false);

                model = (DefaultTreeModel)tree.getModel();
                treeView = new JScrollPane(tree);

                panel.add(treeView, BorderLayout.CENTER);
                frame.add(panel);
                frame.pack();
                frame.setVisible(true);
        }

        public void update(Source src) {
            if (displayedSource.contains(src)) {
                // No Need to update
            } else {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(src);
                top.add(node);
                model.reload(top);
                panel.revalidate();
                displayedSource.add(src);
            }
        }
    }

}
