package sc.fiji.bdvpg.scijava.gui.swing;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.viewer.EasySwingDisplayViewer;
import org.scijava.ui.viewer.DisplayViewer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO:
 * - Show info of view setups
 * - Add update button for bdvh linked if necessary
 * - Add edit possibility for view setup
 * - List commands which can be used with AbstractSpimData
 */

@Plugin(type = DisplayViewer.class)
public class SwingAbstractSpimDataViewer extends
        EasySwingDisplayViewer<AbstractSpimData> {

    @Parameter
    CommandService cmds;

    /**
     * SpimData shown by this instance of display
     */
    public AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>> asd;

    /**
     * Current tree representation of viewSetups
     */
    public SpimDataTree sdt;

    /**
     * Class of entities used for sorting the tree
     */
    public List<Class<? extends Entity>> entitiesListUsedForViewSetupSorting = new ArrayList<>();

    /**
     * String representation of entities present in the dataset
     */
    ArrayList<String> presentEntities = new ArrayList<>();

    /**
     * Swing JTree used for displaying SpimDataTree object
     */
    public JTree tree;

    /**
     * List of all entities found in the SpimData
     */
    Map<Class, Set<Entity>> entitiesSortedByClass;

    /**
     * root node of the tree
     */
    DefaultMutableTreeNode top;

    /**
     * GUI Swing elements
     */
    JLabel nameLabel;
    JTextArea textAreaSpimDataOrdering;
    JTextArea textAreaAvailableEntities;
    JTextArea textAreaMessage;
    JTabbedPane tabbedPane;
    JScrollPane treeView;
    JPanel panel;
    DefaultTreeModel model;

    public int offsetBdvSourceIndex = 0;

    public SwingAbstractSpimDataViewer()
    {
        super( AbstractSpimData.class );
    }

    @Override
    protected boolean canView(AbstractSpimData abstractSpimData) {
        return true;
    }

    @Override
    protected void redoLayout() {
    }

    @Override
    protected void setLabel(String s) {
    }

    static List<BasicViewSetup> getViewSetupFilteredByEntity(Entity e, List<BasicViewSetup> lbvs) {
        return      lbvs.stream()
                        .filter(vs -> vs.getAttribute(e.getClass())!=null)
                        .filter(vs ->  vs.getAttribute(e.getClass()).getId()==e.getId())
                        .collect(Collectors.toList());
    }

    public void updateCurrentEntityClasses() {
        presentEntities.clear();
        entitiesSortedByClass.keySet().forEach(
                c -> presentEntities.add(((Class<Entity>)c).getSimpleName().toLowerCase())
        );

        String strEntities =
                "List of entities in the dataset:\n"+
                presentEntities.stream().collect(Collectors.joining(","))+"\n"+
                "Type in the text area below how to want to sort the setups of this dataset"+"\n"+
                "The tree will be updated when you press enter. \n"+
                "Take care to case sensitivity!";
        textAreaAvailableEntities.setText(strEntities);
    }

    public void sortEntitiesByClass() {
        entitiesSortedByClass =
                asd.getSequenceDescription()
                        .getViewSetups()
                        .values().stream()
                        .map(bvs -> bvs.getAttributes().values())
                        .reduce(new ArrayList<>(), (a, b) -> {a.addAll(b); return a;}).stream()
                        .collect(Collectors.groupingBy(e -> e.getClass(),Collectors.toSet()));
    }

    @Override
    protected void redraw() {
        sortEntitiesByClass();
        updateCurrentEntityClasses();
        sdt = new SpimDataTree(entitiesListUsedForViewSetupSorting,null, asd.getSequenceDescription().getViewSetupsOrdered());
        top.removeAllChildren();
        addNodes(top, sdt);
        model.reload(top);
        panel.revalidate();
    }

    public void reOrderTree(String str) {
        String[] entityNames = str.split(",");
        List<Class<? extends Entity>> newEntitiesOrder = new ArrayList<>();
        // Testing names
        for (String classStr:entityNames) {
            Optional<String> cEntity =  presentEntities.stream().filter(e -> e.equals(classStr.toLowerCase().trim())).findFirst();
            if (!cEntity.isPresent()) {
                textAreaMessage.setText("Cannot find entity "+classStr);
                return;
            } else {
                newEntitiesOrder.add(0,
                        entitiesSortedByClass.keySet().stream().filter(
                                e -> e.getSimpleName().toLowerCase().equals(cEntity.get())
                        ).findFirst().get()
                );
            }
        }
        // All names validated
        this.entitiesListUsedForViewSetupSorting = newEntitiesOrder;
        sdt = new SpimDataTree(entitiesListUsedForViewSetupSorting,null, asd.getSequenceDescription().getViewSetupsOrdered());
        top.removeAllChildren();
        addNodes(top, sdt);
        model.reload(top);
        panel.revalidate();
        textAreaMessage.setText("SpimData tree ordering: done.");
    }

    public List<Integer> getSelectedIds() {
        HashSet<Integer> selectedVSIds = new HashSet<>();
        for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
            Object userObj = ((DefaultMutableTreeNode)tp.getLastPathComponent()).getUserObject();
            if (userObj instanceof SpimDataTree) {
                sdt = (SpimDataTree) userObj;
                selectedVSIds.addAll(sdt.allVSFromBranch.stream().map(vs -> vs.getId()).collect(Collectors.toList()));
            } else {
                assert userObj instanceof RenamableViewSetup;
                selectedVSIds.add(((RenamableViewSetup) userObj).vs.getId());
            }
        }
        return selectedVSIds.stream().sorted().map(id -> id + offsetBdvSourceIndex).collect(Collectors.toList());
    }

    public String updateSelectedViewSetupsIds() {

        List<Integer> orderedVSIds = getSelectedIds();

        // Simple List
        //String strIndexes = orderedVSIds.stream().map(id -> Integer.toString(id)).collect(Collectors.joining(","));

        String betterListOfIndexes = "";
        if (orderedVSIds.size()>0) {
            int lastIndex = orderedVSIds.get(0);
            betterListOfIndexes = betterListOfIndexes+lastIndex;
            boolean buildingRange = false;
            for (int i=1;i<orderedVSIds.size();i++) {
                int nextIndex = orderedVSIds.get(i);
                if (nextIndex==lastIndex+1) {
                    if (buildingRange) {
                        lastIndex = nextIndex;
                    } else {
                        betterListOfIndexes = betterListOfIndexes+":";
                        lastIndex = nextIndex;
                        buildingRange = true;
                    }
                } else {
                    if (buildingRange) {
                        betterListOfIndexes = betterListOfIndexes + lastIndex+","+nextIndex;
                        buildingRange=false;
                        lastIndex = nextIndex;
                    } else {
                        betterListOfIndexes = betterListOfIndexes +","+nextIndex;
                        lastIndex = nextIndex;
                    }
                }
            }
            if (buildingRange) {
                betterListOfIndexes = betterListOfIndexes+orderedVSIds.get(orderedVSIds.size()-1);
            }
        }
        textAreaMessage.setText(betterListOfIndexes);
        return betterListOfIndexes;
    }

    /*public Map<String, Object> getPreFilledParameters() {
        Map<String, Object> out = new HashMap<>();
        if (cs.get(asd) != null) {
            List<BdvStackSource<?>> lbss = (List<BdvStackSource<?>>) cs.get(asd);
            if (lbss.size()>0) {
                textAreaMessage.setText("OK.");
                out.put("sourceIndexString", updateSelectedViewSetupsIds());
                out.put("bdvh", lbss.get(0).getBdvHandle());
                return out;
            } else {
                textAreaMessage.setText("No source found.");
                return out;
            }
        } else {
            textAreaMessage.setText("Could not find cached Bdv Window instance, please run Add to Bdv > SpimDataset.");
            return out;
        }
    }*/

    @Override
    protected JPanel createDisplayPanel(AbstractSpimData abstractSpimData) {

        this.asd = abstractSpimData;

        sortEntitiesByClass();

        textAreaMessage = new JTextArea();
        textAreaMessage.setEditable(false);

        // From Inner parts to outer parts
        nameLabel = new JLabel(asd.toString());

        // For ordering SpimData according to entitiesListUsedForViewSetupSorting
        textAreaSpimDataOrdering = new JTextArea();
        textAreaSpimDataOrdering.setEditable(true);
        textAreaSpimDataOrdering.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (e.getKeyCode() == e.VK_ENTER) {
                    reOrderTree(((JTextArea)e.getSource()).getText());
                }
            }
        });

        textAreaAvailableEntities = new JTextArea();
        textAreaAvailableEntities.setEditable(false);

        updateCurrentEntityClasses();

        JPanel tabOrdering = new JPanel();
        tabOrdering.setLayout(new GridLayout(2,1));
        tabOrdering.add(new JScrollPane(textAreaAvailableEntities));
        tabOrdering.add(textAreaSpimDataOrdering);

        // Tree view of Spimdata
        top = new DefaultMutableTreeNode("SpimData");
        tree = new JTree(top);

        // PopupMenu
        //final JPopupMenu popup = (new SwingBdvPopupMenu(cmds, () -> getPreFilledParameters())).getPopup();

        // JTree of SpimData
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (SwingUtilities.isRightMouseButton(e)) {
                   // popup.show(e.getComponent(), e.getX(), e.getY());
                }
                if (e.getClickCount()==2 && !e.isConsumed()) {
                    e.consume();
                    /* Map params = getPreFilledParameters();
                    if (params.containsKey("sourceIndexString")) {
                        String sourceIndexes = (String) params.get("sourceIndexString");
                        if ((sourceIndexes.split(":").length==1)&&(sourceIndexes.split(",").length==1)) {
                            int idSelected = Integer.valueOf(sourceIndexes);
                            cmds.run(BdvWindowTranslateOnSource.class, true,
                                    "bdvh", params.get("bdvh"),
                                    "sourceIndex", idSelected);
                        }
                    } */
                }
            }
        });

        tree.setRootVisible(false);
        tree.addTreeSelectionListener(e -> updateSelectedViewSetupsIds());

        model = (DefaultTreeModel)tree.getModel();
        treeView = new JScrollPane(tree);

        // TabbedPane
        tabbedPane = new JTabbedPane();
        tabbedPane.add("Tree", treeView);
        tabbedPane.add("Tree Ordering", tabOrdering);

        // Global Panel
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        // Top: name
        panel.add(nameLabel, BorderLayout.NORTH);
        // Center: Tabbed Pane
        panel.add(tabbedPane, BorderLayout.CENTER);
        // Bottom: indexes or message
        panel.add(textAreaMessage, BorderLayout.SOUTH);

        //JButton showInBdv = new JButton("<html>S<br>H<br>O<br>W</html>");

        //showInBdv.addActionListener(a -> {
           //  cmds.run(BdvAppendSpimData.class, true, "spimData", abstractSpimData);
        //});

        //panel.add(showInBdv, BorderLayout.WEST);

        //JButton saveButton = new JButton("<html>S<br>A<br>V<br>E</html>");

        //saveButton.addActionListener(a -> {
           // cmds.run(SpimdatasetSave.class, true, "spimData", abstractSpimData);
        //});

        //panel.add(saveButton, BorderLayout.EAST);
        // myTree.expandPath(new TreePath(currentNode.getPath()));

        this.redraw();
        return panel;
    }

    private void addNodes(DefaultMutableTreeNode basenode, SpimDataTree sdt_in ) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(sdt_in);
        sdt_in.leavesOnNode.stream()
                .map(vs -> new DefaultMutableTreeNode(new RenamableViewSetup(vs)))
                .forEach(n -> node.add(n));
        sdt_in.branches.values().forEach( branch -> {
            addNodes(node,branch);
        });
        basenode.add(node);
    }

    public class SpimDataTree {

        Map<Entity, SpimDataTree> branches = new HashMap<>();

        List<BasicViewSetup> allVSFromBranch;

        List<BasicViewSetup> leavesOnNode;

        Entity rootEntity;

        public SpimDataTree(List<Class<? extends Entity>> entitiesToSort, Entity rootEntity, List<BasicViewSetup> viewSetups) {
            this.rootEntity = rootEntity;
            allVSFromBranch = viewSetups;
            if (entitiesToSort.size()>0) {
                List<Class<? extends Entity>> entitiesToSortCopy = new ArrayList<>();

                entitiesToSort.forEach(cl -> entitiesToSortCopy.add(cl));

                Class<? extends Entity> entityClass = entitiesToSortCopy.get(entitiesToSort.size()-1);
                boolean stillContainsClass = true;
                while ((stillContainsClass)&&(!entitiesSortedByClass.containsKey(entityClass))) {
                    entitiesToSortCopy.remove(entitiesToSort.size()-1);
                    stillContainsClass = entitiesToSortCopy.size()>0;
                    if (stillContainsClass) {
                        entityClass = entitiesToSortCopy.get(entitiesToSort.size()-1);
                    }
                }
                if (stillContainsClass) {
                    entitiesToSortCopy.remove(entityClass);
                    entitiesSortedByClass.get(entityClass).forEach(
                            e -> {
                                List<BasicViewSetup> lvs = getViewSetupFilteredByEntity(e,allVSFromBranch);
                                if (lvs.size()>0)
                                branches.put(e,new SpimDataTree(entitiesToSortCopy,e,lvs));
                            }
                    );
                    final Class<? extends Entity> c = entityClass;
                    leavesOnNode = allVSFromBranch
                            .stream()
                            .filter(vs -> vs.getAttribute(c)==null)
                            .collect(Collectors.toList());
                } else {
                    leavesOnNode = allVSFromBranch;
                }
            } else {
                leavesOnNode = allVSFromBranch;
            }
        }

        public String toString() {
            if (rootEntity!=null) {
                return rootEntity.getClass().getSimpleName().toLowerCase() + ":" + rootEntity.getId()+" ("+allVSFromBranch.size()+")";
            } else {
                return "SpimData";
            }
        }
    }

    public class RenamableViewSetup {
        public BasicViewSetup vs;

        public RenamableViewSetup(BasicViewSetup vs) {
            this.vs = vs;
        }

        public String toString() {
            return vs.getName()+":"+vs.getId();
        }
    }

}
