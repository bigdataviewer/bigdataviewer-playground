/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.RenamableSourceAndConverter;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Allows dropping SourceAndConverter from the SourceAndConverterServiceUI into
 * the bdv windows Allows importing nodes from the SourceAndConverterServiceUI
 * JTree
 */

public class JListTransferHandler extends TransferHandler {

	protected static final Logger logger = LoggerFactory.getLogger(
		JListTransferHandler.class);

	DataFlavor nodesFlavor;
	final DataFlavor[] flavors = new DataFlavor[2];

	public JListTransferHandler() {
		try {
			String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" +
				DefaultMutableTreeNode[].class.getName() + "\"";
			nodesFlavor = new DataFlavor(mimeType);
			flavors[0] = nodesFlavor;
			flavors[1] = SourcesTransferable.flavor;
		}
		catch (ClassNotFoundException e) {
			logger.warn("ClassNotFound: " + e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	public void updateDropLocation(TransferSupport support, DropLocation dl) {
		// Do nothing : can be extended for custom behaviour
	}

	public Optional<BdvHandle> getBdvHandleFromViewerPanel(
		ViewerPanel viewerPanel)
	{
		return SourceAndConverterServices.getBDVService().getViewers()
			.stream().filter(bdvh -> bdvh.getViewerPanel().equals(viewerPanel))
			.findFirst();
	}

	public boolean canImport(TransferSupport support) {

		if (support.getComponent() instanceof JComponent) {
			for (int i = 0, n = support.getDataFlavors().length; i < n; i++) {
				for (DataFlavor flavor : flavors) {
					if (support.getDataFlavors()[i].equals(flavor)) {
						DropLocation dl = support.getDropLocation();
						updateDropLocation(support, dl);
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean importData(TransferSupport support) {
		if (!canImport(support)) {
			return false;
		}

		// See if we can get the SourcesTransferable flavor
		if (support.getTransferable().isDataFlavorSupported(
			SourcesTransferable.flavor))
		{
			try {
				final List<SourceAndConverter<?>> sacs = SourceAndConverterHelper
					.sortDefaultGeneric(((SourcesTransferable.SourceList) support
						.getTransferable().getTransferData(SourcesTransferable.flavor))
							.getSources());
				if (support.getComponent() instanceof JList) {
					JList target = (JList) (support.getComponent());
					DefaultListModel model = (DefaultListModel) (target.getModel());
					JList.DropLocation dropLocation = (JList.DropLocation) support
						.getDropLocation();
					int idxInsert = dropLocation.getIndex();
					for (SourceAndConverter source : sacs) {
						model.add(idxInsert, source);
						idxInsert++;
					}
					return true;
				}
				else return false;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {

			// Extract transfer data.
			DefaultMutableTreeNode[] nodes = null;
			try {
				Transferable t = support.getTransferable();
				nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
			}
			catch (UnsupportedFlavorException ufe) {
				logger.warn("UnsupportedFlavor: " + ufe.getMessage());
			}
			catch (java.io.IOException ioe) {
				logger.error("I/O error: " + ioe.getMessage());
			}

			if (SourceAndConverterServices
				.getSourceAndConverterService() instanceof SourceAndConverterService)
			{
				List<SourceAndConverter<?>> sacs = new ArrayList<>();
				SourceAndConverterServiceUI ui =
					((SourceAndConverterService) SourceAndConverterServices
						.getSourceAndConverterService()).getUI();

				for (DefaultMutableTreeNode node : Objects.requireNonNull(nodes)) {
					DefaultMutableTreeNode unwrapped = (DefaultMutableTreeNode) (node
						.getUserObject());
					if (unwrapped
						.getUserObject() instanceof RenamableSourceAndConverter)
					{
						sacs.add(((RenamableSourceAndConverter) unwrapped
							.getUserObject()).sac);
					}
					else {
						for (SourceAndConverter sac : ui
							.getSourceAndConvertersFromChildrenOf(unwrapped))
						{
							// noinspection UseBulkOperation
							sacs.add(sac);
						}
					}
				}
				if (support.getComponent() instanceof JList) {
					JList target = (JList) (support.getComponent());
					DefaultListModel model = (DefaultListModel) (target.getModel());
					JList.DropLocation dropLocation = (JList.DropLocation) support
						.getDropLocation();
					int idxInsert = dropLocation.getIndex();
					for (SourceAndConverter source : sacs) {
						model.add(idxInsert, source);
						idxInsert++;
					}
					return true;
				}
				else {
					return false;
				}
			}
			else {
				// Unsupported drop
				return false;
			}
		}
		return false;

	}

	Function<JComponent, Transferable> transferableSupplier = null;

	public void setTransferableFunction(
		Function<JComponent, Transferable> transferableSupplier)
	{
		this.transferableSupplier = transferableSupplier;
	}

	@Override
	protected Transferable createTransferable(JComponent c) {
		logger.debug("Create BDV Transferable");
		if (transferableSupplier != null) {
			return transferableSupplier.apply(c);
		}
		else {
			return super.createTransferable(c);
		}
	}

	@Override
	public int getSourceActions(JComponent c) {
		return COPY_OR_MOVE;
	}
}
