package sc.fiji.bdvpg.scijava.services;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import sc.fiji.bdvpg.services.IBdvSourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * Scijava Service which centralizes Bdv Sources, independently of their display
 * Bdv Sources can be registered to this Service.
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
public class BdvSourceAndConverterService extends AbstractService implements SciJavaService, IBdvSourceAndConverterService {

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println(BdvSourceAndConverterService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println(BdvSourceAndConverterService.class.getSimpleName()+":"+str);

    /**
     * Scijava Object Service : will contain all the sourceAndConverters
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
    Map<SourceAndConverter, Map<String, Object>> data;

    /**
     * Reserved key for the data map. data.get(sourceandconverter).get(SETSPIMDATA)
     * is expected to return a List of Spimdata Objects which refer to this sourceandconverter
     * whether a list of necessary is not obvious at the moment
     * TODO : make an example
     */
    final public static String SETSPIMDATA = "SETSPIMDATA";

    /**
     * Test if a Source is already registered in the Service
     * @param src
     * @return
     */
    public boolean isRegistered(SourceAndConverter src) {
        return data.containsKey(src);
    }


    /**
     * Gets lists of associated objects and data attached to a Bdv Source
     * @return
     */
    @Override
    public Map<SourceAndConverter, Map<String, Object>> getAttachedSourceAndConverterData() {
        return data;
    }

    /**
     * Register a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    public void register(SourceAndConverter src) {
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

    public void register(List<SourceAndConverter> sources) {
        for (SourceAndConverter sac:sources) {
            this.register(sac);
        }
    }

    public void register(AbstractSpimData asd) {
        List<SourceAndConverter> sacs = SourceAndConverterUtils.createSourceAndConverters(asd);
        this.register(sacs);
        sacs.forEach(sac -> this.linkToSpimData(sac, asd));
    }

    @Override
    public void remove(SourceAndConverter src) {
        // TODO!
        errlog.accept("Removal of SourceAndConverter unsupported yet");
    }


    @Override
    public List<SourceAndConverter> getSources() {
        return objectService.getObjects(SourceAndConverter.class);
    }

    @Override
    public List<SourceAndConverter> getSourceAndConverterFromSpimdata(AbstractSpimData asd) {
        return objectService.getObjects(SourceAndConverter.class)
                .stream()
                .filter(s -> ((HashSet<AbstractSpimData>)data.get(s).get(SETSPIMDATA)).contains(asd))
                .collect(Collectors.toList());
    }

    public void linkToSpimData(SourceAndConverter src, AbstractSpimData asd) {
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
        scriptService.addAlias(SourceAndConverter.class);
        // -- TODO Check whether it's a good idea to add this here
        scriptService.addAlias(RealTransform.class);
        scriptService.addAlias(AffineTransform3D.class);
        scriptService.addAlias(AbstractSpimData.class);
        // -- End of to check$

        data = new HashMap<>();
        if (uiService!=null) {
            log.accept("uiService detected : Constructing JPanel for BdvSourceAndConverterService");
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

        BdvSourceAndConverterService bss;
        JFrame frame;
        JPanel panel;
        /**
         * Swing JTree used for displaying Sources object
         */
        JTree tree;
        DefaultMutableTreeNode top;
        JScrollPane treeView;
        DefaultTreeModel model;
        Set<SourceAndConverter> displayedSource = new HashSet<>();

        public UIBdvSourceService(BdvSourceAndConverterService bss) {
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

        public void update(SourceAndConverter src) {
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
