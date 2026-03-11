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
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.scijava.service.SourceService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe model for the source tree.
 *
 * <p>This class manages the pure data model for the source tree and provides
 * batch operations for efficient updates. All modifications are protected by
 * a {@link ReentrantReadWriteLock} allowing concurrent reads and exclusive writes.</p>
 *
 * <p>The model maintains indexes for O(1) lookup:</p>
 * <ul>
 *   <li>{@code sourceIndex}: Maps sources to the filter nodes that contain them</li>
 *   <li>{@code spimDataIndex}: Maps SpimData to their filter nodes</li>
 *   <li>{@code bdvHandleIndex}: Maps BdvHandles to their filter nodes</li>
 * </ul>
 *
 * <p>Events are fired to registered listeners after each modification, allowing
 * the view to update incrementally.</p>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class SourceTreeModel {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final FilterNode root;
    private final FilterNode otherSourcesNode;
    private final SourceService sourceAndConverterService;
    private final SpimDataFilterFactory spimDataFilterFactory;

    // Indexes for O(1) lookup
    private final Map<SourceAndConverter<?>, Set<FilterNode>> sourceIndex = new HashMap<>();
    private final Map<AbstractSpimData<?>, SpimDataFilterNode> spimDataIndex = new HashMap<>();
    private final Map<BdvHandle, BdvHandleFilterNode> bdvHandleIndex = new HashMap<>();

    // Listeners
    private final List<SourceTreeModelListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates a new SourceTreeModel.
     *
     * @param sourceAndConverterService the service for metadata queries
     */
    public SourceTreeModel(SourceService sourceAndConverterService) {
        this.sourceAndConverterService = sourceAndConverterService;
        this.spimDataFilterFactory = new SpimDataFilterFactory(sourceAndConverterService);

        // Create root node
        this.root = new FilterNode("Sources", source -> true, false);

        // Create "Other Sources" node for sources not in any SpimData
        this.otherSourcesNode = new FilterNode("Other Sources",
                source -> !sourceAndConverterService.containsMetadata(source,
                        SourceService.SPIM_DATA_INFO), true);
        root.addChild(otherSourcesNode);
    }

    /**
     * @return the root filter node
     */
    public FilterNode getRoot() {
        lock.readLock().lock();
        try {
            return root;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @return the "Other Sources" node for sources not in any SpimData
     */
    public FilterNode getOtherSourcesNode() {
        lock.readLock().lock();
        try {
            return otherSourcesNode;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ============ Listener Management ============

    /**
     * Adds a listener for model changes.
     * @param listener the listener to add
     */
    public void addListener(SourceTreeModelListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener.
     * @param listener the listener to remove
     */
    public void removeListener(SourceTreeModelListener listener) {
        listeners.remove(listener);
    }

    private void fireSourcesChanged(SourcesChangedEvent event) {
        if (!event.isEmpty()) {
            for (SourceTreeModelListener listener : listeners) {
                listener.sourcesChanged(event);
            }
        }
    }

    private void fireStructureChanged(StructureChangedEvent event) {
        for (SourceTreeModelListener listener : listeners) {
            listener.structureChanged(event);
        }
    }

    // ============ Source Operations ============

    /**
     * Adds a single source to the model.
     * @param source the source to add
     */
    public void addSource(SourceAndConverter<?> source) {
        addSources(Collections.singletonList(source));
    }

    /**
     * Adds multiple sources to the model in a single batch operation.
     * This fires a single event for all sources, enabling efficient UI updates.
     *
     * @param sources the sources to add
     */
    public void addSources(Collection<SourceAndConverter<?>> sources) {
        if (sources.isEmpty()) return;

        lock.writeLock().lock();
        try {
            Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes = new HashMap<>();

            for (SourceAndConverter<?> source : sources) {
                addSourceToNode(source, root, affectedNodes);
            }

            // Fire single event for all sources
            SourcesChangedEvent event = new SourcesChangedEvent(
                    SourcesChangedEvent.Type.ADDED, sources, affectedNodes);
            fireSourcesChanged(event);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Recursively adds a source to a node and its children, tracking affected nodes.
     */
    private void addSourceToNode(SourceAndConverter<?> source, FilterNode node,
                                  Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes) {
        boolean passed = node.addSource(source);
        if (passed) {
            // Track that this node displays the source
            if (node.isDisplaySources()) {
                affectedNodes.computeIfAbsent(node, k -> new ArrayList<>()).add(source);
            }

            // Update source index
            sourceIndex.computeIfAbsent(source, k -> new HashSet<>()).add(node);

            // Recursively add to children
            for (FilterNode child : node.children()) {
                addSourceToNode(source, child, affectedNodes);
            }
        }
    }

    /**
     * Removes a single source from the model.
     * @param source the source to remove
     */
    public void removeSource(SourceAndConverter<?> source) {
        removeSources(Collections.singletonList(source));
    }

    /**
     * Removes multiple sources from the model in a single batch operation.
     *
     * @param sources the sources to remove
     */
    public void removeSources(Collection<SourceAndConverter<?>> sources) {
        if (sources.isEmpty()) return;

        lock.writeLock().lock();
        try {
            Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes = new HashMap<>();

            for (SourceAndConverter<?> source : sources) {
                removeSourceFromNode(source, root, affectedNodes);

                // Remove from source index
                sourceIndex.remove(source);
            }

            // Fire source removal event first, so the view can update
            // source nodes before the structural change removes parent nodes
            SourcesChangedEvent event = new SourcesChangedEvent(
                    SourcesChangedEvent.Type.REMOVED, sources, affectedNodes);
            fireSourcesChanged(event);

            // Auto-remove SpimData nodes that have become empty
            removeEmptySpimDataNodes();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks all SpimData filter nodes and removes any that have no remaining output sources.
     * This is called after source removal to clean up empty SpimData nodes.
     */
    private void removeEmptySpimDataNodes() {
        List<AbstractSpimData<?>> toRemove = new ArrayList<>();
        for (Map.Entry<AbstractSpimData<?>, SpimDataFilterNode> entry : spimDataIndex.entrySet()) {
            if (!entry.getValue().hasOutputSources()) {
                toRemove.add(entry.getKey());
            }
        }
        for (AbstractSpimData<?> spimData : toRemove) {
            SpimDataFilterNode spimDataNode = spimDataIndex.remove(spimData);
            int removeIndex = root.children().indexOf(spimDataNode);
            root.removeChild(spimDataNode);

            // Clean up source index (should be empty, but be safe)
            for (SourceAndConverter<?> source : spimDataNode.outputSources()) {
                Set<FilterNode> nodes = sourceIndex.get(source);
                if (nodes != null) {
                    removeNodesRecursively(spimDataNode, nodes);
                }
            }

            StructureChangedEvent structEvent = new StructureChangedEvent(
                    StructureChangedEvent.Type.NODES_REMOVED,
                    Collections.singletonList(spimDataNode),
                    root,
                    Collections.singletonList(removeIndex));
            fireStructureChanged(structEvent);
        }
    }

    /**
     * Recursively removes a source from a node and its children.
     */
    private void removeSourceFromNode(SourceAndConverter<?> source, FilterNode node,
                                       Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes) {
        boolean wasVisible = node.removeSource(source);
        if (wasVisible && node.isDisplaySources()) {
            affectedNodes.computeIfAbsent(node, k -> new ArrayList<>()).add(source);
        }

        // Recursively remove from children
        for (FilterNode child : node.children()) {
            removeSourceFromNode(source, child, affectedNodes);
        }
    }

    /**
     * Updates sources (re-evaluates filters) in a single batch operation.
     *
     * @param sources the sources to update
     */
    public void updateSources(Collection<SourceAndConverter<?>> sources) {
        if (sources.isEmpty()) return;

        lock.writeLock().lock();
        try {
            Map<FilterNode, List<SourceAndConverter<?>>> addedNodes = new HashMap<>();
            Map<FilterNode, List<SourceAndConverter<?>>> removedNodes = new HashMap<>();

            for (SourceAndConverter<?> source : sources) {
                updateSourceInNode(source, root, addedNodes, removedNodes);
            }

            // Fire events for added and removed
            if (!removedNodes.isEmpty()) {
                SourcesChangedEvent removeEvent = new SourcesChangedEvent(
                        SourcesChangedEvent.Type.REMOVED, sources, removedNodes);
                fireSourcesChanged(removeEvent);
            }
            if (!addedNodes.isEmpty()) {
                SourcesChangedEvent addEvent = new SourcesChangedEvent(
                        SourcesChangedEvent.Type.ADDED, sources, addedNodes);
                fireSourcesChanged(addEvent);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Re-evaluates a source in a node and its children.
     */
    private void updateSourceInNode(SourceAndConverter<?> source, FilterNode node,
                                     Map<FilterNode, List<SourceAndConverter<?>>> addedNodes,
                                     Map<FilterNode, List<SourceAndConverter<?>>> removedNodes) {
        int result = node.reevaluateSource(source);
        if (result != 0 && node.isDisplaySources()) {
            if (result > 0) {
                addedNodes.computeIfAbsent(node, k -> new ArrayList<>()).add(source);
            } else {
                removedNodes.computeIfAbsent(node, k -> new ArrayList<>()).add(source);
            }
        }

        // Update source index
        if (result > 0) {
            sourceIndex.computeIfAbsent(source, k -> new HashSet<>()).add(node);
        } else if (result < 0) {
            Set<FilterNode> nodes = sourceIndex.get(source);
            if (nodes != null) {
                nodes.remove(node);
            }
        }

        // Recursively update children
        for (FilterNode child : node.children()) {
            updateSourceInNode(source, child, addedNodes, removedNodes);
        }
    }

    // ============ SpimData Operations ============

    /**
     * Adds a SpimData to the model, creating its filter hierarchy.
     *
     * @param spimData the SpimData to add
     * @param name the display name for the SpimData node
     */
    public void addSpimData(AbstractSpimData<?> spimData, String name) {
        lock.writeLock().lock();
        try {
            if (spimDataIndex.containsKey(spimData)) {
                return; // Already added
            }

            // Create the filter hierarchy
            SpimDataFilterNode spimDataNode = spimDataFilterFactory.createHierarchy(spimData, name);
            spimDataIndex.put(spimData, spimDataNode);

            // Populate the new node with any sources already registered for this SpimData
            // This must happen BEFORE firing NODES_ADDED, because the view's buildTreeNode
            // (called via invokeLater) will see the populated state and build source nodes.
            // Firing a separate SOURCES_ADDED after NODES_ADDED would cause duplicates.
            List<SourceAndConverter<?>> existingSources = sourceAndConverterService
                    .getSourcesFromDataset(spimData);
            if (!existingSources.isEmpty()) {
                Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes = new HashMap<>();
                for (SourceAndConverter<?> source : existingSources) {
                    addSourceToNode(source, spimDataNode, affectedNodes);
                }
            }

            // Add to root and fire structure changed event
            int insertIndex = root.childCount();
            root.addChild(spimDataNode);

            StructureChangedEvent event = new StructureChangedEvent(
                    StructureChangedEvent.Type.NODES_ADDED,
                    Collections.singletonList(spimDataNode),
                    root,
                    Collections.singletonList(insertIndex));
            fireStructureChanged(event);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a SpimData and its filter hierarchy from the model.
     *
     * @param spimData the SpimData to remove
     */
    public void removeSpimData(AbstractSpimData<?> spimData) {
        lock.writeLock().lock();
        try {
            SpimDataFilterNode spimDataNode = spimDataIndex.remove(spimData);
            if (spimDataNode == null) {
                return; // Not found
            }

            // Find index before removing
            int removeIndex = root.children().indexOf(spimDataNode);

            // Remove from root
            root.removeChild(spimDataNode);

            // Clean up source index
            for (SourceAndConverter<?> source : spimDataNode.outputSources()) {
                Set<FilterNode> nodes = sourceIndex.get(source);
                if (nodes != null) {
                    removeNodesRecursively(spimDataNode, nodes);
                }
            }

            // Fire structure changed event
            StructureChangedEvent event = new StructureChangedEvent(
                    StructureChangedEvent.Type.NODES_REMOVED,
                    Collections.singletonList(spimDataNode),
                    root,
                    Collections.singletonList(removeIndex));
            fireStructureChanged(event);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Recursively removes a node and its children from a set.
     */
    private void removeNodesRecursively(FilterNode node, Set<FilterNode> nodes) {
        nodes.remove(node);
        for (FilterNode child : node.children()) {
            removeNodesRecursively(child, nodes);
        }
    }

    /**
     * Clears all sources from a node and its descendants, and removes them
     * from the source index. Used before repopulating a node from a new parent
     * (e.g. during drag-and-drop reparenting).
     *
     * @param node the node to clear
     */
    private void clearNodeSources(FilterNode node) {
        // Clean source index for this node and all descendants
        for (SourceAndConverter<?> source : node.outputSources()) {
            Set<FilterNode> nodes = sourceIndex.get(source);
            if (nodes != null) {
                removeNodesRecursively(node, nodes);
            }
        }
        // Clear the node's own sources
        node.clearSources();
        // Recursively clear children
        for (FilterNode child : node.children()) {
            clearNodeSources(child);
        }
    }

    /**
     * Gets the SpimDataFilterNode for a SpimData.
     *
     * @param spimData the SpimData to look up
     * @return the filter node, or null if not found
     */
    public SpimDataFilterNode getSpimDataNode(AbstractSpimData<?> spimData) {
        lock.readLock().lock();
        try {
            return spimDataIndex.get(spimData);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Updates the name of a SpimData node.
     *
     * @param spimData the SpimData whose node to rename
     * @param name the new name
     */
    public void renameSpimData(AbstractSpimData<?> spimData, String name) {
        lock.writeLock().lock();
        try {
            SpimDataFilterNode node = spimDataIndex.get(spimData);
            if (node != null) {
                node.setName(name);
                fireStructureChanged(StructureChangedEvent.nodeRenamed(node));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ============ BdvHandle Operations ============

    /**
     * Adds a BdvHandle filter node to the model.
     *
     * @param bdvHandle the BdvHandle to add
     * @param name the display name
     * @param parentNode the parent to add to (usually root)
     */
    public void addBdvHandle(BdvHandle bdvHandle, String name, FilterNode parentNode) {
        lock.writeLock().lock();
        try {
            if (bdvHandleIndex.containsKey(bdvHandle)) {
                return;
            }

            BdvHandleFilterNode bdvNode = new BdvHandleFilterNode(name, bdvHandle);
            bdvHandleIndex.put(bdvHandle, bdvNode);

            // Set up callback for filter updates when sources are added/removed from BDV
            bdvNode.setFilterUpdateCallback(() -> {
                refreshBdvHandleNode(bdvHandle);
            });

            // Add "All Sources" child
            FilterNode allSourcesNode = new FilterNode("All Sources", source -> true, true);
            bdvNode.addChild(allSourcesNode);

            // Populate with sources already in the parent BEFORE firing NODES_ADDED
            // This prevents the view from seeing an empty node and then receiving
            // a separate SOURCES_ADDED event that would double the source tree nodes
            for (SourceAndConverter<?> source : parentNode.outputSources()) {
                Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes = new HashMap<>();
                addSourceToNode(source, bdvNode, affectedNodes);
            }

            int insertIndex = parentNode.childCount();
            parentNode.addChild(bdvNode);

            StructureChangedEvent event = new StructureChangedEvent(
                    StructureChangedEvent.Type.NODES_ADDED,
                    Collections.singletonList(bdvNode),
                    parentNode,
                    Collections.singletonList(insertIndex));
            fireStructureChanged(event);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a BdvHandle filter node from the model.
     *
     * @param bdvHandle the BdvHandle to remove
     */
    public void removeBdvHandle(BdvHandle bdvHandle) {
        lock.writeLock().lock();
        try {
            BdvHandleFilterNode bdvNode = bdvHandleIndex.remove(bdvHandle);
            if (bdvNode == null) {
                return;
            }

            bdvNode.cleanup();

            FilterNode parent = bdvNode.parent();
            if (parent != null) {
                int removeIndex = parent.children().indexOf(bdvNode);
                parent.removeChild(bdvNode);

                StructureChangedEvent event = new StructureChangedEvent(
                        StructureChangedEvent.Type.NODES_REMOVED,
                        Collections.singletonList(bdvNode),
                        parent,
                        Collections.singletonList(removeIndex));
                fireStructureChanged(event);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes all BdvHandle nodes for a specific BdvHandle from anywhere in the tree.
     *
     * @param bdvHandle the BdvHandle to remove
     */
    public void removeBdvHandleNodes(BdvHandle bdvHandle) {
        lock.writeLock().lock();
        try {
            List<BdvHandleFilterNode> toRemove = new ArrayList<>();
            collectBdvHandleNodes(root, bdvHandle, toRemove);

            for (BdvHandleFilterNode node : toRemove) {
                node.cleanup();
                FilterNode parent = node.parent();
                if (parent != null) {
                    int removeIndex = parent.children().indexOf(node);
                    parent.removeChild(node);

                    StructureChangedEvent event = new StructureChangedEvent(
                            StructureChangedEvent.Type.NODES_REMOVED,
                            Collections.singletonList(node),
                            parent,
                            Collections.singletonList(removeIndex));
                    fireStructureChanged(event);
                }
            }

            bdvHandleIndex.remove(bdvHandle);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void collectBdvHandleNodes(FilterNode node, BdvHandle bdvHandle, List<BdvHandleFilterNode> result) {
        if (node instanceof BdvHandleFilterNode) {
            BdvHandleFilterNode bfn = (BdvHandleFilterNode) node;
            if (bfn.getBdvHandle().equals(bdvHandle)) {
                result.add(bfn);
            }
        }
        for (FilterNode child : node.children()) {
            collectBdvHandleNodes(child, bdvHandle, result);
        }
    }

    /**
     * Refreshes a BdvHandle node by comparing the BDV viewer's current sources
     * against the node's output, then adding/removing the difference.
     *
     * <p>Called by the BdvHandleFilterNode's state listener when sources
     * are added/removed from the BDV viewer.</p>
     *
     * @param bdvHandle the BdvHandle whose node to refresh
     */
    private void refreshBdvHandleNode(BdvHandle bdvHandle) {
        lock.writeLock().lock();
        try {
            BdvHandleFilterNode bdvNode = bdvHandleIndex.get(bdvHandle);
            if (bdvNode == null) return;

            // Sources currently in the BDV viewer
            Set<SourceAndConverter<?>> bdvSources = new HashSet<>(
                    bdvHandle.getViewerPanel().state().getSources());

            // Sources currently accepted by this node
            Set<SourceAndConverter<?>> currentOutput = bdvNode.outputSources();

            // Add sources that are in BDV but not yet in this node's output
            List<SourceAndConverter<?>> toAdd = new ArrayList<>();
            for (SourceAndConverter<?> source : bdvSources) {
                if (!currentOutput.contains(source)) {
                    toAdd.add(source);
                }
            }
            if (!toAdd.isEmpty()) {
                Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes = new HashMap<>();
                for (SourceAndConverter<?> source : toAdd) {
                    addSourceToNode(source, bdvNode, affectedNodes);
                }
                if (!affectedNodes.isEmpty()) {
                    SourcesChangedEvent event = new SourcesChangedEvent(
                            SourcesChangedEvent.Type.ADDED, toAdd, affectedNodes);
                    fireSourcesChanged(event);
                }
            }

            // Remove sources that are in this node's output but no longer in BDV
            List<SourceAndConverter<?>> toRemove = new ArrayList<>();
            for (SourceAndConverter<?> source : currentOutput) {
                if (!bdvSources.contains(source)) {
                    toRemove.add(source);
                }
            }
            if (!toRemove.isEmpty()) {
                Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes = new HashMap<>();
                for (SourceAndConverter<?> source : toRemove) {
                    removeSourceFromNode(source, bdvNode, affectedNodes);
                }
                if (!affectedNodes.isEmpty()) {
                    SourcesChangedEvent event = new SourcesChangedEvent(
                            SourcesChangedEvent.Type.REMOVED, toRemove, affectedNodes);
                    fireSourcesChanged(event);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ============ Generic Node Operations ============

    /**
     * Adds a filter node as a child of another node.
     *
     * @param parent the parent node
     * @param child the child node to add
     */
    public void addNode(FilterNode parent, FilterNode child) {
        lock.writeLock().lock();
        try {
            // Clear old sources from the child (and its descendants) so that
            // the node is repopulated purely from its new parent's outputs.
            // This is essential when reparenting a node via drag-and-drop.
            clearNodeSources(child);

            // Populate with sources from parent BEFORE firing NODES_ADDED
            // This prevents the view from seeing an empty node and then receiving
            // a separate SOURCES_ADDED event that would double the source tree nodes
            for (SourceAndConverter<?> source : parent.outputSources()) {
                Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes = new HashMap<>();
                addSourceToNode(source, child, affectedNodes);
            }

            int insertIndex = parent.childCount();
            parent.addChild(child);

            // Fire structure event only - no separate SOURCES_ADDED needed
            StructureChangedEvent structEvent = new StructureChangedEvent(
                    StructureChangedEvent.Type.NODES_ADDED,
                    Collections.singletonList(child),
                    parent,
                    Collections.singletonList(insertIndex));
            fireStructureChanged(structEvent);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a filter node from its parent.
     *
     * @param node the node to remove
     */
    public void removeNode(FilterNode node) {
        lock.writeLock().lock();
        try {
            FilterNode parent = node.parent();
            if (parent == null) {
                return; // Can't remove root
            }

            int removeIndex = parent.children().indexOf(node);
            parent.removeChild(node);

            // Clean up source index
            for (SourceAndConverter<?> source : node.outputSources()) {
                Set<FilterNode> nodes = sourceIndex.get(source);
                if (nodes != null) {
                    removeNodesRecursively(node, nodes);
                }
            }

            StructureChangedEvent event = new StructureChangedEvent(
                    StructureChangedEvent.Type.NODES_REMOVED,
                    Collections.singletonList(node),
                    parent,
                    Collections.singletonList(removeIndex));
            fireStructureChanged(event);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the filter nodes that contain a specific source.
     *
     * @param source the source to look up
     * @return an unmodifiable set of filter nodes, or empty set if not found
     */
    public Set<FilterNode> getNodesForSource(SourceAndConverter<?> source) {
        lock.readLock().lock();
        try {
            Set<FilterNode> nodes = sourceIndex.get(source);
            return nodes != null ? Collections.unmodifiableSet(new HashSet<>(nodes)) : Collections.emptySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if a source has been consumed by any filter node.
     *
     * @param source the source to check
     * @return true if the source is in the model
     */
    public boolean hasConsumed(SourceAndConverter<?> source) {
        lock.readLock().lock();
        try {
            return root.hasConsumed(source);
        } finally {
            lock.readLock().unlock();
        }
    }
}
