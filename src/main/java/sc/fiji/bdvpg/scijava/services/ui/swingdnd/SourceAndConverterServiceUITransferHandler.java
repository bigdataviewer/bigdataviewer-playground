package sc.fiji.bdvpg.scijava.services.ui.swingdnd;

import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.tree.*;

public class SourceAndConverterServiceUITransferHandler extends TreeTransferHandler {

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    @Override
    protected void exportDone(JComponent c, Transferable t, int action) {
        // Nothing to do
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



}
