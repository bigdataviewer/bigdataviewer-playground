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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    public String getName() {
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
    public FilterNode getParent() {
        return parent;
    }

    /**
     * @return a copy of the children list (thread-safe snapshot)
     */
    public synchronized List<FilterNode> getChildren() {
        return new ArrayList<>(children);
    }

    /**
     * @return the number of child nodes
     */
    public synchronized int getChildCount() {
        return children.size();
    }

    /**
     * Gets a child at the specified index.
     * @param index the child index
     * @return the child node
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public synchronized FilterNode getChild(int index) {
        return children.get(index);
    }

    /**
     * @return a copy of the input sources set (thread-safe snapshot)
     */
    public synchronized Set<SourceAndConverter<?>> getInputSources() {
        return new LinkedHashSet<>(inputSources);
    }

    /**
     * @return a copy of the output sources set (thread-safe snapshot)
     */
    public synchronized Set<SourceAndConverter<?>> getOutputSources() {
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
     * @param sac the source to test
     * @return true if the source has passed through this node's filter and is in outputSources
     */
    public synchronized boolean hasConsumed(SourceAndConverter<?> sac) {
        return outputSources.contains(sac);
    }

    /**
     * Tests if the given source has been received as input by this node.
     * @param sac the source to test
     * @return true if the source is in inputSources
     */
    public synchronized boolean hasReceived(SourceAndConverter<?> sac) {
        return inputSources.contains(sac);
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
     * @param sac the source to add
     * @return true if the source passed the filter and was added to outputSources
     */
    synchronized boolean addSource(SourceAndConverter<?> sac) {
        if (inputSources.contains(sac)) {
            if (!dynamicFilter) {
                return false; // Static filter: already processed, result won't change
            }
            // Dynamic filter: re-evaluate, but only report as added if newly passing
            if (filter.test(sac)) {
                return outputSources.add(sac); // returns false if already present
            }
            return false;
        }
        inputSources.add(sac);
        if (filter.test(sac)) {
            return outputSources.add(sac);
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
        for (SourceAndConverter<?> sac : sources) {
            if (addSourceInternal(sac)) {
                passed.add(sac);
            }
        }
        return passed;
    }

    /**
     * Internal add without synchronization (called from already synchronized methods).
     */
    private boolean addSourceInternal(SourceAndConverter<?> sac) {
        if (inputSources.contains(sac)) {
            return false;
        }
        inputSources.add(sac);
        if (filter.test(sac)) {
            outputSources.add(sac);
            return true;
        }
        return false;
    }

    /**
     * Removes a source from this node.
     * Package-private, called by SourceTreeModel.
     *
     * @param sac the source to remove
     * @return true if the source was in outputSources (and thus was visible)
     */
    synchronized boolean removeSource(SourceAndConverter<?> sac) {
        inputSources.remove(sac);
        return outputSources.remove(sac);
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
        for (SourceAndConverter<?> sac : sources) {
            if (removeSourceInternal(sac)) {
                removed.add(sac);
            }
        }
        return removed;
    }

    /**
     * Internal remove without synchronization (called from already synchronized methods).
     */
    private boolean removeSourceInternal(SourceAndConverter<?> sac) {
        inputSources.remove(sac);
        return outputSources.remove(sac);
    }

    /**
     * Re-evaluates the filter for a source.
     * Package-private, called by SourceTreeModel.
     *
     * @param sac the source to re-evaluate
     * @return -1 if source was removed from output, 0 if unchanged, 1 if source was added to output
     */
    synchronized int reevaluateSource(SourceAndConverter<?> sac) {
        if (!inputSources.contains(sac)) {
            return 0; // Not our source
        }

        boolean wasInOutput = outputSources.contains(sac);
        boolean passesNow = filter.test(sac);

        if (wasInOutput && !passesNow) {
            outputSources.remove(sac);
            return -1; // Removed
        } else if (!wasInOutput && passesNow) {
            outputSources.add(sac);
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

    @Override
    public String toString() {
        return name;
    }
}
