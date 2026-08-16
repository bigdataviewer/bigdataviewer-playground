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

import bdv.viewer.SourceAndConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.source.SourceHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Pure data model for a filter node in the source tree.
 * This class has NO Swing dependencies and is managed by {@link SourceTreeModel}.
 *
 * <p>A FilterNode filters sources that pass through it. Sources that pass the filter
 * are added to {@code outputSources} and propagated to children nodes. If
 * {@code displaySources} is true, the sources are also displayed directly under this node.</p>
 *
 * <p>Thread safety: This class is NOT thread-safe. All access should be coordinated
 * through {@link SourceTreeModel} which provides proper synchronization.</p>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class FilterNode {

    private static final Logger logger = LoggerFactory.getLogger(FilterNode.class);

    private String name;
    private Predicate<SourceAndConverter<?>> filter;
    private final boolean displaySources;
    private boolean dynamicFilter;
    private FilterNode parent;
    private final List<FilterNode> children = new ArrayList<>();

    // LinkedHashSet maintains insertion order for consistent iteration
    private final Set<SourceAndConverter<?>> inputSources = new LinkedHashSet<>();
    private final Set<SourceAndConverter<?>> outputSources = new LinkedHashSet<>();

    /**
     * Creates a new FilterNode.
     *
     * @param name the display name of this node
     * @param filter the predicate that determines which sources pass through this node
     * @param displaySources if true, sources that pass the filter are displayed as direct children
     */
    public FilterNode(String name, Predicate<SourceAndConverter<?>> filter, boolean displaySources) {
        this.name = name;
        this.filter = filter;
        this.displaySources = displaySources;
    }

    /**
     * @return the display name of this node
     */
    public String name() {
        return name;
    }

    /**
     * Sets the display name of this node.
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the filter predicate
     */
    public Predicate<SourceAndConverter<?>> getFilter() {
        return filter;
    }

    /**
     * Sets the filter predicate. This will require re-evaluation of all sources.
     * @param filter the new filter
     */
    public void setFilter(Predicate<SourceAndConverter<?>> filter) {
        this.filter = filter;
    }

    /**
     * @return true if this node's filter can change its result for the same source over time
     */
    public boolean isDynamicFilter() {
        return dynamicFilter;
    }

    /**
     * Marks this node's filter as dynamic, meaning the same source may pass or fail
     * at different times. When true, {@link #addSource} always re-evaluates the filter
     * instead of skipping sources already in {@code inputSources}.
     *
     * @param dynamicFilter true if the filter result can change over time
     */
    public void setDynamicFilter(boolean dynamicFilter) {
        this.dynamicFilter = dynamicFilter;
    }

    /**
     * @return true if sources passing the filter should be displayed directly under this node
     */
    public boolean isDisplaySources() {
        return displaySources;
    }

    /**
     * @return the parent node, or null if this is the root
     */
    public FilterNode parent() {
        return parent;
    }

    /**
     * @return a copy of the children list (thread-safe snapshot)
     */
    public synchronized List<FilterNode> children() {
        return new ArrayList<>(children);
    }

    /**
     * @return the number of child nodes
     */
    public synchronized int childCount() {
        return children.size();
    }

    /**
     * Gets a child at the specified index.
     * @param index the child index
     * @return the child node
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public synchronized FilterNode child(int index) {
        return children.get(index);
    }

    /**
     * @return a copy of the input sources set (thread-safe snapshot)
     */
    public synchronized Set<SourceAndConverter<?>> inputSources() {
        return new LinkedHashSet<>(inputSources);
    }

    /**
     * @return a copy of the output sources set (thread-safe snapshot)
     */
    public synchronized Set<SourceAndConverter<?>> outputSources() {
        return new LinkedHashSet<>(outputSources);
    }

    /**
     * @return true if this node has any output sources
     */
    public synchronized boolean hasOutputSources() {
        return !outputSources.isEmpty();
    }

    /**
     * Tests if this node has already processed the given source.
     * @param source the source to test
     * @return true if the source has passed through this node's filter and is in outputSources
     */
    public synchronized boolean hasConsumed(SourceAndConverter<?> source) {
        return outputSources.contains(source);
    }

    /**
     * Tests if the given source has been received as input by this node.
     * @param source the source to test
     * @return true if the source is in inputSources
     */
    public synchronized boolean hasReceived(SourceAndConverter<?> source) {
        return inputSources.contains(source);
    }

    // ============ Package-private methods for SourceTreeModel to use ============

    /**
     * Adds a child node. Package-private, called by SourceTreeModel.
     * @param child the child to add
     */
    synchronized void addChild(FilterNode child) {
        children.add(child);
        child.parent = this;
    }

    /**
     * Adds a child node at a specific index. Package-private, called by SourceTreeModel.
     * @param index the index at which to insert
     * @param child the child to add
     */
    synchronized void addChild(int index, FilterNode child) {
        children.add(index, child);
        child.parent = this;
    }

    /**
     * Removes a child node. Package-private, called by SourceTreeModel.
     * @param child the child to remove
     * @return true if the child was found and removed
     */
    synchronized boolean removeChild(FilterNode child) {
        boolean removed = children.remove(child);
        if (removed) {
            child.parent = null;
        }
        return removed;
    }

    /**
     * Clears all children. Package-private, called by SourceTreeModel.
     */
    synchronized void clearChildren() {
        for (FilterNode child : children) {
            child.parent = null;
        }
        children.clear();
    }

    /**
     * Adds a source to the input set and tests the filter.
     * Package-private, called by SourceTreeModel.
     *
     * @param source the source to add
     * @return true if the source passed the filter and was added to outputSources
     */
    synchronized boolean addSource(SourceAndConverter<?> source) {
        if (inputSources.contains(source)) {
            if (!dynamicFilter) {
                return false; // Static filter: already processed, result won't change
            }
            // Dynamic filter: re-evaluate, but only report as added if newly passing
            if (filter.test(source)) {
                return outputSources.add(source); // returns false if already present
            }
            return false;
        }
        inputSources.add(source);
        if (filter.test(source)) {
            return outputSources.add(source);
        }
        return false;
    }

    /**
     * Adds sources to the input set and tests the filter.
     * Package-private, called by SourceTreeModel.
     *
     * @param sources the sources to add
     * @return list of sources that passed the filter and were added to outputSources
     */
    synchronized List<SourceAndConverter<?>> addSources(Collection<SourceAndConverter<?>> sources) {
        List<SourceAndConverter<?>> passed = new ArrayList<>();
        for (SourceAndConverter<?> source : sources) {
            if (addSourceInternal(source)) {
                passed.add(source);
            }
        }
        return passed;
    }

    /**
     * Internal add without synchronization (called from already synchronized methods).
     */
    private boolean addSourceInternal(SourceAndConverter<?> source) {
        if (inputSources.contains(source)) {
            return false;
        }
        inputSources.add(source);
        if (filter.test(source)) {
            outputSources.add(source);
            return true;
        }
        return false;
    }

    /**
     * Removes a source from this node.
     * Package-private, called by SourceTreeModel.
     *
     * @param source the source to remove
     * @return true if the source was in outputSources (and thus was visible)
     */
    synchronized boolean removeSource(SourceAndConverter<?> source) {
        inputSources.remove(source);
        return outputSources.remove(source);
    }

    /**
     * Removes sources from this node.
     * Package-private, called by SourceTreeModel.
     *
     * @param sources the sources to remove
     * @return list of sources that were in outputSources (visible)
     */
    synchronized List<SourceAndConverter<?>> removeSources(Collection<SourceAndConverter<?>> sources) {
        List<SourceAndConverter<?>> removed = new ArrayList<>();
        for (SourceAndConverter<?> source : sources) {
            if (removeSourceInternal(source)) {
                removed.add(source);
            }
        }
        return removed;
    }

    /**
     * Internal remove without synchronization (called from already synchronized methods).
     */
    private boolean removeSourceInternal(SourceAndConverter<?> source) {
        inputSources.remove(source);
        return outputSources.remove(source);
    }

    /**
     * Re-evaluates the filter for a source.
     * Package-private, called by SourceTreeModel.
     *
     * @param source the source to re-evaluate
     * @return -1 if source was removed from output, 0 if unchanged, 1 if source was added to output
     */
    synchronized int reevaluateSource(SourceAndConverter<?> source) {
        if (!inputSources.contains(source)) {
            return 0; // Not our source
        }

        boolean wasInOutput = outputSources.contains(source);
        boolean passesNow = filter.test(source);

        if (wasInOutput && !passesNow) {
            outputSources.remove(source);
            return -1; // Removed
        } else if (!wasInOutput && passesNow) {
            outputSources.add(source);
            return 1; // Added
        }
        return 0; // Unchanged
    }

    /**
     * Clears all sources from this node.
     * Package-private, called by SourceTreeModel.
     */
    synchronized void clearSources() {
        inputSources.clear();
        outputSources.clear();
    }

    // ============ Lifecycle ============

    /**
     * Releases whatever this node registered outside of the tree.
     *
     * <p>Called by {@link SourceTreeModel} when the node leaves the tree, and by
     * {@link SourceTreeModel#dispose()} for every node still in it. The base
     * implementation does nothing: a plain node only holds outgoing references,
     * so dropping its sources and children is enough to release it.</p>
     *
     * <p>Override this when a node subscribes to something that outlives the
     * tree - a viewer state, a service, a timer - otherwise that subscription
     * keeps the node, and through it every source it holds, alive forever. See
     * {@link BdvHandleFilterNode#cleanup()}.</p>
     *
     * <p><b>Never called while the model lock is held</b>, so an implementation
     * is free to take an external monitor (as unregistering from a
     * {@link bdv.viewer.SynchronizedViewerState} does). Implementations must be
     * idempotent.</p>
     */
    public void cleanup() {
        // No external registration to release by default.
    }

    // ============ Copying ============

    /**
     * Creates a fresh, empty, detached node that filters like this one.
     *
     * <p>Used to reuse a tree's <em>structure</em> with a different set of
     * sources: the copy starts out with no sources, no children and no parent,
     * and it is up to the caller to feed it.</p>
     *
     * <h2>This is not {@link Object#clone()}</h2>
     *
     * <p>{@link FilterNode} does not implement {@link Cloneable} and must not. A
     * field-by-field copy would share the {@code inputSources} /
     * {@code outputSources} sets, so both trees would write into the same source
     * state; it would share the {@code children} list; it would carry over the
     * {@code parent} pointer into the original tree; and - worst - subclasses
     * install their predicate as {@code setFilter(this::filter)}, so the copy's
     * filter would stay bound to the original instance. Each subclass therefore
     * rebuilds itself through its own constructor instead.</p>
     *
     * <p>Returning null means "this node must not be copied", and the caller is
     * expected to drop it along with its subtree. The default implementation
     * refuses any subclass that has not overridden this method, rather than
     * building a plain {@link FilterNode} around a predicate that may be bound to
     * this instance.</p>
     *
     * @return a new node of the same type with the same filtering behaviour, or
     *         null if this node must not be copied
     */
    public FilterNode copyNode() {
        if (getClass() != FilterNode.class) {
            logger.warn("Cannot copy node '" + name() + "' of type " +
                    getClass().getName() + ": this FilterNode subclass does not " +
                    "override copyNode(), the subtree is skipped.");
            return null;
        }
        FilterNode copy = new FilterNode(name(), filter, displaySources);
        copy.setDynamicFilter(dynamicFilter);
        return copy;
    }

    /**
     * Copies this node and, recursively, everything below it.
     *
     * <p>Children that refuse to be copied are skipped, so the returned subtree
     * may be smaller than this one.</p>
     *
     * @return the copied subtree, detached and empty of sources, or null if this
     *         node itself must not be copied
     */
    public FilterNode copySubtree() {
        FilterNode copy = copyNode();
        if (copy == null) {
            return null;
        }
        // children() hands back a snapshot, so the recursion runs without holding
        // this node's monitor
        for (FilterNode child : children()) {
            FilterNode childCopy = child.copySubtree();
            if (childCopy != null) {
                copy.addChild(childCopy);
            }
        }
        return copy;
    }

    // ============ Convenience methods for programmatic tree navigation ============

    /**
     * Finds a direct child by name.
     *
     * @param childName the name to match
     * @return the first child with the given name, or null if not found
     */
    public synchronized FilterNode child(String childName) {
        for (FilterNode child : children) {
            if (child.name().equals(childName)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Subscript operator for Groovy: {@code node['childName']}.
     * Equivalent to {@link #child(String)}.
     *
     * @param childName the name to match
     * @return the first child with the given name, or null if not found
     */
    public FilterNode getAt(String childName) {
        return child(childName);
    }

    /**
     * Returns all sources that pass this node's filter, sorted.
     * Since sources propagate down the tree, this includes all sources
     * visible in any descendant of this node.
     *
     * @return sorted array of all sources in this subtree
     */
    public SourceAndConverter<?>[] sources() {
        return SourceHelper.sortDefaultGeneric(outputSources()).toArray(
                new SourceAndConverter<?>[0]);
    }

    /**
     * Returns the path from root to this node, with names separated by "&gt;".
     *
     * @return the path string
     */
    public String path() {
        String fullPath = name;
        FilterNode current = parent;
        while (current != null) {
            fullPath = current.name() + ">" + fullPath;
            current = current.parent();
        }
        return fullPath;
    }

    @Override
    public String toString() {
        return name;
    }
}
