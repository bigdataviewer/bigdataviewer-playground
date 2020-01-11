package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService;

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
import java.util.function.Consumer;
import java.util.function.Predicate;

import static sc.fiji.bdvpg.scijava.services.BdvSourceAndConverterService.SETSPIMDATA;

public class BdvSourceServiceUI {

    BdvSourceAndConverterService bss;
    JFrame frame;
    JPanel panel;
    /**
     * Swing JTree used for displaying Sources object
     */
    JTree tree;
    SourceFilterNode top;
    JScrollPane treeView;
    DefaultTreeModel model;
    Set<SourceAndConverter> displayedSource = new HashSet<>();

    JPopupMenu popup = new JPopupMenu();
    SourceFilterNode allSourcesNode;

    public BdvSourceServiceUI(BdvSourceAndConverterService bss) {
        this.bss = bss;

        frame = new JFrame("Bdv Sources");
        panel = new JPanel(new BorderLayout());

        // Tree view of Spimdata
        top = new SourceFilterNode("Sources", (sac) -> true, true);
        tree = new JTree(top);
        tree.setRootVisible(false);
        allSourcesNode = new SourceFilterNode("All Sources", (sac) -> true, false);
        top.add(allSourcesNode);

        SourceFilterNode spimDataSources = new SourceFilterNode("In SpimData", (sac) -> bss.getAttachedSourceAndConverterData().get(sac).containsKey(SETSPIMDATA), false);
        allSourcesNode.add(spimDataSources);

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
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
                // Double Click : display source
                if (e.getClickCount()==2 && !e.isConsumed()) {
                    /*commandService.run(BdvSourcesAdderCommand.class, true,
                            "sacs", getSelectedSourceAndConverters(),
                            "autoContrast", true,
                            "adjustViewOnSource", true
                    );*/
                }
            }
        });

        frame.add(panel);
        frame.pack();
        frame.setVisible( false );
    }

    public void update(SourceAndConverter sac) {
        if (displayedSource.contains(sac)) {
            // No Need to update
            visitAllNodesAndDelete(top, sac);
            updateSpimDataFilterNodes();
            insertIntoTree(sac);
            //model.reload();
        } else {
            System.out.println("Adding "+sac.getSpimSource().getName());
            displayedSource.add(sac);
            updateSpimDataFilterNodes();
            //model.reload();
            insertIntoTree(sac);
            //model.reload();
            panel.revalidate();
            frame.setVisible( true );
        }
    }

    List<SpimDataFilterNode> spimdataFilterNodes = new ArrayList<>();

    private void updateSpimDataFilterNodes() {
        // Fetch All Spimdatas from all Sources
        Set<AbstractSpimData> currentSpimdatas = new HashSet<>();
        displayedSource.forEach(sac -> {
            if (bss.getAttachedSourceAndConverterData().get(sac).containsKey(SETSPIMDATA)) {
                Set<AbstractSpimData> set = (Set<AbstractSpimData>)(bss.getAttachedSourceAndConverterData().get(sac).get(SETSPIMDATA));
                currentSpimdatas.addAll(set);

            }
        });

        // Check for obsolete spimdatafilternodes
        spimdataFilterNodes.forEach(fnode -> {
            if (!currentSpimdatas.contains(fnode.asd)) {
                 model.removeNodeFromParent(fnode);
            }
        });

        // Check for new spimdata
        currentSpimdatas.forEach(asdtest -> {
                System.out.println("Test "+asdtest.toString());
                if ((spimdataFilterNodes.size()==0)||(spimdataFilterNodes.stream().noneMatch(fnode -> fnode.asd.equals(asdtest)))) {
                    SpimDataFilterNode newNode = new SpimDataFilterNode("SpimData "+spimdataFilterNodes.size(), asdtest);
                    spimdataFilterNodes.add(newNode);
                    top.insert(newNode, 0);
                    model.reload(top);
                    System.out.println("Adding");
                }
            }
        );

    }

    void insertIntoTree(SourceAndConverter sac) {
        RenamableSourceAndConverter rsac = new RenamableSourceAndConverter(sac);
        insertIntoTree(top, rsac);
    }

    void insertIntoTree(SourceFilterNode parent, RenamableSourceAndConverter rsac) {
        boolean consumed = false;
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) parent.getChildAt(i);
            if (n instanceof SourceFilterNode) {
                SourceFilterNode f = (SourceFilterNode) n;
                if (f.filter.test(rsac.sac)) {
                    insertIntoTree(f, rsac);
                    if (!f.allowDuplicate) {
                        consumed = true;
                    }
                }
            }
        }
        if (!consumed) {
            //System.out.println("Adding "+rsac.sac.getSpimSource().getName());
            parent.add(new DefaultMutableTreeNode(rsac));
            model.reload(parent);
        }
    }

    public void remove(SourceAndConverter sac) {
        if (displayedSource.contains(sac)) {
            // No Need to update
            displayedSource.remove(sac);
            visitAllNodesAndDelete(top, sac);
            updateSpimDataFilterNodes();
        }
    }

    void visitAllNodesAndDelete(TreeNode node, SourceAndConverter sac) {
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                if (n.isLeaf() && ((DefaultMutableTreeNode) n).getUserObject() instanceof RenamableSourceAndConverter) {
                    if (((RenamableSourceAndConverter)((DefaultMutableTreeNode) n).getUserObject()).sac.equals(sac)) {
                        model.removeNodeFromParent(((DefaultMutableTreeNode) n));
                    }
                } else {
                    visitAllNodesAndDelete(n, sac);
                }
            }
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

    public void addPopupAction(Consumer<SourceAndConverter[]> action, String actionName) {
        // Show
        JMenuItem menuItem = new JMenuItem(actionName);
        menuItem.addActionListener(e -> action.accept(getSelectedSourceAndConverters()));
        popup.add(menuItem);
    }

    public void addPopupLine() {
        popup.addSeparator();
    }

    public class RenamableSourceAndConverter {
        public SourceAndConverter sac;
        public RenamableSourceAndConverter(SourceAndConverter sac) {
            this.sac = sac;
        }
        public String toString() {
            return sac.getSpimSource().getName();
        }
    }

    public class SourceFilterNode extends DefaultMutableTreeNode {
        Predicate<SourceAndConverter> filter;
        boolean allowDuplicate;
        String name;

        public SourceFilterNode(String name, Predicate<SourceAndConverter> filter, boolean allowDuplicate) {
            super(name);
            this.name = name;
            this.filter = filter;
            this.allowDuplicate = allowDuplicate;
        }

        public String toString() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public class SpimDataFilterNode extends SourceFilterNode {

        public AbstractSpimData asd;

        public boolean filter(SourceAndConverter sac) {
            Map<String, Object> props = bss.getAttachedSourceAndConverterData().get(sac);
            assert props!=null;
            //System.out.println("Testing "+sac.getSpimSource().getName()+" vs "+asd.toString());
            return (props.containsKey(SETSPIMDATA))&&((Set<AbstractSpimData>)props.get(SETSPIMDATA)).contains(asd);
        }

        public SpimDataFilterNode(String name, AbstractSpimData spimdata) {
            super(name,null, true);
            this.filter = this::filter;
            asd = spimdata;
        }

        public String toString() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}
