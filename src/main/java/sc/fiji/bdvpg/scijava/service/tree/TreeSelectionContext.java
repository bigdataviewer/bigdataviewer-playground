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

package sc.fiji.bdvpg.scijava.service.tree;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.scijava.service.RenamableSource;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of a (possibly multi-)selection in the source tree,
 * partitioned by the kind of object each selected node designates.
 *
 * <p>Selected nodes are classified as follows:</p>
 * <ul>
 *   <li>Source leaves ({@link RenamableSource} user objects) go into
 *   {@link #sources()}.</li>
 *   <li>{@link BdvHandleFilterNode}s go into {@link #bdvNodes()}. The sources
 *   they contain are deliberately NOT collected into {@link #sources()}:
 *   selecting a BDV window node expresses an intent on the window itself, not
 *   on the sources it displays.</li>
 *   <li>Any other {@link FilterNode} (SpimData dataset nodes, entity nodes
 *   such as "Channel 0", user-created filter nodes) contributes all the
 *   sources below it to {@link #sources()}. {@link SpimDataFilterNode}s are
 *   additionally listed in {@link #spimDataNodes()} so actions can name the
 *   datasets involved.</li>
 *   <li>Inspection result nodes (see {@link SourceTree#INSPECT_NODE_PREFIX})
 *   go into {@link #inspectNodes()}.</li>
 * </ul>
 *
 * <p>Since sources can appear several times in the tree, {@link #sources()}
 * is a set: selecting the same source in several places yields it once.</p>
 *
 * <p>This class has no dependency on a live tree: it works from selection
 * paths and a tree-node-to-filter-node resolver, which makes it testable
 * without a GUI.</p>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class TreeSelectionContext {

	private final TreePath[] paths;
	private final Set<SourceAndConverter<?>> sources = new LinkedHashSet<>();
	private final List<BdvHandleFilterNode> bdvNodes = new ArrayList<>();
	private final List<SpimDataFilterNode> spimDataNodes = new ArrayList<>();
	private final List<DefaultMutableTreeNode> inspectNodes = new ArrayList<>();

	/**
	 * Classifies the given selection.
	 *
	 * @param selectionPaths the selected tree paths, may be null or empty
	 * @param filterNodeResolver maps a Swing tree node to its {@link FilterNode},
	 *          or null if the tree node does not represent a filter node
	 *          (typically {@link SourceTreeView#getFilterNode})
	 */
	public TreeSelectionContext(TreePath[] selectionPaths,
		Function<DefaultMutableTreeNode, FilterNode> filterNodeResolver)
	{
		this.paths = selectionPaths == null ? new TreePath[0] : selectionPaths;
		for (TreePath tp : paths) {
			Object last = tp.getLastPathComponent();
			if (!(last instanceof DefaultMutableTreeNode)) continue;
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) last;
			Object userObject = treeNode.getUserObject();
			if (userObject instanceof RenamableSource) {
				sources.add(((RenamableSource) userObject).source);
				continue;
			}
			FilterNode filterNode = filterNodeResolver.apply(treeNode);
			if (filterNode instanceof BdvHandleFilterNode) {
				bdvNodes.add((BdvHandleFilterNode) filterNode);
			}
			else if (filterNode != null) {
				if (filterNode instanceof SpimDataFilterNode) {
					spimDataNodes.add((SpimDataFilterNode) filterNode);
				}
				sources.addAll(filterNode.outputSources());
			}
			else if (treeNode.toString().startsWith(
				SourceTree.INSPECT_NODE_PREFIX))
			{
				inspectNodes.add(treeNode);
			}
		}
	}

	/**
	 * @return the raw selection paths this context was built from
	 */
	public TreePath[] paths() {
		return paths;
	}

	/**
	 * @return the sources designated by the selection: directly selected source
	 *         leaves plus all sources below selected non-BDV filter nodes
	 */
	public Set<SourceAndConverter<?>> sources() {
		return Collections.unmodifiableSet(sources);
	}

	/**
	 * @return the selected BDV window nodes
	 */
	public List<BdvHandleFilterNode> bdvNodes() {
		return Collections.unmodifiableList(bdvNodes);
	}

	/**
	 * @return the {@link BdvHandle}s of the selected BDV window nodes
	 */
	public List<BdvHandle> bdvHandles() {
		return bdvNodes.stream().map(BdvHandleFilterNode::getBdvHandle).distinct()
			.collect(Collectors.toList());
	}

	/**
	 * @return the selected SpimData dataset nodes (their sources are also in
	 *         {@link #sources()})
	 */
	public List<SpimDataFilterNode> spimDataNodes() {
		return Collections.unmodifiableList(spimDataNodes);
	}

	/**
	 * @return the selected inspection result nodes
	 */
	public List<DefaultMutableTreeNode> inspectNodes() {
		return Collections.unmodifiableList(inspectNodes);
	}

	/**
	 * @return true if the selection designates nothing actionable
	 */
	public boolean isEmpty() {
		return sources.isEmpty() && bdvNodes.isEmpty() && inspectNodes.isEmpty();
	}
}
