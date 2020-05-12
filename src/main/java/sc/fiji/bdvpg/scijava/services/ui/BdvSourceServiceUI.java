package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * Swing UI for Scijava {@link SourceAndConverterService}
 *
 * All the {@link SourceAndConverter} are inserted into the tree potentially multiple times,
 * in order to allow for multiple useful hierarchy to appear. The most basic example is a
 * SourceAndConverter which is part of a SpimData object. This source will be caught by
 * the appropriate {@link SpimDataFilterNode} in which it will appear again multiple times, sorted
 * by {@link Entity} class found in the spimdata object (Illumination, Angle, Channel ...)
 *
 *
 * Nodes are {@link DefaultMutableTreeNode} containing potentially:
 * - {@link RenamableSourceAndConverter} (SourceAndConverter with an overriden toString method)
 * - Filtering nodes : {@link SourceFilterNode} nodes that can filter SourceAndConverters,
 * they contain a {@link Predicate<SourceAndConverter>} that decides whether a SourceAndConverter
 * object should be included in the node; they also contain a boolean flag which sets whether the
 * SourceAndConverter should be passed to the current remaining branch in the tree. In short the sourceandconverter
 * is either captured, or captured and duplicated
 * - Getter Node for Source properties. For instance in the inspect method, a Transformed Source will
 * create a Node for the wrapped source and another node which holds a getter for the fixedAffineTransform
 *
 * For Spimdata synchronization : TODO
 *
 * Addition TODO : a BdvHandle filtering node (which is a filtering node), allows to sort sourceandconverters
 * based on whether they are displayed or not within a BdvHandle
 *
 * For BdvHandle synchronization :
 * TODO
 *
 */
public class BdvSourceServiceUI {

    /**
     * Linked {@link SourceAndConverterService}
     */
    SourceAndConverterService sourceAndConverterService;

    /**
     * JFrame container
     */
    JFrame frame;

    /**
     * JPanel container
     */
    JPanel panel;

    /**
     * Swing JTree used for displaying Sources object
     */
    JTree tree;

    /**
     * Tree root note
     */
    SourceFilterNode top;

    /**
     * Scrollpane to display the JTree, if too big
     */
    JScrollPane treeView;

    /**
     * Tree model
     */
    DefaultTreeModel model;

    /**
     * SourceAndConverter currently displayed in the JTree
     */
    Set<SourceAndConverter> displayedSource = ConcurrentHashMap.newKeySet();

    /**
     * Spimdata Filter nodes currently present in the tree
     */
    List<SpimDataFilterNode> spimdataFilterNodes = new ArrayList<>();

    public BdvSourceServiceUI( SourceAndConverterService sourceAndConverterService ) {
        this.sourceAndConverterService = sourceAndConverterService;

        frame = new JFrame("Bdv Sources");
        panel = new JPanel(new BorderLayout());

        // Tree view of Spimdata
        top = new SourceFilterNode("Sources", (sac) -> true, false);
        tree = new JTree(top);

        SourceFilterNode outsideSpimDataSources = new SourceFilterNode("Other Sources", (sac) -> !sourceAndConverterService.getSacToMetadata().get(sac).containsKey( SPIM_DATA_INFO ), true);
        top.add(outsideSpimDataSources);

        model = (DefaultTreeModel)tree.getModel();
        treeView = new JScrollPane(tree);

        panel.add(treeView, BorderLayout.CENTER);

        // JTree of SpimData
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                // Right Click -> popup
                if (SwingUtilities.isRightMouseButton(e)) {
                    new SourceAndConverterPopupMenu(getSelectedSourceAndConverters())
                            .getPopup()
                            .show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        this.sourceAndConverterService.registerAction("Inspect Sources", this::inspectSources);

        // Delete node for inspection result only
        JMenuItem menuItem = new JMenuItem("Delete Inspect Node");
        menuItem.addActionListener(e -> {
                    for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
                        if ((tp.getLastPathComponent()).toString().startsWith("Inspect Results [")) {
                            model.removeNodeFromParent((DefaultMutableTreeNode)tp.getLastPathComponent());
                        }
                    }
                }
        );
        frame.add(panel);
        frame.pack();
        frame.setVisible(false);
    }

    public void inspectSources(SourceAndConverter[] sacs) {
        for (SourceAndConverter sac:sacs) {
            inspectSource(sac);
        }
    }

    public void inspectSource(SourceAndConverter sac) {
        DefaultMutableTreeNode parentNodeInspect = new DefaultMutableTreeNode("Inspect Results ["+sac.getSpimSource().getName()+"]");
        SourceAndConverterInspector.appendInspectorResult(parentNodeInspect, sac, sourceAndConverterService);
        top.add(parentNodeInspect);
        model.reload(top);
    }

    public void update(SourceAndConverter sac) {
        displayedSource.add(sac);
        updateSpimDataFilterNodes();
        //model.reload();
        if (top.hasConsumed(sac)) {
            top.update(new SourceFilterNode.SourceUpdateEvent(sac));
        } else {
            top.add(new DefaultMutableTreeNode(new RenamableSourceAndConverter(sac)));
        }
        model.reload();
        //panel.revalidate();
        if (!frame.isVisible()) {
            frame.setVisible( true );
        }
    }

