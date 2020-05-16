package sc.fiji.bdvpg.scijava.services.ui.swingdnd;

import bdv.ui.SourcesTransferable;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.RenamableSourceAndConverter;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

/**
 * Goal : allows to drop drop SourceAndConverter from the SourceAndConverterServiceUI into the bdv windows
 *
 * Allows importing nodes from the SourceAndConverterServiceUI JTree
 */

public class BdvTransferHandler extends TransferHandler {
    DataFlavor nodesFlavor;
    DataFlavor[] flavors = new DataFlavor[2];

    public BdvTransferHandler() {
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

    //TransferHandler
    /*@Override public boolean canImport(JComponent comp, DataFlavor flavor[]) {
        for (int i = 0, n = flavor.length; i < n; i++) {
            for (int j = 0, m = flavors.length; j < m; j++) {
                if (flavor[i].equals(flavors[j])) {

                    return true;
                }
            }
        }
        return false;
    }*/


    public boolean canImport(TransferSupport support) {

        if (support.getComponent() instanceof JComponent) {
            //return canImport((JComponent)support.getComponent(), support.getDataFlavors());

            for (int i = 0, n = support.getDataFlavors().length; i < n; i++) {
                for (int j = 0, m = flavors.length; j < m; j++) {
                    if (support.getDataFlavors()[i].equals(flavors[j])) {

                        return true;
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    @Override public boolean importData(TransferHandler.TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }

        // See if we can get the SourcesTransferable flavor
        if (support.getTransferable().isDataFlavorSupported(SourcesTransferable.flavor)) {
            try {
                System.out.println("SourcesTransferable flavor");
                final List<SourceAndConverter<?>> sacs =
                        ((SourcesTransferable.SourceList) support.getTransferable().getTransferData(SourcesTransferable.flavor))
                                .getSources();
                ((bdv.viewer.ViewerPanel)support.getComponent()).state().addSources(sacs);
                ((bdv.viewer.ViewerPanel)support.getComponent()).state().setSourcesActive(sacs, true);
            } catch (Exception e) {

            }
        }

        // Extract transfer data.
        DefaultMutableTreeNode[] nodes = null;
        try {
            Transferable t = support.getTransferable();
            nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
        } catch (UnsupportedFlavorException ufe) {
            System.err.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch (java.io.IOException ioe) {
            System.err.println("I/O error: " + ioe.getMessage());
        }
        // Get drop location info.
        /*
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
        */
        // Add data to model.

        if (SourceAndConverterServices.getSourceAndConverterService() instanceof SourceAndConverterService) {
            List<SourceAndConverter<?>> sacs = new ArrayList<>();
            SourceAndConverterServiceUI ui =
                    ((SourceAndConverterService)SourceAndConverterServices.getSourceAndConverterService()).getUI();

            for (int i = 0; i < nodes.length; i++) {
                DefaultMutableTreeNode unwraped = (DefaultMutableTreeNode) (nodes[i].getUserObject());
                if (unwraped.getUserObject() instanceof RenamableSourceAndConverter) {
                    sacs.add(((RenamableSourceAndConverter)unwraped.getUserObject()).sac);
                } else {
                    for (SourceAndConverter sac : ui.getSourceAndConvertersFromChildrenOf(unwraped)) {
                        sacs.add(sac);
                    }
                }
            }

            ((bdv.viewer.ViewerPanel)support.getComponent()).state().addSources(sacs);
            ((bdv.viewer.ViewerPanel)support.getComponent()).state().setSourcesActive(sacs, true);

            return true;
        } else {
            // Unsupported drop
            return false;
        }

    }


}
