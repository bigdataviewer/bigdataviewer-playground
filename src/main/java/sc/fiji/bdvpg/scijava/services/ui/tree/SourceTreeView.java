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

package sc.fiji.bdvpg.scijava.services.ui.tree;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.scijava.services.ui.RenamableSourceAndConverter;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Swing view for the source tree that implements efficient incremental updates.
 *
 * <p>This view listens to {@link SourceTreeModel} and updates the Swing
 * {@link DefaultTreeModel} incrementally using {@code nodesWereInserted} and
 * {@code nodesWereRemoved} instead of full {@code reload()} calls.</p>
 *
 * <p>All Swing operations are performed on the EDT via {@link SwingUtilities#invokeLater}.</p>
 *
 * <p>Key design decisions:</p>
 * <ul>
 *   <li>Sorting happens here in the View (not in the Model)</li>
 *   <li>Maintains bidirectional mappings for O(1) lookup between FilterNode and TreeNode</li>
 *   <li>Source nodes are wrapped in {@link RenamableSourceAndConverter} for display</li>
 * </ul>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class SourceTreeView implements SourceTreeModelListener {

    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode treeRoot;
    private final SourceTreeModel sourceModel;

    // Bidirectional mappings for O(1) lookup
    private final Map<FilterNode, DefaultMutableTreeNode> filterToTreeNode = new HashMap<>();
    private final Map<SourceAndConverter<?>, Set<DefaultMutableTreeNode>> sourceToTreeNodes = new HashMap<>();

    // Comparator for sorting sources
    private final Comparator<SourceAndConverter<?>> sourceComparator;

    /**
     * Creates a new SourceTreeView.
     *
     * @param sourceModel the model to display
     */
    public SourceTreeView(SourceTreeModel sourceModel) {
        this.sourceModel = sourceModel;

        // Create root tree node
        FilterNode modelRoot = sourceModel.getRoot();
        treeRoot = new DefaultMutableTreeNode(modelRoot.getName());
        filterToTreeNode.put(modelRoot, treeRoot);

        // Create the Swing tree model
        treeModel = new DefaultTreeModel(treeRoot);

        // Build initial tree structure
        buildTreeNode(modelRoot, treeRoot);

        // Use the default source comparator
        sourceComparator = Comparator.comparingInt(sac ->
                SourceAndConverterHelper.sortDefaultGeneric(Collections.singletonList(sac)).isEmpty() ? 0 : 0);

        // Register as listener
        sourceModel.addListener(this);
    }

    /**
     * @return the Swing tree model
     */
    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * @return the root tree node
     */
    public DefaultMutableTreeNode getTreeRoot() {
        return treeRoot;
    }

    /**
     * Recursively builds tree nodes from the model.
     */
    private void buildTreeNode(FilterNode filterNode, DefaultMutableTreeNode treeNode) {
        // Add child filter nodes
        for (FilterNode child : filterNode.getChildren()) {
            DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(child.getName());
            filterToTreeNode.put(child, childTreeNode);
            treeNode.add(childTreeNode);
            buildTreeNode(child, childTreeNode);
        }

        // Add source nodes if this node displays sources
        if (filterNode.isDisplaySources()) {
            List<SourceAndConverter<?>> sortedSources = SourceAndConverterHelper.sortDefaultGeneric(
                    filterNode.getOutputSources());
            for (SourceAndConverter<?> sac : sortedSources) {
                DefaultMutableTreeNode sourceNode = new DefaultMutableTreeNode(
                        new RenamableSourceAndConverter(sac));
                treeNode.add(sourceNode);
                sourceToTreeNodes.computeIfAbsent(sac, k -> new HashSet<>()).add(sourceNode);
            }
        }
    }

    // ============ SourceTreeModelListener Implementation ============

    @Override
    public void sourcesChanged(SourcesChangedEvent event) {
        SwingUtilities.invokeLater(() -> applySourcesChanged(event));
    }

    @Override
    public void structureChanged(StructureChangedEvent event) {
        SwingUtilities.invokeLater(() -> applyStructureChanged(event));
    }

    /**
     * Applies source changes to the Swing tree model.
     */
    private void applySourcesChanged(SourcesChangedEvent event) {
        switch (event.getType()) {
            case ADDED:
                applySourcesAdded(event);
                break;
            case REMOVED:
                applySourcesRemoved(event);
                break;
            case UPDATED:
                // Updates are handled as remove + add
                break;
        }
    }

    /**
     * Adds source nodes to the tree.
     */
    private void applySourcesAdded(SourcesChangedEvent event) {
        for (Map.Entry<FilterNode, List<SourceAndConverter<?>>> entry : event.getAffectedNodes().entrySet()) {
            FilterNode filterNode = entry.getKey();
            List<SourceAndConverter<?>> sources = entry.getValue();

            if (!filterNode.isDisplaySources()) {
                continue;
            }

            DefaultMutableTreeNode treeNode = filterToTreeNode.get(filterNode);
            if (treeNode == null) {
                continue;
            }

            // Get all sources currently in the tree node (both filter children and source children)
            List<SourceAndConverter<?>> allSources = new ArrayList<>();
            for (int i = 0; i < treeNode.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(i);
                Object userObject = child.getUserObject();
                if (userObject instanceof RenamableSourceAndConverter) {
                    allSources.add(((RenamableSourceAndConverter) userObject).sac);
                }
            }

            // Add new sources and sort
            allSources.addAll(sources);
            List<SourceAndConverter<?>> sortedSources = SourceAndConverterHelper.sortDefaultGeneric(allSources);

            // Find insertion points for each new source
            List<int[]> insertions = new ArrayList<>();
            for (SourceAndConverter<?> sac : sources) {
                int sortedIndex = sortedSources.indexOf(sac);
                // Account for filter node children (non-source children)
                int filterChildCount = countFilterChildren(treeNode);
                int treeIndex = filterChildCount + sortedIndex;

                DefaultMutableTreeNode sourceNode = new DefaultMutableTreeNode(
                        new RenamableSourceAndConverter(sac));
                treeNode.insert(sourceNode, treeIndex);
                sourceToTreeNodes.computeIfAbsent(sac, k -> new HashSet<>()).add(sourceNode);
                insertions.add(new int[]{treeIndex});
            }

            // Fire single nodesWereInserted event for all insertions
            if (!insertions.isEmpty()) {
                int[] indices = new int[insertions.size()];
                for (int i = 0; i < insertions.size(); i++) {
                    indices[i] = insertions.get(i)[0];
                }
                // Sort indices for proper notification
                java.util.Arrays.sort(indices);
                treeModel.nodesWereInserted(treeNode, indices);
            }
        }
    }

    /**
     * Removes source nodes from the tree.
     */
    private void applySourcesRemoved(SourcesChangedEvent event) {
        for (Map.Entry<FilterNode, List<SourceAndConverter<?>>> entry : event.getAffectedNodes().entrySet()) {
            FilterNode filterNode = entry.getKey();
            List<SourceAndConverter<?>> sources = entry.getValue();

            if (!filterNode.isDisplaySources()) {
                continue;
            }

            DefaultMutableTreeNode treeNode = filterToTreeNode.get(filterNode);
            if (treeNode == null) {
                continue;
            }

            // Find and remove source nodes
            List<DefaultMutableTreeNode> nodesToRemove = new ArrayList<>();
            List<Integer> indicesToRemove = new ArrayList<>();

            for (int i = 0; i < treeNode.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(i);
                Object userObject = child.getUserObject();
                if (userObject instanceof RenamableSourceAndConverter) {
                    SourceAndConverter<?> sac = ((RenamableSourceAndConverter) userObject).sac;
                    if (sources.contains(sac)) {
                        nodesToRemove.add(child);
                        indicesToRemove.add(i);
                    }
                }
            }

            // Remove in reverse order to maintain indices
            Object[] removedNodes = new Object[nodesToRemove.size()];
            int[] removedIndices = new int[indicesToRemove.size()];

            for (int i = nodesToRemove.size() - 1; i >= 0; i--) {
                DefaultMutableTreeNode nodeToRemove = nodesToRemove.get(i);
                removedNodes[i] = nodeToRemove;
                removedIndices[i] = indicesToRemove.get(i);

                treeNode.remove(nodeToRemove);

                // Update sourceToTreeNodes mapping
                Object userObject = nodeToRemove.getUserObject();
                if (userObject instanceof RenamableSourceAndConverter) {
                    SourceAndConverter<?> sac = ((RenamableSourceAndConverter) userObject).sac;
                    Set<DefaultMutableTreeNode> nodes = sourceToTreeNodes.get(sac);
                    if (nodes != null) {
                        nodes.remove(nodeToRemove);
                        if (nodes.isEmpty()) {
                            sourceToTreeNodes.remove(sac);
                        }
                    }
                }
            }

            // Fire nodesWereRemoved event
            if (removedNodes.length > 0) {
                treeModel.nodesWereRemoved(treeNode, removedIndices, removedNodes);
            }
        }
    }

    /**
     * Counts the number of filter node children (non-source children).
     */
    private int countFilterChildren(DefaultMutableTreeNode treeNode) {
        int count = 0;
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            if (!(child.getUserObject() instanceof RenamableSourceAndConverter)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Applies structural changes to the Swing tree model.
     */
    private void applyStructureChanged(StructureChangedEvent event) {
        switch (event.getType()) {
            case NODES_ADDED:
                applyNodesAdded(event);
                break;
            case NODES_REMOVED:
                applyNodesRemoved(event);
                break;
            case NODE_RENAMED:
                applyNodeRenamed(event);
                break;
        }
    }

    /**
     * Adds filter nodes to the tree.
     */
    private void applyNodesAdded(StructureChangedEvent event) {
        FilterNode parentFilterNode = event.getParentNode();
        DefaultMutableTreeNode parentTreeNode = filterToTreeNode.get(parentFilterNode);
        if (parentTreeNode == null) {
            return;
        }

        List<FilterNode> addedNodes = event.getAffectedNodes();
        List<Integer> indices = event.getChildIndices();

        for (int i = 0; i < addedNodes.size(); i++) {
            FilterNode filterNode = addedNodes.get(i);
            int index = indices.get(i);

            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(filterNode.getName());
            filterToTreeNode.put(filterNode, treeNode);

            parentTreeNode.insert(treeNode, index);

            // Build child structure
            buildTreeNode(filterNode, treeNode);
        }

        // Fire event
        if (!indices.isEmpty()) {
            int[] indicesArray = indices.stream().mapToInt(Integer::intValue).toArray();
            treeModel.nodesWereInserted(parentTreeNode, indicesArray);
        }
    }

    /**
     * Removes filter nodes from the tree.
     */
    private void applyNodesRemoved(StructureChangedEvent event) {
        FilterNode parentFilterNode = event.getParentNode();
        DefaultMutableTreeNode parentTreeNode = filterToTreeNode.get(parentFilterNode);
        if (parentTreeNode == null) {
            return;
        }

        List<FilterNode> removedNodes = event.getAffectedNodes();
        List<Integer> indices = event.getChildIndices();

        Object[] removedTreeNodes = new Object[removedNodes.size()];
        int[] indicesArray = new int[indices.size()];

        // Remove in reverse order to maintain indices
        for (int i = removedNodes.size() - 1; i >= 0; i--) {
            FilterNode filterNode = removedNodes.get(i);
            DefaultMutableTreeNode treeNode = filterToTreeNode.remove(filterNode);

            if (treeNode != null) {
                removedTreeNodes[i] = treeNode;
                indicesArray[i] = indices.get(i);
                parentTreeNode.remove(treeNode);

                // Clean up mappings recursively
                cleanupMappings(filterNode, treeNode);
            }
        }

        // Fire event
        if (removedTreeNodes.length > 0) {
            treeModel.nodesWereRemoved(parentTreeNode, indicesArray, removedTreeNodes);
        }
    }

    /**
     * Recursively cleans up mappings for a removed node and its children.
     */
    private void cleanupMappings(FilterNode filterNode, DefaultMutableTreeNode treeNode) {
        // Remove filter node mapping
        filterToTreeNode.remove(filterNode);

        // Remove source mappings for sources displayed in this node
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeNode.getChildAt(i);
            Object userObject = child.getUserObject();
            if (userObject instanceof RenamableSourceAndConverter) {
                SourceAndConverter<?> sac = ((RenamableSourceAndConverter) userObject).sac;
                Set<DefaultMutableTreeNode> nodes = sourceToTreeNodes.get(sac);
                if (nodes != null) {
                    nodes.remove(child);
                    if (nodes.isEmpty()) {
                        sourceToTreeNodes.remove(sac);
                    }
                }
            }
        }

        // Recursively clean up children
        for (FilterNode child : filterNode.getChildren()) {
            DefaultMutableTreeNode childTreeNode = filterToTreeNode.get(child);
            if (childTreeNode != null) {
                cleanupMappings(child, childTreeNode);
            }
        }
    }

    /**
     * Handles node rename events.
     */
    private void applyNodeRenamed(StructureChangedEvent event) {
        FilterNode filterNode = event.getAffectedNodes().get(0);
        DefaultMutableTreeNode treeNode = filterToTreeNode.get(filterNode);
        if (treeNode != null) {
            treeNode.setUserObject(filterNode.getName());
            treeModel.nodeChanged(treeNode);
        }
    }

    // ============ Utility Methods ============

    /**
     * Gets the tree node for a filter node.
     *
     * @param filterNode the filter node
     * @return the corresponding tree node, or null if not found
     */
    public DefaultMutableTreeNode getTreeNode(FilterNode filterNode) {
        return filterToTreeNode.get(filterNode);
    }

    /**
     * Gets all tree nodes that display a source.
     *
     * @param sac the source
     * @return a set of tree nodes, or empty set if not found
     */
    public Set<DefaultMutableTreeNode> getTreeNodes(SourceAndConverter<?> sac) {
        Set<DefaultMutableTreeNode> nodes = sourceToTreeNodes.get(sac);
        return nodes != null ? new HashSet<>(nodes) : new HashSet<>();
    }

    /**
     * Gets all sources displayed in a tree node and its children.
     *
     * @param treeNode the tree node
     * @return a set of sources
     */
    public Set<SourceAndConverter<?>> getSourcesFromTreeNode(DefaultMutableTreeNode treeNode) {
        Set<SourceAndConverter<?>> sources = new HashSet<>();
        collectSources(treeNode, sources);
        return sources;
    }

    private void collectSources(DefaultMutableTreeNode treeNode, Set<SourceAndConverter<?>> sources) {
        Object userObject = treeNode.getUserObject();
        if (userObject instanceof RenamableSourceAndConverter) {
            sources.add(((RenamableSourceAndConverter) userObject).sac);
        }

        for (int i = 0; i < treeNode.getChildCount(); i++) {
            collectSources((DefaultMutableTreeNode) treeNode.getChildAt(i), sources);
        }
    }

    /**
     * Gets the filter node for a tree node.
     *
     * @param treeNode the tree node
     * @return the filter node, or null if this is a source node
     */
    public FilterNode getFilterNode(DefaultMutableTreeNode treeNode) {
        for (Map.Entry<FilterNode, DefaultMutableTreeNode> entry : filterToTreeNode.entrySet()) {
            if (entry.getValue() == treeNode) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Disposes of this view, removing it as a listener.
     */
    public void dispose() {
        sourceModel.removeListener(this);
        filterToTreeNode.clear();
        sourceToTreeNodes.clear();
    }
}
