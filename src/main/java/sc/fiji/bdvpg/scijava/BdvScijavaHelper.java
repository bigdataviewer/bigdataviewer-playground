/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2024 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.scijava;

import bdv.util.BdvHandle;
import bdv.util.BdvHandleFrame;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class that can be used to put a SciJava Command into the menu bar of a
 * BdvHandle
 */

public class BdvScijavaHelper {

	protected static final Logger logger = LoggerFactory.getLogger(
		BdvScijavaHelper.class);

	static public void clearBdvHandleMenuBar(BdvHandle bdvh) {
		if (bdvh instanceof BdvHandleFrame) {
			final JMenuBar bdvMenuBar = ((BdvHandleFrame) bdvh).getBigDataViewer()
				.getViewerFrame().getJMenuBar();
			int componentsNumber = bdvMenuBar.getComponentCount();
			for (int i = 0; i < componentsNumber; i++) {
				bdvMenuBar.remove(0);
			}
			bdvMenuBar.revalidate();
			bdvMenuBar.updateUI();
		}
	}

	// https://github.com/scijava/scijava-ui-swing/blob/557d79230d0f70eaae8573e242f0a02c66b07d62/src/main/java/org/scijava/ui/swing/AbstractSwingUI.java
	static public void addCommandToBdvHandleMenu(BdvHandle bdvh, Context ctx,
		Class<? extends Command> commandClass, int skipTopLevels, Object... args)
	{
		Plugin plugin = commandClass.getDeclaredAnnotation(Plugin.class);
		addActionToBdvHandleMenu(bdvh, plugin.menuPath(), skipTopLevels, () -> ctx
			.getService(CommandService.class).run(commandClass, true, args));
	}

	static public void addSeparator(BdvHandle bdvh,	String pathHierarchy) {
		if (bdvh instanceof BdvHandleFrame) {
			final JMenuBar bdvMenuBar = ((BdvHandleFrame) bdvh).getBigDataViewer()
					.getViewerFrame().getJMenuBar();
			List<String> path = Arrays.stream(pathHierarchy.split(">")).map(
					String::trim).collect(Collectors.toList());
			path.add("Separator"); // Simplify my life : direct reuse of the method below
			JMenuItem jMenuItemRoot = findOrCreateJMenu(bdvMenuBar, path);
			if (jMenuItemRoot != null) {
				final JSeparator jSeparator = new JSeparator();
				jMenuItemRoot.add(jSeparator);
				bdvMenuBar.updateUI();
			}
			else {
				logger.error("Could not find or create jmenu (" + bdvMenuBar + ", " +
						path + ")");
			}
		}
		else {
			logger.error(
					"Cannot put command on menu : the bdvhandle is not a frame.");
		}
	}

	static public void addCommandToBdvHandleMenu(BdvHandle bdvh, Context ctx, String path,
												 Class<? extends Command> commandClass, Object... args)
	{
		addActionToBdvHandleMenu(bdvh, path, 0, () -> ctx
				.getService(CommandService.class).run(commandClass, true, args));
	}

	static public void addActionToBdvHandleMenu(BdvHandle bdvh,
		String pathHierarchy, int skipTopLevels, Runnable runnable)
	{
		if (bdvh instanceof BdvHandleFrame) {
			final JMenuBar bdvMenuBar = ((BdvHandleFrame) bdvh).getBigDataViewer()
				.getViewerFrame().getJMenuBar();
			List<String> path = Arrays.stream(pathHierarchy.split(">")).map(
				String::trim).collect(Collectors.toList());

			if (skipTopLevels > 0) {
				path.subList(0, skipTopLevels).clear();
			}

			JMenuItem jMenuItemRoot = findOrCreateJMenu(bdvMenuBar, path);
			if (jMenuItemRoot != null) {
				final JMenuItem jMenuItem = new JMenuItem(path.get(path.size() - 1));
				jMenuItem.addActionListener(e -> runnable.run());
				jMenuItemRoot.add(jMenuItem);
				bdvMenuBar.updateUI();
			}
			else {
				logger.error("Could not find or create jmenu (" + bdvMenuBar + ", " +
					path + ")");
			}
		}
		else {
			logger.error(
				"Cannot put command on menu : the bdvhandle is not a frame.");
		}
	}

	private static JMenu findOrCreateJMenu(JMenuBar bdvMenuBar,
		List<String> path)
	{
		if (path.isEmpty()) {
			logger.error("No Path specified in find or create JMenu!");
		}
		if (path.size() == 1) {
			// TODO or not ? It means the action is on the top level window
			return null;
		}
		boolean found = false;
		int idx = 0;
		while ((idx < bdvMenuBar.getMenuCount()) && (!found)) {
			JMenu jmenuTest = bdvMenuBar.getMenu(idx);
			if (jmenuTest.getText().equals(path.get(0))) {
				found = true;
			}
			else {
				idx++;
			}
		}
		JMenu jmenu;
		if (found) {
			jmenu = bdvMenuBar.getMenu(idx);
		}
		else {
			jmenu = new JMenu(path.get(0));
			bdvMenuBar.add(jmenu);
		}
		path.remove(0);
		return findOrCreateJMenu(jmenu, path);
	}

	private static JMenu findOrCreateJMenu(JMenu jMenu, List<String> path) {
		if (path.isEmpty()) {
			logger.error(" Reached unreachable statement !");
		}
		if (path.size() == 1) {
			return jMenu;
		}
		int idx = 0;
		while (idx < jMenu.getMenuComponentCount()) {
			Component component = jMenu.getMenuComponent(idx);
			if (component instanceof JMenu) {
				JMenu jmi = (JMenu) component;
				if (path.get(0).equals(jmi.getText())) {
					path.remove(0);
					return findOrCreateJMenu(jmi, path);
				}
			}
			idx++;
		}

		JMenu jm = new JMenu(path.get(0));
		jMenu.add(jm);
		path.remove(0);
		return findOrCreateJMenu(jm, path);
	}
}
