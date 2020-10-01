package sc.fiji.bdvpg.scijava.services.ui.swingdnd;

import bdv.ui.SourcesTransferable;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
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
import java.util.Optional;
import java.util.function.Function;

/**
 * Allows to drop drop SourceAndConverter from the SourceAndConverterServiceUI into the bdv windows
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

    public void updateDropLocation(TransferSupport support, DropLocation dl) {
        // Do nothing : can be extended for custom behaviour
    }

    public void importSourcesAndConverters(TransferSupport support, List<SourceAndConverter<?>> sacs) {
        // Can be extended for custom action on sources import
        Optional<BdvHandle> bdvh = getBdvHandleFromViewerPanel(((bdv.viewer.ViewerPanel)support.getComponent()));
        if (bdvh.isPresent()) {
            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .show(bdvh.get(), sacs.toArray(new SourceAndConverter[sacs.size()]));
        }
    }

    public Optional<BdvHandle> getBdvHandleFromViewerPanel(ViewerPanel viewerPanel) {
        return SourceAndConverterServices.
                getSourceAndConverterDisplayService()
                .getDisplays().stream().filter(bdvh -> bdvh.getViewerPanel().equals(viewerPanel)).findFirst();
    }

    public boolean canImport(TransferSupport support) {

        if (support.getComponent() instanceof JComponent) {
            for (int i = 0, n = support.getDataFlavors().length; i < n; i++) {
                for (int j = 0, m = flavors.length; j < m; j++) {
                    if (support.getDataFlavors()[i].equals(flavors[j])) {
                        DropLocation dl = support.getDropLocation();
                        updateDropLocation(support, dl);
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
                final List<SourceAndConverter<?>> sacs =
                        ((SourcesTransferable.SourceList) support.getTransferable().getTransferData(SourcesTransferable.flavor))
                                .getSources();
                importSourcesAndConverters(support, sacs);
                return true;
            } catch (Exception e) {

            }
        } else {

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

            if (SourceAndConverterServices.getSourceAndConverterService() instanceof SourceAndConverterService) {
                List<SourceAndConverter<?>> sacs = new ArrayList<>();
                SourceAndConverterServiceUI ui =
                        ((SourceAndConverterService) SourceAndConverterServices.getSourceAndConverterService()).getUI();

                for (int i = 0; i < nodes.length; i++) {
                    DefaultMutableTreeNode unwraped = (DefaultMutableTreeNode) (nodes[i].getUserObject());
                    if (unwraped.getUserObject() instanceof RenamableSourceAndConverter) {
                        sacs.add(((RenamableSourceAndConverter) unwraped.getUserObject()).sac);
                    } else {
                        for (SourceAndConverter sac : ui.getSourceAndConvertersFromChildrenOf(unwraped)) {
                            sacs.add(sac);
                        }
                    }
                }

                importSourcesAndConverters(support, sacs);

                return true;
            } else {
                // Unsupported drop
                return false;
            }
        }
        return false;

    }

    Function<JComponent, Transferable> transferableSupplier = null;

    public void setTransferableFunction(Function<JComponent, Transferable> transferableSupplier) {
        this.transferableSupplier = transferableSupplier;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        System.out.println("Create BDV Transferable");
        if (transferableSupplier!=null) {
            return transferableSupplier.apply(c);
        } else {
            return super.createTransferable(c);
        }
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }
}
