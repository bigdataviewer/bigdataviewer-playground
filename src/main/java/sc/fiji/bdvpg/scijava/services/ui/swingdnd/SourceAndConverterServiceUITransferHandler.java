package sc.fiji.bdvpg.scijava.services.ui.swingdnd;

import bdv.ui.SourcesTransferable;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
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

    protected Transferable createTransferable(JComponent c) {
        Transferable t = super.createTransferable(c);
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
        return supp.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport supp) {
        if (!canImport(supp)) {
            return false;
        }

        // Fetch the Transferable and its data
        Transferable t = supp.getTransferable();
        try {
            List<File> files = (List)t.getTransferData(DataFlavor.javaFileListFlavor);
            for (File f : files) {
                if (f.getAbsolutePath().endsWith(".xml")) {
                    new SpimDataFromXmlImporter(f).run();
                } else {
                    System.out.println("Unsupported drop operation with file "+f.getAbsolutePath());
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
