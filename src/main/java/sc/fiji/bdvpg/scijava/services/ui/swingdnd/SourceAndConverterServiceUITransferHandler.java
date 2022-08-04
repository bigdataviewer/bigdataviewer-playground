/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.fiji.bdvpg.scijava.services.ui.swingdnd;

import bdv.ui.SourcesTransferable;
import bdv.viewer.SourceAndConverter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;
import sc.fiji.bdvpg.scijava.services.ui.SourceFilterNode;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class which allows Drag and Drop in the tree UI of source and converter.
 *
 * XML BDV Dataset can be dragged into the tree
 *
 * Some nodes can be dragged to create custom filters in the UI (sources cannot be dragged)
 *
 */

public class SourceAndConverterServiceUITransferHandler extends TreeTransferHandler {

    protected static Logger logger = LoggerFactory.getLogger(SourceAndConverterServiceUITransferHandler.class);

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
            logger.debug("ClassNotFound: " + e.getMessage());
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
            //List<DefaultMutableTreeNode> toRemove = new ArrayList<>();
            DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) paths[0].getLastPathComponent();
            DefaultMutableTreeNode copy = copy(node);
            copies.add(copy);
            //toRemove.add(node);
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
                    //toRemove.add(next);
                }
            }
            DefaultMutableTreeNode[] nodes =
                    copies.toArray(new DefaultMutableTreeNode[0]);
            /*DefaultMutableTreeNode[] nodesToRemove =
                    toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);*/
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
                for (SourceAndConverter sac : ui.getSelectedSourceAndConverters((JTree) c)) {
                    sacs.add(sac);
                }
                //Collections.addAll(sacs, ui.getSelectedSourceAndConverters()); // Do not work, even if intellij suggests it
                extT.setSourcesList(sacs);
            }
            return extT;
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
        return null;
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
                List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                for (File f : files) {
                    if (f.getAbsolutePath().endsWith(".xml")) {
                        new SpimDataFromXmlImporter(f).run();
                    } else {
                        logger.info("Unsupported drop operation with file " + f.getAbsolutePath());
                    }
                }
            } else if (t.isDataFlavorSupported(nodesFlavor)) {
                DefaultMutableTreeNode[] nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
                if (nodes.length!=1) {
                    logger.info("Only one node should be dragged");
                    return false;
                }
                if ((nodes[0]) instanceof SourceFilterNode) {

                    JTree.DropLocation dl = (JTree.DropLocation) supp.getDropLocation();
                    TreePath dest = dl.getPath();

                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();

                    SourceFilterNode sfn = (SourceFilterNode) (nodes[0]);
                    parent.add(sfn);

                    return true;

                } else {
                    logger.debug("A source filter node should be selected");
                    logger.debug("You have selected a node of class "+nodes[0].getClass().getName());
                    return false;
                }

            }
        } catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static class ExtTransferable implements Transferable {

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

        @NotNull
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
