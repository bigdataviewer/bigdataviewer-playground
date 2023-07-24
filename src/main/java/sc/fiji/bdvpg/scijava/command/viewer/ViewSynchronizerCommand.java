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

package sc.fiji.bdvpg.scijava.command.viewer;

import bdv.viewer.AbstractViewerPanel;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.viewer.ViewerHelper;
import sc.fiji.bdvpg.viewer.ViewerTransformSyncStarter;
import sc.fiji.bdvpg.viewer.ViewerTransformSyncStopper;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * I wanted to do this as an Interactive Command but there's no callback when an
 * interactive command is closed (bug
 * https://github.com/scijava/scijava-common/issues/379) so we cannot stop the
 * synchronization appropriately. Hence the dirty JFrame the user has to close
 * to stop synchronization ... TODO fix potential memory leaks which could be a
 * consequence of this extra JFrame author Nicolas Chiaruttini, BIOP, EPFL, 2020
 */

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menuPath = ScijavaBdvDefaults.RootMenu + "Synchronize Views",
	description = "Synchronizes the view of a set of BDV or BVV windows. A window popup should be closed" +
		" to stop the synchronization")
public class ViewSynchronizerCommand implements BdvPlaygroundActionCommand {

	protected static final Logger logger = LoggerFactory.getLogger(
		ViewSynchronizerCommand.class);

	@Parameter(label = "Select Viewers to Synchronize", required = false, persist = false)
	AbstractViewerPanel[] viewers;

	@Parameter(label = "Synchronize timepoints")
	boolean synchronizetime = true;

	ViewerTransformSyncStarter sync;

	public void run() {
		CommandService cm;

		if (viewers == null) viewers = new AbstractViewerPanel[0];
		if (viewers.length < 2) {
			logger.error("You should select at least 2 viewers!");
			return;
		}

		// Starting synchronization of selected bdvhandles
		sync = new ViewerTransformSyncStarter(viewers, synchronizetime);
		if (viewers.length > 0) {
			sync.setHandleInitialReference(
				SourceAndConverterServices.getBdvDisplayService().getActiveBdv().getViewerPanel());
		}
		sync.run();

		// JFrame serving the purpose of stopping synchronization when it is being
		// closed
		JFrame frameStopSync = new JFrame();
		frameStopSync.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				new ViewerTransformSyncStopper(sync.getSynchronizers(), sync
					.getTimeSynchronizers()).run();
				e.getWindow().dispose();
			}
		});
		frameStopSync.setTitle("Close window to stop synchronization");

		// Building JFrame with a simple panel and textarea
		String text = "";
		for (AbstractViewerPanel viewer : viewers) {
			text += ViewerHelper.getViewerTitle(viewer) + "\n";
		}

		JPanel pane = new JPanel();
		JTextArea textArea = new JTextArea(text);
		textArea.setEditable(false);
		pane.add(textArea);
		frameStopSync.add(pane);
		frameStopSync.setPreferredSize(new Dimension(600, 100));

		frameStopSync.pack();
		frameStopSync.setVisible(true);
	}

}