    private void updateSpimDataFilterNodes() {
        // Fetch All Spimdatas from all Sources
        Set<AbstractSpimData> currentSpimdatas = new HashSet<>();

        displayedSource.forEach(sac -> {
            if (sourceAndConverterService.getSacToMetadata().get(sac)!=null) {
                if (sourceAndConverterService.getSacToMetadata().get(sac).containsKey(SPIM_DATA_INFO)) {
                    currentSpimdatas.add(((SourceAndConverterService.SpimDataInfo) sourceAndConverterService.getSacToMetadata().get(sac).get(SPIM_DATA_INFO)).asd);
                }
            };
        });

        // Check for obsolete spimdatafilternodes
        spimdataFilterNodes.forEach(fnode -> {
            if (!currentSpimdatas.contains(fnode.asd)) {
                if (fnode.getParent()!=null) {
                    model.removeNodeFromParent(fnode);
                }
            }
        });

        // Check for new spimdata
        currentSpimdatas.forEach(asd -> {
                    //System.out.println("Test "+sdi.toString());
                    if ((spimdataFilterNodes.size()==0)||(spimdataFilterNodes.stream().noneMatch(fnode -> fnode.asd.equals(asd)))) {
                        SpimDataFilterNode newNode = new SpimDataFilterNode("SpimData "+spimdataFilterNodes.size(), asd, sourceAndConverterService);
                        spimdataFilterNodes.add(newNode);
                        addEntityFilterNodes(newNode, asd);
                        top.insert(newNode, 0);
                        //model.reload(top);
                        //System.out.println("Adding");
                    }
                }
        );
    }

    private void addEntityFilterNodes(SpimDataFilterNode nodeSpimData, AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>> asd) {
        // Gets all entities by class


        Map<Class, List<Entity>> entitiesByClass = asd.getSequenceDescription()
                .getViewDescriptions()
                // Streams viewSetups
                .values().stream()
                // Filters if view is present
                .filter(v -> v.isPresent())
                // Gets Entities associated to ViewSetup
                .map(v -> v.getViewSetup().getAttributes().values())
                // Reduce into a single list and stream
                .reduce(new ArrayList<>(), (a, b) -> {
                    a.addAll(b);
                    return a;
                }).stream()
                // removes null entities
                .filter(e -> e!=null)
                // Collected and sorted by class
                .collect(Collectors.groupingBy(e -> e.getClass(), Collectors.toList()));

        Map<Class, SourceFilterNode> classNodes = new HashMap<>();
        entitiesByClass.keySet().forEach((c)-> {
            classNodes.put(c, new SourceFilterNode(c.getSimpleName(),(sac)-> true, false));
        });

        classNodes.values().forEach((f) -> nodeSpimData.add(f));

        Set<Entity> entitiesAlreadyRegistered = new HashSet<>();
        entitiesByClass.forEach((c,el) -> {
            el.forEach(entity -> {
                if (!entitiesAlreadyRegistered.contains(entity)) {
                    classNodes.get(c).add(new SpimDataElementFilter(c.getSimpleName()+" "+entity.getId(),entity, sourceAndConverterService));
                    entitiesAlreadyRegistered.add(entity);
                }
            });
        });

    }

    public void remove(SourceAndConverter sac) {
        if (displayedSource.contains(sac)) {
            // No Need to update
            displayedSource.remove(sac);
            top.remove(sac);
            //visitAllNodesAndDelete(top, sac);
            // updateSpimDataFilterNodes();
        }
    }

    public SourceAndConverter[] getSelectedSourceAndConverters() {
        Set<SourceAndConverter> sacList = new HashSet<>(); // A set avoids duplicate SourceAndConverter
        for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
            if (((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject() instanceof RenamableSourceAndConverter) {
                Object userObj = ((RenamableSourceAndConverter) ((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject()).sac;
                sacList.add((SourceAndConverter) userObj);
            } else {
                sacList.addAll(getSourceAndConvertersFromChildrenOf((DefaultMutableTreeNode) tp.getLastPathComponent()));
            }
        }
        return sacList.toArray(new SourceAndConverter[sacList.size()]);
    }

    private Set<SourceAndConverter> getSourceAndConvertersFromChildrenOf(DefaultMutableTreeNode node) {
        Set<SourceAndConverter> sacs = new HashSet<>();
        for (int i=0;i<node.getChildCount();i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            if (child.getUserObject() instanceof RenamableSourceAndConverter) {
                Object userObj = ((RenamableSourceAndConverter) (child.getUserObject())).sac;
                sacs.add((SourceAndConverter) userObj);
            } else {
                sacs.addAll(getSourceAndConvertersFromChildrenOf(child));
            }
        }
        return sacs;
    }

    public DefaultTreeModel getTreeModel() {
        return model;
    }

    public TreePath getTreePathFromString(String path) {

        String[] stringPath = path.split(">");

        Object[] nodes = new Object[stringPath.length];

        TreeNode current = top;
        int currentDepth = 0;

        while (currentDepth<stringPath.length) {
            final Enumeration children = current.children();
            System.out.println("Searching "+stringPath[currentDepth].trim());
            boolean found = false;
            while (children.hasMoreElements()) {
                TreeNode testNode = (TreeNode)children.nextElement();
                if (testNode.toString().trim().equals(stringPath[currentDepth].trim())) {
                    System.out.println("Found "+testNode.toString().trim());
                    nodes[currentDepth] = testNode;
                    currentDepth++;
                    current = testNode;
                    found = true;
                    break;
                } else {
                    System.out.println("Not Matched "+testNode.toString().trim());
                }
            }
            if (found==false) break;
        }

        if (currentDepth==stringPath.length) {
            return new TreePath(nodes);
        } else {
            System.err.println("TreePath "+path+" not found.");
            return null;
        }
    }

    public Set<SourceAndConverter> getSourceAndConvertersFromTreePath(TreePath path) {
        return getSourceAndConvertersFromChildrenOf((DefaultMutableTreeNode) path.getLastPathComponent());
    }
}
