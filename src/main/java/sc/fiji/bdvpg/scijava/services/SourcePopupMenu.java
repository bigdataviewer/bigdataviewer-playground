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

package sc.fiji.bdvpg.scijava.services;

import bdv.viewer.SourceAndConverter;
import org.scijava.Context;
import org.scijava.MenuEntry;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.PluginService;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.command.workspace.TreeSourceServiceShowCommand;
import sc.fiji.bdvpg.scijava.BdvPgMenus;
import sc.fiji.bdvpg.services.SourceServices;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SourcePopupMenu {

	private final JPopupMenu popup;
	private final Supplier<SourceAndConverter<?>[]> sources_supplier;

	String[] popupActionWithPaths;

	/**
	 * Builds the popup menu by discovering all {@link BdvPlaygroundActionCommand}
	 * plugins via the SciJava context. The commands are sorted by their menu path
	 * after stripping the {@link BdvPgMenus#RootMenu} prefix.
	 *
	 * @param sources_supplier supplier of sources to act on
	 * @param context the SciJava context used to discover commands
	 */
	public SourcePopupMenu(
		Supplier<SourceAndConverter<?>[]> sources_supplier, Context context)
	{
		this.sources_supplier = sources_supplier;

		PluginService pluginService = context.getService(PluginService.class);
		CommandService commandService = context.getService(CommandService.class);
		String rootPrefix = BdvPgMenus.RootMenu;

		this.popupActionWithPaths = pluginService
			.getPluginsOfType(Command.class).stream()
			.filter(pi -> !pi.getClassName().equals(TreeSourceServiceShowCommand.class.getName()))
			.map(pi -> commandService.getCommand(pi.getClassName()))
			.filter(ci -> ci != null && ci.getMenuPath() != null && !ci.getMenuPath().isEmpty())
			.filter(ci -> ci.getMenuPath().stream().map(MenuEntry::getName).collect(Collectors.joining(">")).startsWith(rootPrefix))
			.sorted((a, b) -> {
				int minLen = Math.min(a.getMenuPath().size(), b.getMenuPath().size());
				for (int i = 0; i < minLen; i++) {
					double wA = a.getMenuPath().get(i).getWeight();
					double wB = b.getMenuPath().get(i).getWeight();
					int cmp = Double.compare(
						Double.isNaN(wA) ? Double.MAX_VALUE : wA,
						Double.isNaN(wB) ? Double.MAX_VALUE : wB);
					if (cmp != 0) return cmp;
					cmp = a.getMenuPath().get(i).getName()
						.compareTo(b.getMenuPath().get(i).getName());
					if (cmp != 0) return cmp;
				}
				return Integer.compare(a.getMenuPath().size(), b.getMenuPath().size());
			})
			.map(ci -> ci.getMenuPath().stream().map(MenuEntry::getName).collect(Collectors.joining(">")).substring(rootPrefix.length()))
			.filter(s -> !s.isEmpty())
			.toArray(String[]::new);

		this.popup = new JPopupMenu();
		this.menuRoot = new DefaultMutableTreeNode(popup);
		createPopupMenu();
	}

	/**
	 * Convenience constructor that obtains the context from
	 * {@link SourceServices}.
	 *
	 * @param sources_supplier supplier of sources to act on
	 */
	public SourcePopupMenu(
		Supplier<SourceAndConverter<?>[]> sources_supplier)
	{
		this(sources_supplier, SourceServices.getContext());
	}

	private void createPopupMenu() {
		for (String actionNameWithPath : popupActionWithPaths) {
			String[] path = actionNameWithPath.split(">");
			String actionName = path[path.length - 1];
			if (actionName.equals("PopupLine")) {
				this.addPopupLine(path);
			}
			else {
				this.addPopupAction(actionNameWithPath,
					SourceServices.getSourceService().getAction(
						actionName.trim()));
			}
		}
	}

	/**
	 * Adds a separator in the popup menu.
	 *
	 * @param path the menu path where the separator should be added
	 */
	public void addPopupLine(String[] path) {
		JComponent component = this.getNodeFromPath(path);
		if (component instanceof JPopupMenu) {
			((JPopupMenu) component).addSeparator();
		}
		else if (component instanceof JMenu) {
			((JMenu) component).addSeparator();
		}
		else {
			System.err.println("Unexpected menu class: " + component.getClass()
				.getSimpleName());
		}
	}

	final DefaultMutableTreeNode menuRoot;

	/**
	 * Adds a menu item and an action which consumes all the selected
	 * SourceAndConverter objects in the popup menu.
	 *
	 * @param actionNameWithPath action name including menu path
	 * @param action action method
	 */
	public void addPopupAction(String actionNameWithPath,
		Consumer<SourceAndConverter<?>[]> action)
	{
		String[] pathsAndAction = actionNameWithPath.split(">");
		String actionName = pathsAndAction[pathsAndAction.length - 1];

		JMenuItem menuItem = new JMenuItem(actionName);
		if (action != null) {
			menuItem.addActionListener(e -> action.accept(sources_supplier.get()));
		}
		else {
			menuItem.addActionListener(e -> System.err.println(
				"No action defined for action named " + actionName));
		}
		getNodeFromPath(pathsAndAction).add(menuItem);
	}

	private JComponent getNodeFromPath(String[] pathsAndAction) {
		DefaultMutableTreeNode currentNode = menuRoot;
		int idx = 0;
		while (idx < pathsAndAction.length - 1) {
			String currentPath = pathsAndAction[idx];
			Enumeration en = currentNode.children();
			boolean found = false;
			while (en.hasMoreElements()) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();
				if (node.getUserObject() instanceof JMenu) {
					JMenu menuEntry = (JMenu) node.getUserObject();
					if (menuEntry.getText().equals(currentPath)) {
						currentNode = node;
						found = true;
						break;
					}
				}
			}
			if (!found) {
				JMenu entry = new JMenu(currentPath);
				if (currentNode.getUserObject() instanceof JMenu) {
					((JMenu) (currentNode.getUserObject())).add(entry);
				}
				else if (currentNode.getUserObject() instanceof JPopupMenu) {
					((JPopupMenu) (currentNode.getUserObject())).add(entry);
				}
				else {
					System.err.println("Unexpected menu class: " + currentNode
						.getUserObject().getClass().getSimpleName());
				}
				DefaultMutableTreeNode newEntry = new DefaultMutableTreeNode(entry);
				currentNode.add(newEntry);
				currentNode = newEntry;
			}
			idx++;
		}
		return (JComponent) currentNode.getUserObject();
	}

	public JPopupMenu getPopup() {
		return popup;
	}
}