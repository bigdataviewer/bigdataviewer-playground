/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
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

	protected static final Logger logger = LoggerFactory.getLogger(
		TreeTransferHandler.class);

	DataFlavor nodesFlavor;
	final DataFlavor[] flavors = new DataFlavor[1];
	// DefaultMutableTreeNode[] nodesToRemove;

	public TreeTransferHandler() {
		try {
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" +
				DefaultMutableTreeNode[].class.getName() + "\"";
			nodesFlavor = new DataFlavor(mimeType);
			flavors[0] = nodesFlavor;
		}
		catch (ClassNotFoundException e) {
			logger.error("ClassNotFound: " + e.getMessage());
		}
	}

	// TransferHandler
	@Override
	public int getSourceActions(JComponent c) {
		return TransferHandler.COPY;
	}

	// TransferHandler
	@Override
	public boolean canImport(JComponent comp, DataFlavor[] flavor) {
		for (DataFlavor dataFlavor : flavor) {
			for (DataFlavor value : flavors) {
				if (dataFlavor.equals(value)) {
					return true;
				}
			}
		}
		return false;
	}

	// TransferHandler
	@Override
	protected Transferable createTransferable(JComponent c) {
		JTree tree = (JTree) c;
		TreePath[] paths = tree.getSelectionPaths();
		if (paths != null) {
			List<DefaultMutableTreeNode> copies = new ArrayList<>();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0]
				.getLastPathComponent();
			DefaultMutableTreeNode copy = copy(node);
			copies.add(copy);
			for (int i = 1; i < paths.length; i++) {
				DefaultMutableTreeNode next = (DefaultMutableTreeNode) paths[i]
					.getLastPathComponent();
				// Do not allow higher level nodes to be added to list.
				if (next.getLevel() < node.getLevel()) {
					break;
				}
				else if (next.getLevel() > node.getLevel()) { // child node
					copy.add(copy(next));
					// node already contains child
				}
				else { // sibling
					copies.add(copy(next));
				}
			}
			DefaultMutableTreeNode[] nodes = copies.toArray(
				new DefaultMutableTreeNode[0]);

			return new NodesTransferable(nodes);
		}
		return null;
	}

	/** Defensive copy used in createTransferable. */
	private DefaultMutableTreeNode copy(TreeNode node) {
		return new DefaultMutableTreeNode(node);
	}

	// TransferHandler
	@Override
	public boolean importData(TransferHandler.TransferSupport support) {
		if (!canImport(support)) {
			return false;
		}
		// Extract transfer data.
		DefaultMutableTreeNode[] nodes = null;
		try {
			Transferable t = support.getTransferable();
			nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
		}
		catch (UnsupportedFlavorException ufe) {
			logger.debug("UnsupportedFlavor: " + ufe.getMessage());
		}
		catch (java.io.IOException ioe) {
			logger.error("I/O error: " + ioe.getMessage());
		}
		// Get drop location info.
		int childIndex;
		TreePath dest;
		if (support.isDrop()) {
			JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
			childIndex = dl.getChildIndex();
			dest = dl.getPath();
		}
		else {
			childIndex = -1;
			JTree tree = (JTree) support.getComponent();
			dest = tree.getSelectionPath();
		}
		assert dest != null;
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest
			.getLastPathComponent();
		JTree tree = (JTree) support.getComponent();
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
		// Configure for drop mode.
		int index = childIndex; // DropMode.INSERT
		if (childIndex == -1) { // DropMode.ON
			index = parent.getChildCount();
		}
		// Add data to model.
		assert nodes != null;
		for (DefaultMutableTreeNode node : nodes) {
			// ArrayIndexOutOfBoundsException
			model.insertNodeInto(node, parent, index++);
		}
		return true;
	}

	// TransferHandler
	@Override
	public boolean importData(JComponent comp, Transferable t) {
		return importData(new TransferHandler.TransferSupport(comp, t));
	}

	public class NodesTransferable implements Transferable {

		final DefaultMutableTreeNode[] nodes;

		public NodesTransferable(DefaultMutableTreeNode[] nodes) {
			this.nodes = nodes;
		}

		// Transferable
		@Override
		public Object getTransferData(DataFlavor flavor) {
			if (!isDataFlavorSupported(flavor)) {
				return false;
			}
			return nodes;
		}

		// Transferable
		@Override
		public DataFlavor[] getTransferDataFlavors() {
			return flavors;
		}

		// Transferable
		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(nodesFlavor);
		}
	}
}
