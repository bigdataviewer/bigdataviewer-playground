package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
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

public class BdvSourceServiceUI {


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

    public BdvSourceServiceUI(BdvSourceAndConverterService bss) {
        this.bss = bss;
        frame = new JFrame("Bdv Sources");
        panel = new JPanel(new BorderLayout());

        // Tree view of Spimdata
        top = new DefaultMutableTreeNode("Sources");
        tree = new JTree(top);

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
                /*if (e.getClickCount()==2 && !e.isConsumed()) {
                    commandService.run(BdvSourcesAdderCommand.class, true,
                            "sacs", getSelectedSourceAndConverters(),
                            "autoContrast", true,
                            "adjustViewOnSource", true
                    );
                }*/
            }
        });

        frame.add(panel);
        frame.pack();
        frame.setVisible( false );
    }

    public void update(SourceAndConverter src) {
        if (displayedSource.contains(src)) {
            // No Need to update
        } else {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
            top.add(node);
            model.reload(top);
            panel.revalidate();
            displayedSource.add(src);
            frame.setVisible( true );
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
        List<SourceAndConverter> sacList = new ArrayList<>();
        for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
            if (((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject() instanceof RenamableSourceAndConverter) {
                Object userObj = ((RenamableSourceAndConverter) ((DefaultMutableTreeNode) tp.getLastPathComponent()).getUserObject()).sac;
                sacList.add((SourceAndConverter) userObj);
            }
        }
        return sacList.toArray(new SourceAndConverter[sacList.size()]);
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

    public void buildTree() {
        //model
    }

    public class SourceFilterNode extends DefaultMutableTreeNode {
        Predicate<SourceAndConverter> filter;
        boolean allowDuplicate;
        public SourceFilterNode(Predicate<SourceAndConverter> filter, boolean allowDuplicate) {
            this.filter = filter;
            this.allowDuplicate = false;
        }
    }
}
