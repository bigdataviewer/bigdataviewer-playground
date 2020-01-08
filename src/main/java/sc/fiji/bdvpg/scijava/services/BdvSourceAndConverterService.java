package sc.fiji.bdvpg.scijava.services;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import sc.fiji.bdvpg.services.IBdvSourceAndConverterDisplayService;
import sc.fiji.bdvpg.services.IBdvSourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
    BdvSourceAndConverterDisplayService bsds = null;

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


    public void setDisplayService(IBdvSourceAndConverterDisplayService bsds) {
        assert bsds instanceof BdvSourceAndConverterDisplayService;
        this.bsds = (BdvSourceAndConverterDisplayService) bsds;
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
        // Remove displays
        if (bsds!=null) {
            bsds.removeFromAllBdvs(src);
        }
        data.remove(src);
        objectService.removeObject(src);
        if (uiAvailable) {
            ui.remove(src);
        }
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
        // -- TODO End of to check
        data = new HashMap<>();
        if (uiService!=null) {
            log.accept("uiService detected : Constructing JPanel for BdvSourceAndConverterService");
            ui = new UIBdvSourceService(this);
            uiAvailable = true;
        }
        registerPopupActions();
        log.accept("Service initialized.");
    }

    //Map<String, Consumer<SourceAndConverter[]>> popupactions = new HashMap<>();

    public void registerPopupSourcesAction(Consumer<SourceAndConverter[]> action, String actionName) {
        if (uiAvailable) {
            //popupactions.put(actionName, action);
            ui.addPopupAction(action, actionName);
        }
    }

    @Parameter
    CommandService commandService;

    public void registerPopupActions() {
        this.registerPopupSourcesAction((srcs) -> {
            for (SourceAndConverter src:srcs){
                System.out.println(src.getSpimSource().getName());
            }}, "Display names");

        /**
         * Ok, Scijava stuff here -> Automated registration of Command into popup actions
         */
        // A there is a single input which is of Type SourceAndConverter or of type SourceAndConverter[],
        // Then we can automatically register it as a popup action
        commandService.getCommands().forEach(ci -> {
            int nCountSourceAndConverter = 0;
            int nCountSourceAndConverterList = 0;
            //System.out.println("Command:"+ci.getTitle());

            // I don't know what's wrong with these two commands...
            // TODO : understand the error to avoid other command failings
            if (!ci.getTitle().equals("Export Fused Sequence as XML/HDF5"))
            if (!ci.getTitle().equals("Export Spim Sequence as XML/HDF5"))
            if (ci.inputs()!=null) {
                for (ModuleItem input: ci.inputs()) {
                    if (input.getType().equals(SourceAndConverter.class)) {
                        nCountSourceAndConverter++;
                    }
                    if (input.getType().equals(SourceAndConverter[].class)) {
                        nCountSourceAndConverterList++;
                    }
                }
                if (nCountSourceAndConverter+nCountSourceAndConverterList==1) {
                    // Can be automatically mapped to popup action
                    for (ModuleItem input: ci.inputs()) {
                        if (input.getType().equals(SourceAndConverter.class)) {
                            // It's an action which takes a SourceAndConverter
                            registerPopupSourcesAction(
                                    (sacs) -> {
                                            // Todo : improve by sending the parameters all over again
                                            //try {
                                                for (SourceAndConverter sac:sacs) {
                                                    commandService.run(ci, true, input.getName(), sac);//.get(); TODO understand why get is impossible
                                                }
                                            /*} catch (InterruptedException e) {
                                                e.printStackTrace();
                                            } catch (ExecutionException e) {
                                                e.printStackTrace();
                                            }*/
                                    },
                                    ci.getTitle());

                            log.accept("Registering action entitled "+ci.getTitle()+" from command "+ci.getClassName());

                        }
                        if (input.getType().equals(SourceAndConverter[].class)) {
                            // It's an action which takes a SourceAndConverter List
                            registerPopupSourcesAction(
                                    (sacs) -> {
                                        //try {
                                            commandService.run(ci, true, input.getName(), sacs);//.get();
                                        /*} catch (InterruptedException e) {
                                            e.printStackTrace();
                                        } catch (ExecutionException e) {
                                            e.printStackTrace();
                                        }*/
                                    },
                                    ci.getTitle());
                            log.accept("Registering action entitled "+ci.getTitle()+" from command "+ci.getClassName());
                        }
                    }
                }
            }
        });



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


        JPopupMenu popup = new JPopupMenu();

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

                // JTree of SpimData
                tree.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        super.mouseClicked(e);
                        if (SwingUtilities.isRightMouseButton(e)) {
                            popup.show(e.getComponent(), e.getX(), e.getY());
                        }
                        if (e.getClickCount()==2 && !e.isConsumed()) {
                            // Double Click action
                            /*e.consume();
                            Map params = getPreFilledParameters();
                            if (params.containsKey("sourceIndexString")) {
                                String sourceIndexes = (String) params.get("sourceIndexString");
                                if ((sourceIndexes.split(":").length==1)&&(sourceIndexes.split(",").length==1)) {
                                    int idSelected = Integer.valueOf(sourceIndexes);
                                    cmds.run(BdvWindowTranslateOnSource.class, true,
                                            "bdvh", params.get("bdvh"),
                                            "sourceIndex", idSelected);
                                }
                            }*/
                        }
                    }
                });



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

        public void remove(SourceAndConverter sac) {
            if (displayedSource.contains(sac)) {
                // No Need to update
                displayedSource.remove(sac);
                visitAllNodesAndDelete(top, sac);
            }
        }

        public void visitAllNodesAndDelete(TreeNode node, SourceAndConverter sac) {
            if (node.getChildCount() >= 0) {
                for (Enumeration e = node.children(); e.hasMoreElements();) {
                    TreeNode n = (TreeNode) e.nextElement();
                    if (n.isLeaf()) {
                        if (((DefaultMutableTreeNode) n).getUserObject().equals(sac)) {
                            model.removeNodeFromParent(((DefaultMutableTreeNode) n));
                        }
                    } else {
                        visitAllNodesAndDelete(n, sac);
                    }
                }
            }
        }

        public SourceAndConverter[] getSelectedSourceAndConverters() {
            List<SourceAndConverter> sacList = new ArrayList<>();
            for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
                Object userObj = ((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject();
                sacList.add((SourceAndConverter) userObj);
            }
            return sacList.toArray(new SourceAndConverter[sacList.size()]);
        }

        public void addPopupAction(Consumer<SourceAndConverter[]> action, String actionName) {
            // Show
            JMenuItem menuItem = new JMenuItem(actionName);
            menuItem.addActionListener(e -> action.accept(getSelectedSourceAndConverters()));
            popup.add(menuItem);
        }

    }

}
