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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event fired when sources are added, removed, or updated in the tree model.
 * This event contains batch information to allow efficient UI updates using
 * {@code nodesWereInserted()} and {@code nodesWereRemoved()} instead of full tree reloads.
 *
 * <p>The {@code affectedNodes} map provides a mapping from each affected {@link FilterNode}
 * to the list of sources that were added/removed/updated at that node. This allows the
 * view to perform targeted updates.</p>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class SourcesChangedEvent {

    /**
     * The type of change that occurred.
     */
    public enum Type {
        /** Sources were added to the model */
        ADDED,
        /** Sources were removed from the model */
        REMOVED,
        /** Sources were updated (filter re-evaluation) */
        UPDATED
    }

    private final Type type;
    private final Collection<SourceAndConverter<?>> sources;
    private final Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes;

    /**
     * Creates a new SourcesChangedEvent.
     *
     * @param type the type of change
     * @param sources the sources that were affected
     * @param affectedNodes mapping from filter nodes to the sources affected at that node
     */
    public SourcesChangedEvent(Type type,
                               Collection<SourceAndConverter<?>> sources,
                               Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes) {
        this.type = type;
        this.sources = Collections.unmodifiableCollection(sources);
        this.affectedNodes = Collections.unmodifiableMap(new HashMap<>(affectedNodes));
    }

    /**
     * @return the type of change
     */
    public Type getType() {
        return type;
    }

    /**
     * @return an unmodifiable collection of the sources that were affected
     */
    public Collection<SourceAndConverter<?>> getSources() {
        return sources;
    }

    /**
     * @return an unmodifiable map from filter nodes to the sources affected at each node
     */
    public Map<FilterNode, List<SourceAndConverter<?>>> getAffectedNodes() {
        return affectedNodes;
    }

    /**
     * @return true if this event has no affected nodes (no visible changes)
     */
    public boolean isEmpty() {
        return affectedNodes.isEmpty();
    }

    @Override
    public String toString() {
        return "SourcesChangedEvent{" +
                "type=" + type +
                ", sources=" + sources.size() +
                ", affectedNodes=" + affectedNodes.size() +
                '}';
    }
}
