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

package sc.fiji.bdvpg.scijava.services.ui;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.base.NamedEntity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.PlaygroundPrefs;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.SourceAndConverterServiceUITransferHandler;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * Swing UI for SciJava {@link SourceAndConverterService} All the
 * {@link SourceAndConverter} are inserted into the tree potentially multiple
 * times, in order to allow for multiple useful hierarchy to appear. The most
 * basic example is a SourceAndConverter which is part of a SpimData object.
 * This source will be caught by the appropriate {@link SpimDataFilterNode} in
 * which it will appear again multiple times, sorted by {@link Entity} class
 * found in the spimdata object (Illumination, Angle, Channel ...) Nodes are
 * {@link DefaultMutableTreeNode} containing potentially: -
 * {@link RenamableSourceAndConverter} (SourceAndConverter with an overridden
 * toString method) - Filtering nodes : {@link SourceFilterNode} nodes that can
 * filter SourceAndConverters, they contain a {@link Predicate}
 * {@link SourceAndConverter} that decides whether a SourceAndConverter object
 * should be included in the tree nodes below this filter; - Filtering nodes can
 * conveniently be chained in order to make complex filtering easily.
 * SourceFilterNode can be dragged and dropped in the tree in order to make
 * these sub groups SourceFilterNodes contains a flag which decides whether the
 * filtered nodes should be displayed directly below the node or not. Usually it
 * is more convenient to create a non filtering SourceFilterNode at the end of
 * the branch. - Programmatically new SourceFilterNode(model, "AllSources", () -
 * true, true); - Or with a Right-Click 'Create a show all filter node' NB : All
 * SourceFilterNodes contains a reference to the tree model, this is used to
 * fire only necessary tree update fire events - and in the EDT thread only in
 * order to avoid many exceptions WARNING ! It is probably not safe to modify in
 * a parallel fashion this UI. TODO : Documentation for inspection - Getter Node
 * for Source properties. For instance in the inspect method, a Transformed
 * Source will create a Node for the wrapped source and another node which holds
 * a getter for the fixedAffineTransform
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
	 * JFrame container
	 */
	final JFrame frame;

	/**
	 * JPanel container
	 */
	final JPanel panel;

	/**
	 * Swing JTree used for displaying Sources object
	 */
	final JTree tree;

	/**
	 * Tree root note
	 */
	final SourceFilterNode top;

	/**
	 * Scrollpane to display the JTree, if too big
	 */
	final JScrollPane treeView;

	/**
	 * Tree model
	 */
	private final DefaultTreeModel model;

	/**
	 * Spimdata Filter nodes currently present in the tree
	 */
	final List<SpimDataFilterNode> spimdataFilterNodes = new ArrayList<>();

	/**
	 * Constructor : Initialize fields + standard actions
	 *
	 * @param sourceAndConverterService the service to which this UI is linked
	 */
	public SourceAndConverterServiceUI(
		SourceAndConverterService sourceAndConverterService)
	{
		this.sourceAndConverterService = sourceAndConverterService;

		frame = new JFrame("BDV Sources");
		panel = new JPanel(new BorderLayout());

		// Tree view of Spimdata
		top = new SourceFilterNode(null, "Sources", (sac) -> true, false);
		model = new DefaultTreeModel(top);
		top.model = model;

		tree = new JTree(model);
		tree.setCellRenderer(new SourceAndConverterTreeCellRenderer());

		SourceFilterNode outsideSpimDataSources = new SourceFilterNode(model,
			"Other Sources", (sac) -> !sourceAndConverterService.containsMetadata(sac,
				SPIM_DATA_INFO), true);
		top.add(outsideSpimDataSources);

		treeView = new JScrollPane(tree);

		panel.add(treeView, BorderLayout.CENTER);

		// Shows Popup on right click
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

					// });
				}
			}
		});

		// Registers the action which would
		this.sourceAndConverterService.registerAction("Inspect Sources",
			this::inspectSources);

		// We can drag the nodes
		tree.setDragEnabled(true);
		tree.setDropMode(DropMode.ON_OR_INSERT);
		// Enables:
		// - drag -> SourceAndConverters
		// - drop -> automatically import xml BDV datasets
		tree.setTransferHandler(new SourceAndConverterServiceUITransferHandler());

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
				SwingUtilities.invokeLater(() -> {
					cacheLabel.setText(sourceAndConverterService.getCache().toString());
				});
			}
		};

		Timer time = new Timer(); // Instantiate Timer Object
		time.schedule(periodicLogger, 0, 2000);

		cacheLabel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					sourceAndConverterService.getCache().invalidateAll();
					SwingUtilities.invokeLater(() -> {
						cacheLabel.setText("Cache cleared.");
					});
				}
			}
		});

		frame.add(panel);
		frame.pack();
		frame.setVisible(false);
		// frame.setVisible(true);

		// TODO : read playground prefs from ij
		// PrefsPlaygroundPrefs.setSourceAndConverterUIVisibility();

	}

	public void show() {
		if (PlaygroundPrefs.getSourceAndConverterUIVisibility()) {
			frame.setVisible(true);
		}
	}

	public void hide() {
		frame.setVisible(false);
	}

	SourceFilterNode copiedNode = null;

	void addUISpecificActions(JPopupMenu popup) {
		// Delete node for inspection result only
		JMenuItem deleteInspectNodesMenuItem = new JMenuItem(
			"Delete Selected Inspect Nodes");
		deleteInspectNodesMenuItem.addActionListener(e -> {
			for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
				if ((tp.getLastPathComponent()).toString().startsWith(
					"Inspect Results ["))
				{
					// TODO Fix The little guy who would name its source "Inspect Results
					// [whatever"
					((DefaultMutableTreeNode) tp.getLastPathComponent())
						.removeFromParent();
				}
			}
		});

		// Copy : only if a single node is selected, and if it is of class
		// SourceFilterNode
		JMenuItem copyFilterNodeMenuItem = new JMenuItem("Copy Filter Node");
		copyFilterNodeMenuItem.addActionListener(e -> {
			TreePath[] paths = tree.getSelectionModel().getSelectionPaths();
			if (paths.length != 1) {
				logger.error("Only one node should be selected");
				return;
			}
			if ((paths[0].getLastPathComponent()) instanceof SourceFilterNode) {
				copiedNode = (SourceFilterNode) ((SourceFilterNode) (paths[0]
					.getLastPathComponent())).clone();
			}
			else {
				logger.error("A source filter node should be selected");
			}

		});

		// Paste : only if a single node is selected, and if it is of class
		// SourceFilterNode
		JMenuItem pasteFilterNodeMenuItem = new JMenuItem("Paste Filter Node");
		pasteFilterNodeMenuItem.addActionListener(e -> {
			TreePath[] paths = tree.getSelectionModel().getSelectionPaths();
			if (paths.length != 1) {
				logger.error("Only one node should be selected");
				return;
			}
			if ((paths[0].getLastPathComponent()) instanceof SourceFilterNode) {
				SourceFilterNode sfn = ((SourceFilterNode) (paths[0]
					.getLastPathComponent()));
				sfn.add(copiedNode);
			}
			else {
				logger.error("A source filter node should be selected");
			}
		});

		// Delete filter nodes
		JMenuItem deleteFilterNodesMenuItem = new JMenuItem(
			"Delete Filter Node(s)");
		deleteFilterNodesMenuItem.addActionListener(e -> {
			TreePath[] paths = tree.getSelectionModel().getSelectionPaths();

			Stream.of(paths).map(TreePath::getLastPathComponent).filter(
				n -> n instanceof SourceFilterNode).forEach(n -> {
					SourceFilterNode sfn = (SourceFilterNode) n;
					if (sfn.equals(top)) {
						logger.error("The root can't be deleted");
					}
					else {
						sfn.removeFromParent();
					}
				});
		});

		// Add show all item
		JMenuItem addShowAllFilterNodeMenuItem = new JMenuItem(
			"Add 'Show All' Filter Node");
		addShowAllFilterNodeMenuItem.addActionListener(e -> {
			TreePath[] paths = tree.getSelectionModel().getSelectionPaths();
			if (paths.length != 1) {
				logger.error("Only one node should be selected");
				return;
			}
			if ((paths[0].getLastPathComponent()) instanceof SourceFilterNode) {

				SourceFilterNode sfn = (SourceFilterNode) (paths[0]
					.getLastPathComponent());
				SourceFilterNode newNode = new SourceFilterNode(model, "All sources", (
					sac) -> true, true);
				sfn.add(newNode);

			}
			else {
				logger.error("A source filter node should be selected");
			}
		});

		popup.add(deleteInspectNodesMenuItem);
		popup.add(copyFilterNodeMenuItem);
		popup.add(pasteFilterNodeMenuItem);
		popup.add(addShowAllFilterNodeMenuItem);
		popup.add(deleteFilterNodesMenuItem);
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
		if (!frame.isVisible()) {
			show();
		}
		DefaultMutableTreeNode parentNodeInspect = new DefaultMutableTreeNode(
			"Inspect Results [" + sac.getSpimSource().getName() + "]");
		SourceAndConverterInspector.appendInspectorResult(parentNodeInspect, sac,
			sourceAndConverterService, false);
		top.add(parentNodeInspect);
	}

	public void removeBdvHandleNodes(BdvHandle bdvh) {
		visitAllNodesAndProcess(top, (node) -> {
			if (node instanceof BdvHandleFilterNode) {
				BdvHandleFilterNode bfn = (BdvHandleFilterNode) node;

				if (bfn.bdvh.equals(bdvh)) {
					bfn.removeFromParent();
					bfn.clear();
				}
			}
		});
	}

	/**
	 * TODO : understand is this method is really for update or only for
	 * creation...
	 * 
	 * @param sac source which UI needs to be updated
	 */
	public void update(SourceAndConverter sac) {
		if (!frame.isVisible()) {
			show();
		}
		synchronized (tree) {
			updateSpimDataFilterNodes();
			if (top.hasConsumed(sac)) {
				top.update(new SourceFilterNode.SourceUpdateEvent(sac));
			}
			else {
				top.add(new DefaultMutableTreeNode(new RenamableSourceAndConverter(
					sac)));
			}
		}
	}

	/**
	 * Method responsible for - removes {@link SpimDataFilterNode} if no sac
	 * belonging to a previous opened SpimData is not there anymore - adds a
	 * {@link SpimDataFilterNode} if sac belonging to a new SpimData is newly
	 * appended in the UI
	 */
	private void updateSpimDataFilterNodes() {
		synchronized (tree) {
			// Fetch All Spimdata from all Sources
			Set<AbstractSpimData<?>> currentSpimdatas = sourceAndConverterService
				.getSpimDatasets();

			Set<SpimDataFilterNode> obsoleteSpimDataFilterNodes = new HashSet<>();

			// Check for obsolete spimdatafilternodes
			spimdataFilterNodes.forEach(fnode -> {
				if (!currentSpimdatas.contains(fnode.asd)) {
					if (fnode.getParent() != null) {
						obsoleteSpimDataFilterNodes.add(fnode);
						fnode.removeFromParent();
					}
				}
			});

			// Important to avoid memory leak
			spimdataFilterNodes.removeAll(obsoleteSpimDataFilterNodes);

			// Check for new spimdata
			currentSpimdatas.forEach(asd -> {
				if ((spimdataFilterNodes.size() == 0) || (spimdataFilterNodes.stream()
					.noneMatch(fnode -> fnode.asd.equals(asd))))
				{
					SpimDataFilterNode newNode = new SpimDataFilterNode(model,
						"SpimData " + spimdataFilterNodes.size(), asd,
						sourceAndConverterService);
					SourceFilterNode allSources = new SourceFilterNode(model,
						"All Sources", (in) -> true, true);
					spimdataFilterNodes.add(newNode);
					newNode.add(allSources);
					top.add(newNode);
					addEntityFilterNodes(newNode,
						(AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>>) asd);
				}
			});
		}
	}

	/**
	 * Renames in the UI a SpimData node by another one
	 * 
	 * @param asd_renamed spimdata to rename
	 * @param name new name
	 */
	public void updateSpimDataName(AbstractSpimData asd_renamed, String name) {
		synchronized (tree) {
			visitAllNodesAndProcess(top, node -> {
				if (node instanceof SpimDataFilterNode) {
					if (((SpimDataFilterNode) node).asd.equals(asd_renamed)) {
						((SpimDataFilterNode) node).setName(name);
						SourceFilterNode.safeModelReloadAction(() -> model.nodeChanged(
							node));
					}
				}
			});
		}
	}

	/**
	 * Applies an operation ({@link Consumer} on all nodes of the tree
	 * 
	 * @param node root node of the tree to be processed
	 * @param processor how to process the node
	 */
	private static void visitAllNodesAndProcess(TreeNode node,
		Consumer<DefaultMutableTreeNode> processor)
	{
		if (node.getChildCount() >= 0) {
			for (Enumeration e = node.children(); e.hasMoreElements();) {
				TreeNode n = (TreeNode) e.nextElement();
				visitAllNodesAndProcess(n, processor);
				if (n instanceof DefaultMutableTreeNode) {
					processor.accept((DefaultMutableTreeNode) n);
				}
			}
		}
	}

	/**
	 * Adds all the filtering nodes which are sorting the source contained in a
	 * SpimData according to each {@link Entity} and before by class (
	 * {@link Entity#getClass()} )
	 * 
	 * @param nodeSpimData spimdata node where to append the filter nodes
	 * @param asd spimdata to filter
	 */
	private void addEntityFilterNodes(SpimDataFilterNode nodeSpimData,
		AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>> asd)
	{
		// Gets all entities by class

		Map<Class, List<Entity>> entitiesByClass = asd.getSequenceDescription()
			.getViewDescriptions()
			// Streams viewSetups
			.values().stream()
			// Filters if view is present
			.filter(BasicViewDescription::isPresent)
			// Gets Entities associated to ViewSetup
			.map(v -> v.getViewSetup().getAttributes().values())
			// Reduce into a single list and stream
			.reduce(new ArrayList<>(), (a, b) -> {
				a.addAll(b);
				return a;
			}).stream()
			// removes null entities
			.filter(Objects::nonNull)
			// Collected and sorted by class
			.collect(Collectors.groupingBy(Entity::getClass, Collectors.toList()));

		Map<Class, SourceFilterNode> classNodes = new HashMap<>();
		entitiesByClass.keySet().forEach((c) -> classNodes.put(c,
			new SourceFilterNode(model, c.getSimpleName(), (sac) -> true, false)));

		List<SourceFilterNode> orderedNodes = new ArrayList<>(classNodes.values());
		orderedNodes.sort(Comparator.comparing(SourceFilterNode::getName));
		orderedNodes.forEach(nodeSpimData::add);

		Set<Entity> entitiesAlreadyRegistered = new HashSet<>();
		entitiesByClass.forEach((c, el) -> el.forEach(entity -> {
			if (!entitiesAlreadyRegistered.contains(entity)) {
				// Attempt to use NamedEntity if applicable
				String entityName = null;
				if (entity instanceof NamedEntity) {
					entityName = ((NamedEntity) entity).getName();
				}
				if ((entityName == null) || (entityName.equals(""))) {
					entityName = c.getSimpleName() + " " + entity.getId();
				}
				entitiesAlreadyRegistered.add(entity);

				SpimDataElementFilter nodeElement = new SpimDataElementFilter(model,
					entityName, entity, sourceAndConverterService);

				SourceFilterNode showAllSources = new SourceFilterNode(model,
					"All Sources", (sac) -> true, true);

				classNodes.get(c).add(nodeElement);
				nodeElement.add(showAllSources);
			}
		}));

	}

	/**
	 * Remove a {@link SourceAndConverter} from the UI of a
	 * SourceAndConverterService
	 * 
	 * @param sac source to remove
	 */
	public void remove(SourceAndConverter sac) {
		if (!frame.isVisible()) {
			show();
		}
		synchronized (tree) {
			if (top.currentInputSacs.contains(sac)) {
				top.remove(sac);
				updateSpimDataFilterNodes();
			}
		}
	}

	/**
	 * @param tree the tree view of sourceandconverters
	 * @return an array containing the list of all {@link SourceAndConverter}
	 *         selected by the user: - all children of a selected node are
	 *         considered selected - the list does not contain duplicates - the
	 *         list is ordered according to
	 *         {@link SourceAndConverterHelper#sortDefault(SourceAndConverter[])}
	 */
	public SourceAndConverter[] getSelectedSourceAndConverters(JTree tree) {
		Set<SourceAndConverter<?>> sacList = new HashSet<>(); // A set avoids
																													// duplicate
																													// SourceAndConverter
		for (TreePath tp : tree.getSelectionModel().getSelectionPaths()) {
			if (((DefaultMutableTreeNode) tp.getLastPathComponent())
				.getUserObject() instanceof RenamableSourceAndConverter)
			{
				Object userObj =
					((RenamableSourceAndConverter) ((DefaultMutableTreeNode) tp
						.getLastPathComponent()).getUserObject()).sac;
				sacList.add((SourceAndConverter<?>) userObj);
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
	 * @return an array containing the list of all {@link SourceAndConverter}
	 *         below the @param node: - the list does not contain duplicates - the
	 *         list order can be considered random
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
		else if (node instanceof SourceFilterNode) {
			sacs.addAll(((SourceFilterNode) node).currentOutputSacs);
		}
		else {
			for (int i = 0; i < node.getChildCount(); i++) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(
					i);
				if (child instanceof SourceFilterNode) {
					sacs.addAll(((SourceFilterNode) child).currentOutputSacs);
				}
				else {
					if (child.getUserObject() instanceof RenamableSourceAndConverter) {
						Object userObj = ((RenamableSourceAndConverter) (child
							.getUserObject())).sac;
						sacs.add((SourceAndConverter) userObj);
					}
					else {
						sacs.addAll(getSourceAndConvertersFromChildrenOf(child));
					}
				}
			}
		}
		return sacs;
	}

	/**
	 * Method used by
	 * {@link sc.fiji.bdvpg.scijava.widget.SwingSourceAndConverterListWidget} and
	 * {@link sc.fiji.bdvpg.scijava.widget.SwingSourceAndConverterWidget} TAKE
	 * CARE TO MEMORY LEAKS IF YOU USE THIS! Check how memory leaks are avoided in
	 * the widgets linked above
	 * 
	 * @return the model of the tree
	 */
	public DefaultTreeModel getTreeModel() {
		return model;
	}

	/**
	 * Allows to get the tree path from a String, nodes names are separated by the
	 * "&gt;" character Like SpimData_0 &gt; Channel &gt; 0 will return the
	 * {@link TreePath} to the childrennode Used in
	 * {@link sc.fiji.bdvpg.scijava.converters.StringToSourceAndConverterArray}
	 * Not the ideal situation where the UI is used to retrieve SourceAndConverter
	 *
	 * @param path path as string
	 * @return treepath fetched from the path
	 */
	public TreePath getTreePathFromString(String path) {
		String[] stringPath = path.split(">");
		Object[] nodes = new Object[stringPath.length];
		TreeNode current = top;
		int currentDepth = 0;

		while (currentDepth < stringPath.length) {
			final Enumeration children = current.children();
			boolean found = false;
			while (children.hasMoreElements()) {
				TreeNode testNode = (TreeNode) children.nextElement();
				if (testNode.toString().trim().equals(stringPath[currentDepth]
					.trim()))
				{
					nodes[currentDepth] = testNode;
					currentDepth++;
					current = testNode;
					found = true;
					break;
				}
				else {
					logger.error("Unmatched " + testNode.toString().trim());
				}
			}
			if (!found) break;
		}

		if (currentDepth == stringPath.length) {
			return new TreePath(nodes);
		}
		else {
			// Better not show anything : a null return is used by the converter to
			// discard any string which cannot be converter to a SourceAndConverter
			// array
			// errlog.accept("TreePath "+path+" not found.");
			return null;
		}
	}

	/**
	 * Used by
	 * {@link sc.fiji.bdvpg.scijava.converters.StringToSourceAndConverterArray}
	 * Note the sorting of SourceAndConverter by
	 * {@link SourceAndConverterHelper#sortDefaultGeneric}
	 * 
	 * @param path path
	 * @return the list of sources in the path
	 */
	public List<SourceAndConverter<?>> getSourceAndConvertersFromTreePath(
		TreePath path)
	{
		return SourceAndConverterHelper.sortDefaultGeneric(
			getSourceAndConvertersFromChildrenOf((DefaultMutableTreeNode) path
				.getLastPathComponent()));
	}

	/**
	 * Used by
	 * {@link sc.fiji.bdvpg.scijava.converters.StringToSourceAndConverterArray}
	 * Note the sorting of SourceAndConverter by
	 * {@link SourceAndConverterHelper#sortDefaultGeneric}
	 * 
	 * @param path path
	 * @return the list of sources in the path
	 */
	public List<SourceAndConverter<?>> getSourceAndConvertersFromPath(
		String path)
	{
		TreePath tp = getTreePathFromString(path);
		if (tp != null) {
			return getSourceAndConvertersFromTreePath(tp);
		}
		else {
			return new ArrayList<>();
		}
	}

	public synchronized void addNode(DefaultMutableTreeNode node) {
		top.add(node);
	}

	public synchronized void removeNode(DefaultMutableTreeNode node) {
		getTreeModel().removeNodeFromParent(node);
	}

	public synchronized void addNode(DefaultMutableTreeNode parent,
		DefaultMutableTreeNode node)
	{
		parent.add(node);
	}

	public Node getRoot() {
		return new Node((TreeNode) model.getRoot());
	}

	public static class Node {

		// SourceFilterNode
		// RenamableSourceAndConverter

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
			if (node instanceof SourceFilterNode) {
				Set<SourceAndConverter<?>> sourcesSet =
					((SourceFilterNode) node).currentOutputSacs;
				return SourceAndConverterHelper.sortDefault(sourcesSet.toArray(
					new SourceAndConverter<?>[0]));
			}
			else if (node instanceof RenamableSourceAndConverter) {
				return new SourceAndConverter[] {
					((RenamableSourceAndConverter) node).sac };
			}
			else {
				return null;
			}
		}

		public Node parent() {
			if (node.getParent() == null) return null;
			return new Node(node.getParent());
		}
	}

}
