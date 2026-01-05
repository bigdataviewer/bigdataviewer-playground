/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import bdv.util.BdvHandleFrame;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;

/**
 * TODO : demo + is this an action ?
 */

public class MenuAdder {

	private final BdvHandle bdvHandle;
	private final ActionListener actionListener;

	public MenuAdder(BdvHandle bdvHandle, ActionListener actionListener) {
		System.setProperty("apple.laf.useScreenMenuBar", "false");

		this.bdvHandle = bdvHandle;
		this.actionListener = actionListener;
	}

	public void addMenu(String menuText, String menuItemText) {
		final JMenu jMenu = createMenuItem(menuText, menuItemText);
		final JMenuBar bdvMenuBar = ((BdvHandleFrame) bdvHandle).getBigDataViewer()
			.getViewerFrame().getJMenuBar();
		bdvMenuBar.add(jMenu);
		bdvMenuBar.updateUI();
	}

	public JMenu createMenuItem(String menuText, String menuItemText) {
		final JMenu jMenu = new JMenu(menuText);
		final JMenuItem jMenuItem = new JMenuItem(menuItemText);
		jMenuItem.addActionListener(actionListener);
		jMenu.add(jMenuItem);
		return jMenu;
	}
}
