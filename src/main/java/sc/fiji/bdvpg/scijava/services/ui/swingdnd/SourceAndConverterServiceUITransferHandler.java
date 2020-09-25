package sc.fiji.bdvpg.scijava.services.ui.swingdnd;

import bdv.ui.SourcesTransferable;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;
import sc.fiji.bdvpg.scijava.services.ui.SourceFilterNode;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SourceAndConverterServiceUITransferHandler extends TreeTransferHandler {

    static DataFlavor nodesFlavor;
    static DataFlavor[] flavors = new DataFlavor[2];

    static {
        try {
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" +
                    DefaultMutableTreeNode[].class.getName() + "\"";
            nodesFlavor = new DataFlavor(mimeType);
            flavors[0] = nodesFlavor;
            flavors[1] = SourcesTransferable.flavor;
        } catch(ClassNotFoundException e) {
            System.out.println("ClassNotFound: " + e.getMessage());
        }
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
        // Nothing to do
    }

    //@Override
    /** Defensive copy used in createTransferable. */
    private DefaultMutableTreeNode copy(TreeNode node) {
        if (node instanceof SourceFilterNode) {
            return (SourceFilterNode)((SourceFilterNode)node).clone();
        } else {
            return new DefaultMutableTreeNode(node);
        }
    }

    //TransferHandler
    protected Transferable createTransferableNodes(JComponent c) {
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
            DefaultMutableTreeNode[] nodesToRemove =
                    toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
            return new NodesTransferable(nodes);
        }
        return null;
    }
    
    protected Transferable createTransferable(JComponent c) {
        Transferable t = createTransferableNodes(c);
        ExtTransferable extT = new ExtTransferable();

        try {
            extT.setNodesData(t.getTransferData(nodesFlavor));
            if (SourceAndConverterServices.getSourceAndConverterService() instanceof SourceAndConverterService) {
                SourceAndConverterServiceUI ui =
                        ((SourceAndConverterService) SourceAndConverterServices.getSourceAndConverterService()).getUI();
                List<SourceAndConverter<?>> sacs = new ArrayList<>();
                for (SourceAndConverter sac : ui.getSelectedSourceAndConverters()) {
                    sacs.add(sac);
                }
                extT.setSourcesList(sacs);
            }
            return extT;
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

        //return new StringSelection(c.getSelection());
    }

    @Override
    public boolean canImport(TransferSupport supp) {
        if (!supp.isDrop()) {
            return false;
        }
        supp.setShowDropLocation(true);
        return (( supp.isDataFlavorSupported(DataFlavor.javaFileListFlavor ))||(supp.isDataFlavorSupported(nodesFlavor)));
    }

    @Override
    public boolean importData(TransferSupport supp) {
        if (!canImport(supp)) {
            return false;
        }

        // Fetch the Transferable and its data
        Transferable t = supp.getTransferable();
        try {
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> files = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
                for (File f : files) {
                    if (f.getAbsolutePath().endsWith(".xml")) {
                        new SpimDataFromXmlImporter(f).run();
                    } else {
                        System.out.println("Unsupported drop operation with file " + f.getAbsolutePath());
                    }
                }
            } else if (t.isDataFlavorSupported(nodesFlavor)) {
                System.out.println("NODES!");
                DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
                if (nodes.length!=1) {
                    System.err.println("Only one node should be dragged");
                    return false;
                }
                if ((nodes[0]) instanceof SourceFilterNode) {

                    SourceFilterNode sfn = (SourceFilterNode) (nodes[0]);

                    //sfn.add(new SourceFilterNode("All sources", (sac) -> true, true));
                    //topNodeStructureChanged = true;
                    System.out.println("In theory DnD OK");

                    JTree.DropLocation dl = (JTree.DropLocation) supp.getDropLocation();
                    int childIndex = dl.getChildIndex();
                    TreePath dest = dl.getPath();

                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();

                    JTree tree = (JTree) supp.getComponent();

                    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();

                    int index = childIndex;

                    if (childIndex == -1) {
                        index = parent.getChildCount();
                    }

                    final int indexFinal = index;

                    SwingUtilities.invokeLater(() -> {
                        model.insertNodeInto(nodes[0], parent, indexFinal);
                        model.nodeStructureChanged(parent);
                        //model.reload();
                    });

                    return true;

                } else {
                    System.err.println("A source filter node should be selected");
                    System.out.println(nodes[0].getClass().getName());
                    return false;
                }

            }
        } catch (UnsupportedFlavorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public class ExtTransferable implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavors[0].equals(flavor)||flavors[1].equals(flavor);
        }

        Object nodes;
        public void setNodesData(Object nodes) {
            this.nodes = nodes;
        }

        SourcesTransferable sourcesTransferable;
        public void setSourcesList(Collection<SourceAndConverter<?>> sources) {
            this.sourcesTransferable = new SourcesTransferable(sources);
        }


        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (flavor.equals(nodesFlavor)) {
                return nodes;
            }
            if (flavor.equals(SourcesTransferable.flavor)) {
                return sourcesTransferable.getTransferData(SourcesTransferable.flavor);
            }
            return null;
        }
    }
}
