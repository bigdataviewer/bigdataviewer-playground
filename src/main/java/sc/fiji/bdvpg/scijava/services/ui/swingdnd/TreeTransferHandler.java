package sc.fiji.bdvpg.scijava.services.ui.swingdnd;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

public class TreeTransferHandler extends TransferHandler {
    DataFlavor nodesFlavor;
    DataFlavor[] flavors = new DataFlavor[1];
    DefaultMutableTreeNode[] nodesToRemove;

    public TreeTransferHandler() {
        try {
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" +
                    DefaultMutableTreeNode[].class.getName() + "\"";
            nodesFlavor = new DataFlavor(mimeType);
            flavors[0] = nodesFlavor;
        } catch(ClassNotFoundException e) {
            System.out.println("ClassNotFound: " + e.getMessage());
        }
    }

    //TransferHandler
    @Override public int getSourceActions(JComponent c) {
        return TransferHandler.COPY;
    }

    //TransferHandler
    @Override public boolean canImport(JComponent comp, DataFlavor flavor[]) {
        for (int i = 0, n = flavor.length; i < n; i++) {
            for (int j = 0, m = flavors.length; j < m; j++) {
                if (flavor[i].equals(flavors[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    //TransferHandler
    @Override protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null) {
            List<DefaultMutableTreeNode> copies = new ArrayList<>();
            List<DefaultMutableTreeNode> toRemove = new ArrayList<>();
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) paths[0].getLastPathComponent();
            DefaultMutableTreeNode copy = copy(node);
            copies.add(copy);
            toRemove.add(node);
            for (int i = 1; i < paths.length; i++) {
                DefaultMutableTreeNode next =
                        (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                // Do not allow higher level nodes to be added to list.
                if (next.getLevel() < node.getLevel()) {
                    break;
                } else if (next.getLevel() > node.getLevel()) {  // child node
                    copy.add(copy(next));
                    // node already contains child
                } else {                                        // sibling
                    copies.add(copy(next));
                    toRemove.add(next);
                }
            }
            DefaultMutableTreeNode[] nodes =
                    copies.toArray(new DefaultMutableTreeNode[copies.size()]);
            nodesToRemove =
                    toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
            return new NodesTransferable(nodes);
        }
        return null;
    }
    /** Defensive copy used in createTransferable. */
    private DefaultMutableTreeNode copy(TreeNode node) {
        return new DefaultMutableTreeNode(node);
    }

    //TransferHandler
    @Override public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        // Extract transfer data.
        DefaultMutableTreeNode[] nodes = null;
        try {
            Transferable t = support.getTransferable();
            nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
        } catch (UnsupportedFlavorException ufe) {
            System.out.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch (java.io.IOException ioe) {
            System.out.println("I/O error: " + ioe.getMessage());
        }
        // Get drop location info.
        int childIndex;
        TreePath dest;
        if (support.isDrop()) {
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            childIndex = dl.getChildIndex();
            dest = dl.getPath();
        } else {
            childIndex = -1;
            JTree tree = (JTree) support.getComponent();
            dest = tree.getSelectionPath();
        }
        DefaultMutableTreeNode parent
                = (DefaultMutableTreeNode) dest.getLastPathComponent();
        JTree tree = (JTree) support.getComponent();
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        // Configure for drop mode.
        int index = childIndex;    // DropMode.INSERT
        if (childIndex == -1) {     // DropMode.ON
            index = parent.getChildCount();
        }
        // Add data to model.
        for (int i = 0; i < nodes.length; i++) {
            // ArrayIndexOutOfBoundsException
            model.insertNodeInto(nodes[i], parent, index++);
        }
        return true;
    }

    //TransferHandler
    @Override public boolean importData(JComponent comp, Transferable t) {
        return importData(new TransferHandler.TransferSupport(comp, t));
    }

    public class NodesTransferable implements Transferable {
        DefaultMutableTreeNode[] nodes;

        public NodesTransferable(DefaultMutableTreeNode[] nodes) {
            this.nodes = nodes;
        }

        //Transferable
        @Override public Object getTransferData(DataFlavor flavor)  {
            if(!isDataFlavorSupported(flavor)) {
                return false;
            }
            return nodes;
        }

        //Transferable
        @Override public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        //Transferable
        @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(nodesFlavor);
        }
    }
}