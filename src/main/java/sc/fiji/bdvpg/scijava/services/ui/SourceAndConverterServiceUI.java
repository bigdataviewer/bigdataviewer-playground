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

package sc.fiji.bdvpg.scijava.services.ui;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.Context;
import org.scijava.object.ObjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.SourceAndConverterServiceUITransferHandler;
import sc.fiji.bdvpg.scijava.services.ui.tree.FilterNode;
import sc.fiji.bdvpg.scijava.services.ui.tree.SourceTreeModel;
import sc.fiji.bdvpg.scijava.services.ui.tree.SourceTreeView;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Swing UI for SciJava {@link SourceAndConverterService}.
 *
 * <p>This UI displays sources in a tree structure with filtering capabilities.
 * Sources can appear multiple times in the tree, organized by different criteria
 * such as SpimData membership, Channel, Angle, etc.</p>
 *
 * <p>The architecture uses a Model-View separation:</p>
 * <ul>
 *   <li>{@link SourceTreeModel} - Thread-safe data model with no Swing dependencies</li>
 *   <li>{@link SourceTreeView} - Swing view that renders the model efficiently using incremental updates</li>
 * </ul>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class SourceAndConverterServiceUI {

	protected static final Logger logger = LoggerFactory.getLogger(
		SourceAndConverterServiceUI.class);

	/**
	 * Linked {@link SourceAndConverterService}
	 */
	final SourceAndConverterService sourceAndConverterService;

	/**
	 * SciJava context for accessing other services
	 */
	final Context context;

	/**
	 * JFrame container, could be null, see guiAvailable
	 */
	final JFrame frame;

	final boolean guiAvailable;

	/**
	 * Swing JTree used for displaying Sources object
	 */
	final JTree tree;

	/**
	 * Thread-safe source tree model
	 */
	private final SourceTreeModel sourceTreeModel;

	/**
	 * Swing view for the tree model
	 */
	private final SourceTreeView sourceTreeView;

	/**
	 * Counter for SpimData naming
	 */
	private final AtomicInteger spimDataCounter = new AtomicInteger(0);

	/**
	 * Constructor : Initialize fields + standard actions
	 *
	 * @param sourceAndConverterService the service to which this UI is linked
	 * @param context the SciJava context
	 * @param makeGUI whether to create and display the GUI
	 */
	public SourceAndConverterServiceUI(
		SourceAndConverterService sourceAndConverterService,
		Context context, boolean makeGUI)
	{
		this.sourceAndConverterService = sourceAndConverterService;
		this.context = context;

		// Create the model and view
		sourceTreeModel = new SourceTreeModel(sourceAndConverterService);
		sourceTreeView = new SourceTreeView(sourceTreeModel);

		// Registers the action which would inspect selected sources
		this.sourceAndConverterService.registerAction("Inspect Sources",
				this::inspectSources);

		tree = new JTree(sourceTreeView.getTreeModel());

		if (makeGUI) {
			frame = new JFrame("BDV Sources");
			JPanel panel = new JPanel(new BorderLayout());
			JScrollPane treeView = new JScrollPane(tree);
			panel.add(treeView, BorderLayout.CENTER);
			// Shows Popup on right click, and handles double-click to adjust view
			tree.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					super.mouseClicked(e);
					// Right Click -> popup
					if (SwingUtilities.isRightMouseButton(e)) {

						JPopupMenu popup = new SourceAndConverterPopupMenu(
								() -> getSelectedSourceAndConverters(tree)).getPopup();

						addUISpecificActions(popup);

						popup.show(e.getComponent(), e.getX(), e.getY());

					}
					// Double-click -> adjust view on selected sources
					else if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
						adjustViewOnSelectedSources();
					}
				}
			});
			// We can drag the nodes
			tree.setDragEnabled(true);
			tree.setDropMode(DropMode.ON_OR_INSERT);
			// Enables:
			// - drag -> SourceAndConverters
			// - drop -> automatically import xml BDV datasets
			tree.setTransferHandler(new SourceAndConverterServiceUITransferHandler(
				sourceTreeModel, sourceTreeView));

			// get the screen size as a java dimension
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

			// get a fixed proportion of the height and of the width
			int height = screenSize.height * 4 / 5;
			int width = screenSize.width / 6;

			// set the jFrame height and width
			frame.setPreferredSize(new Dimension(width, height));

			JLabel cacheLabel = new JLabel("Cache");

			panel.add(cacheLabel, BorderLayout.SOUTH);
			TimerTask periodicLogger = new TimerTask() {

				@Override
				public void run() {
					SwingUtilities.invokeLater(() -> cacheLabel.setText(sourceAndConverterService.getCache().toString()));
				}
			};

			Timer time = new Timer(); // Instantiate Timer Object
			time.schedule(periodicLogger, 0, 2000);

			cacheLabel.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						sourceAndConverterService.getCache().invalidateAll();
						SwingUtilities.invokeLater(() -> cacheLabel.setText("Cache cleared."));
					}
				}
			});

			frame.add(panel);
			frame.pack();
			frame.setVisible(false);
			guiAvailable = true;
		} else {
			guiAvailable = false;
			frame = null;
		}
	}

	public void show() {
		if ((guiAvailable)&&(PlaygroundPrefs.getSourceAndConverterUIVisibility())) {
			frame.setVisible(true);
		}
	}

	public void hide() {
		if (guiAvailable) {
			frame.setVisible(false);
		}
	}

	void addUISpecificActions(JPopupMenu popup) {
		// Delete node for inspection result only
		JMenuItem deleteInspectNodesMenuItem = new JMenuItem(
			"Delete Selected Inspect Nodes");
		deleteInspectNodesMenuItem.addActionListener(e -> {
			TreePath[] paths = tree.getSelectionModel().getSelectionPaths();
			if (paths != null) {
				for (TreePath tp : paths) {
					if ((tp.getLastPathComponent()).toString().startsWith(
						"Inspect Results ["))
					{
						((DefaultMutableTreeNode) tp.getLastPathComponent())
							.removeFromParent();
					}
				}
				// Refresh the tree model
				sourceTreeView.getTreeModel().reload();
			}
		});

		// Add show all item
		JMenuItem addShowAllFilterNodeMenuItem = new JMenuItem(
			"Add 'Show All' Filter Node");
		addShowAllFilterNodeMenuItem.addActionListener(e -> {
			TreePath[] paths = tree.getSelectionModel().getSelectionPaths();
			if (paths == null || paths.length != 1) {
				logger.error("Only one node should be selected");
				return;
			}
			DefaultMutableTreeNode selectedTreeNode = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
			FilterNode filterNode = sourceTreeView.getFilterNode(selectedTreeNode);
			if (filterNode != null) {
				FilterNode newNode = new FilterNode("All Sources", sac -> true, true);
				sourceTreeModel.addNode(filterNode, newNode);
			} else {
				logger.error("A filter node should be selected");
			}
		});

		JComponent child = popup;

		boolean found = false;
		for (int i = 0; i<popup.getComponentCount(); i++) {
			if (popup.getComponent(i) instanceof JMenu) {
				if (((JMenu) popup.getComponent(i)).getText().equals("Tree View")) {
					child = (JMenu) popup.getComponent(i);
					found = true;
					break;
				}
			}
		}

		if (!found) {
			JMenu menu = new JMenu("Tree View");
			popup.add(menu);
			child = menu;
		}

		child.add(deleteInspectNodesMenuItem);
		child.add(addShowAllFilterNodeMenuItem);
	}

	/**
	 * Recursive source inspection of an array of {@link SourceAndConverter} -
	 * adds a node per source which summarizes the results of the inspection
	 *
	 * @param sacs sources that should be inspected
	 */
	public void inspectSources(SourceAndConverter<?>[] sacs) {
		for (SourceAndConverter sac : sacs) {
			inspectSource(sac);
		}
	}

	/**
	 * Recursive source inspection of a {@link SourceAndConverter} - adds a node
	 * per source which summarizes the results of the inspection
	 *
	 * @param sac source to inspect
	 */
	public void inspectSource(SourceAndConverter sac) {
		if ((guiAvailable)&&(!frame.isVisible())) {
			show();
		}
		DefaultMutableTreeNode parentNodeInspect = new DefaultMutableTreeNode(
			"Inspect Results [" + sac.getSpimSource().getName() + "]");
		SourceAndConverterInspector.appendInspectorResult(parentNodeInspect, sac,
			sourceAndConverterService, false);
		// Add to tree root directly via Swing model
		DefaultMutableTreeNode treeRoot = sourceTreeView.getTreeRoot();
		treeRoot.add(parentNodeInspect);
		sourceTreeView.getTreeModel().nodesWereInserted(treeRoot, new int[]{treeRoot.getChildCount() - 1});
	}

	/**
	 * Removes BdvHandle filter nodes from the tree.
	 *
	 * @param bdvh the BdvHandle to remove
	 */
	public void removeBdvHandleNodes(BdvHandle bdvh) {
		sourceTreeModel.removeBdvHandleNodes(bdvh);
	}

	// ============ Source Management (Batch API) ============

	/**
	 * Adds a single source to the tree.
	 * For adding multiple sources, prefer {@link #addSources(Collection)} for better performance.
	 *
	 * @param sac source to add
	 */
	public void update(SourceAndConverter sac) {
		if ((guiAvailable) && (frame != null) && (!frame.isVisible())) {
			show();
		}
		// Check if new SpimData needs to be added
		updateSpimDataFilterNodes();
		sourceTreeModel.addSource(sac);
	}

	/**
	 * Adds multiple sources to the tree in a single batch operation.
	 * This is more efficient than calling {@link #update(SourceAndConverter)} multiple times.
	 *
	 * @param sources the sources to add
	 */
	public void addSources(Collection<SourceAndConverter<?>> sources) {
		if (sources.isEmpty()) return;
		if ((guiAvailable) && (frame != null) && (!frame.isVisible())) {
			show();
		}
		updateSpimDataFilterNodes();
		sourceTreeModel.addSources(sources);
	}

	/**
	 * Updates SpimData filter nodes based on currently registered SpimData sets.
	 */
	private void updateSpimDataFilterNodes() {
		Set<AbstractSpimData<?>> currentSpimdatas = sourceAndConverterService.getSpimDatasets();

		for (AbstractSpimData<?> asd : currentSpimdatas) {
			if (sourceTreeModel.getSpimDataNode(asd) == null) {
				String name = "SpimData " + spimDataCounter.getAndIncrement();
				sourceTreeModel.addSpimData(asd, name);
			}
		}
	}

	/**
	 * Renames a SpimData node in the UI.
	 *
	 * @param asd_renamed spimdata to rename
	 * @param name new name
	 */
	public void updateSpimDataName(AbstractSpimData asd_renamed, String name) {
		sourceTreeModel.renameSpimData(asd_renamed, name);
	}

	/**
	 * Removes a source from the UI.
	 *
	 * @param sac source to remove
	 */
	public void remove(SourceAndConverter sac) {
		if ((guiAvailable) && (frame != null) && (!frame.isVisible())) {
			show();
		}
		sourceTreeModel.removeSource(sac);
	}

	/**
	 * Removes multiple sources from the UI in a batch operation.
	 *
	 * @param sources sources to remove
	 */
	public void removeSources(Collection<SourceAndConverter<?>> sources) {
		if ((guiAvailable) && (frame != null) && (!frame.isVisible())) {
			show();
		}
		sourceTreeModel.removeSources(sources);
	}

	/**
	 * @param tree the tree view of sourceandconverters
	 * @return an array containing the list of all {@link SourceAndConverter}
	 *         selected by the user
	 */
	public SourceAndConverter[] getSelectedSourceAndConverters(JTree tree) {
		Set<SourceAndConverter<?>> sacList = new HashSet<>();
		TreePath[] paths = tree.getSelectionModel().getSelectionPaths();
		if (paths == null) {
			return new SourceAndConverter[0];
		}
		for (TreePath tp : paths) {
			if (((DefaultMutableTreeNode) tp.getLastPathComponent())
				.getUserObject() instanceof RenamableSourceAndConverter)
			{
				SourceAndConverter<?> userObj =
					((RenamableSourceAndConverter) ((DefaultMutableTreeNode) tp
						.getLastPathComponent()).getUserObject()).sac;
				sacList.add(userObj);
			}
			else {
				sacList.addAll(getSourceAndConvertersFromChildrenOf(
					(DefaultMutableTreeNode) tp.getLastPathComponent()));
			}
		}
		return SourceAndConverterHelper.sortDefaultGeneric(sacList).toArray(
			new SourceAndConverter<?>[0]);
	}

	/**
	 * @param node root node to get sources from
	 * @return a set containing the list of all {@link SourceAndConverter}
	 *         below the node
	 */
	public Set<SourceAndConverter<?>> getSourceAndConvertersFromChildrenOf(
		DefaultMutableTreeNode node)
	{
		Set<SourceAndConverter<?>> sacs = new HashSet<>();
		if (node.getUserObject() instanceof RenamableSourceAndConverter) {
			Object userObj = ((RenamableSourceAndConverter) (node
				.getUserObject())).sac;
			sacs.add((SourceAndConverter) userObj);
		}
		else {
			// Check if this is a filter node and get sources from the model
			FilterNode filterNode = sourceTreeView.getFilterNode(node);
			if (filterNode != null) {
				sacs.addAll(filterNode.getOutputSources());
			} else {
				// Fallback: traverse children
				for (int i = 0; i < node.getChildCount(); i++) {
					DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
					sacs.addAll(getSourceAndConvertersFromChildrenOf(child));
				}
			}
		}
		return sacs;
	}

	/**
	 * @return the Swing tree model
	 */
	public DefaultTreeModel getTreeModel() {
		return sourceTreeView.getTreeModel();
	}

	/**
	 * @return the source tree model (thread-safe data model)
	 */
	public SourceTreeModel getSourceTreeModel() {
		return sourceTreeModel;
	}

	/**
	 * @return the source tree view
	 */
	public SourceTreeView getSourceTreeView() {
		return sourceTreeView;
	}

	/**
	 * Gets the tree path from a string representation.
	 *
	 * @param path path as string (nodes separated by "&gt;")
	 * @return treepath fetched from the path
	 */
	public TreePath getTreePathFromString(String path) {
		String[] stringPath = path.split(">");
		Object[] nodes = new Object[stringPath.length];
		TreeNode current = sourceTreeView.getTreeRoot();
		int currentDepth = 0;

		while (currentDepth < stringPath.length) {
			final Enumeration children = current.children();
			boolean found = false;
			while (children.hasMoreElements()) {
				TreeNode testNode = (TreeNode) children.nextElement();
				if (testNode.toString().trim().equals(stringPath[currentDepth].trim())) {
					nodes[currentDepth] = testNode;
					currentDepth++;
					current = testNode;
					found = true;
					break;
				}
			}
			if (!found) break;
		}

		if (currentDepth == stringPath.length) {
			return new TreePath(nodes);
		} else {
			return null;
		}
	}

	/**
	 * Gets sources from a tree path.
	 *
	 * @param path path
	 * @return the list of sources in the path
	 */
	public List<SourceAndConverter<?>> getSourceAndConvertersFromTreePath(TreePath path) {
		return SourceAndConverterHelper.sortDefaultGeneric(
			getSourceAndConvertersFromChildrenOf((DefaultMutableTreeNode) path.getLastPathComponent()));
	}

	/**
	 * Gets sources from a path string.
	 *
	 * @param path path
	 * @return the list of sources in the path
	 */
	public List<SourceAndConverter<?>> getSourceAndConvertersFromPath(String path) {
		TreePath tp = getTreePathFromString(path);
		if (tp != null) {
			return getSourceAndConvertersFromTreePath(tp);
		} else {
			return new ArrayList<>();
		}
	}

	public synchronized void addNode(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode root = sourceTreeView.getTreeRoot();
		root.add(node);
		sourceTreeView.getTreeModel().nodesWereInserted(root, new int[]{root.getChildCount() - 1});
	}

	public synchronized void removeNode(DefaultMutableTreeNode node) {
		getTreeModel().removeNodeFromParent(node);
	}

	public synchronized void addNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode node) {
		parent.add(node);
		sourceTreeView.getTreeModel().nodesWereInserted(parent, new int[]{parent.getChildCount() - 1});
	}

	public Node getRoot() {
		return new Node(sourceTreeView.getTreeRoot());
	}

	public static class Node {

		private final TreeNode node;

		protected Node(TreeNode node) {
			this.node = node;
		}

		public Node child(int index) {
			return new Node(node.getChildAt(index));
		}

		public Node child(String name) {
			final Enumeration children = node.children();
			while (children.hasMoreElements()) {
				TreeNode testNode = (TreeNode) children.nextElement();
				if (testNode.toString().equals(name)) {
					return new Node(testNode);
				}
			}
			return null;
		}

		public List<Node> children() {
			ArrayList<Node> list = new ArrayList<>(node.getChildCount());
			final Enumeration children = node.children();
			while (children.hasMoreElements()) {
				TreeNode n = (TreeNode) children.nextElement();
				list.add(new Node(n));
			}
			return list;
		}

		public String name() {
			return node.toString();
		}

		@Override
		public String toString() {
			return name();
		}

		public String path() {
			String fullPath = name();
			Node parent = parent();
			while (parent != null) {
				fullPath = parent().name() + ">" + fullPath;
				parent = parent.parent();
			}
			return fullPath;
		}

		public SourceAndConverter<?>[] sources() {
			if (node instanceof DefaultMutableTreeNode) {
				DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) node;
				Object userObject = dmtn.getUserObject();
				if (userObject instanceof RenamableSourceAndConverter) {
					return new SourceAndConverter[] {
						((RenamableSourceAndConverter) userObject).sac };
				}
			}
			return null;
		}

		public Node parent() {
			if (node.getParent() == null) return null;
			return new Node(node.getParent());
		}
	}

	/**
	 * Returns the JTree used by this UI.
	 * @return the JTree component
	 */
	public JTree getTree() {
		return tree;
	}

	/**
	 * Expands all nodes in the tree.
	 */
	public void expandAll() {
		SwingUtilities.invokeLater(() -> {
			for (int i = 0; i < tree.getRowCount(); i++) {
				tree.expandRow(i);
			}
		});
	}

	/**
	 * Expands nodes up to a specified depth.
	 * @param depth the maximum depth to expand (0 = root only, 1 = root + first level, etc.)
	 */
	public void expandToDepth(int depth) {
		SwingUtilities.invokeLater(() -> expandNode(sourceTreeView.getTreeRoot(), 0, depth));
	}

	private void expandNode(TreeNode node, int currentDepth, int maxDepth) {
		if (currentDepth >= maxDepth) return;

		TreePath path = getTreePath(node);
		if (path != null) {
			tree.expandPath(path);
		}

		for (int i = 0; i < node.getChildCount(); i++) {
			expandNode(node.getChildAt(i), currentDepth + 1, maxDepth);
		}
	}

	/**
	 * Gets the TreePath for a given TreeNode.
	 * @param node the node to get the path for
	 * @return the TreePath to the node
	 */
	private TreePath getTreePath(TreeNode node) {
		List<TreeNode> path = new ArrayList<>();
		TreeNode current = node;
		while (current != null) {
			path.add(0, current);
			current = current.getParent();
		}
		return new TreePath(path.toArray());
	}

	/**
	 * Expands a path specified as a string (nodes separated by superior sign).
	 * @param pathString the path to expand
	 */
	public void expandPath(String pathString) {
		SwingUtilities.invokeLater(() -> {
			TreePath tp = getTreePathFromString(pathString);
			if (tp != null) {
				tree.expandPath(tp);
			}
		});
	}

	/**
	 * Adjusts the view of the active BDV window to show the currently selected sources.
	 */
	private void adjustViewOnSelectedSources() {
		SourceAndConverter<?>[] selectedSources = getSelectedSourceAndConverters(tree);
		if (selectedSources == null || selectedSources.length == 0) {
			return;
		}

		SourceAndConverterBdvDisplayService bdvDisplayService =
			context.getService(SourceAndConverterBdvDisplayService.class);
		if (bdvDisplayService == null) {
			return;
		}

		List<BdvHandle> bdvhs = context.getService(ObjectService.class).getObjects(BdvHandle.class);
		if ((bdvhs == null) || (bdvhs.isEmpty())) {
			return;
		}

		BdvHandle activeBdv = bdvDisplayService.getActiveBdv();
		if (activeBdv == null) {
			return;
		}

		new ViewerTransformAdjuster(activeBdv, selectedSources, 300).run();
	}
}
