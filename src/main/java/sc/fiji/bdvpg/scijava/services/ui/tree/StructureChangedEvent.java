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

import java.util.Collections;
import java.util.List;

/**
 * Event fired when the structure of the tree changes (nodes added or removed).
 * This is distinct from {@link SourcesChangedEvent} which handles source data changes.
 *
 * <p>Structure changes occur when:</p>
 * <ul>
 *   <li>A new SpimData is registered, creating SpimDataFilterNode and its hierarchy</li>
 *   <li>A SpimData is removed, deleting its filter node hierarchy</li>
 *   <li>A BdvHandle is registered/unregistered</li>
 *   <li>User manually adds/removes filter nodes</li>
 * </ul>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class StructureChangedEvent {

    /**
     * The type of structural change.
     */
    public enum Type {
        /** Nodes were added to the tree */
        NODES_ADDED,
        /** Nodes were removed from the tree */
        NODES_REMOVED,
        /** A node was renamed */
        NODE_RENAMED
    }

    private final Type type;
    private final List<FilterNode> affectedNodes;
    private final FilterNode parentNode;
    private final List<Integer> childIndices;

    /**
     * Creates a new StructureChangedEvent.
     *
     * @param type the type of structural change
     * @param affectedNodes the nodes that were added/removed
     * @param parentNode the parent node where the change occurred
     * @param childIndices the indices of the affected children (for add: insertion indices, for remove: old indices)
     */
    public StructureChangedEvent(Type type,
                                  List<FilterNode> affectedNodes,
                                  FilterNode parentNode,
                                  List<Integer> childIndices) {
        this.type = type;
        this.affectedNodes = Collections.unmodifiableList(affectedNodes);
        this.parentNode = parentNode;
        this.childIndices = childIndices != null ? Collections.unmodifiableList(childIndices) : Collections.emptyList();
    }

    /**
     * Creates a node renamed event.
     *
     * @param node the node that was renamed
     * @return the event
     */
    public static StructureChangedEvent nodeRenamed(FilterNode node) {
        return new StructureChangedEvent(Type.NODE_RENAMED,
                Collections.singletonList(node),
                node.getParent(),
                null);
    }

    /**
     * @return the type of structural change
     */
    public Type getType() {
        return type;
    }

    /**
     * @return an unmodifiable list of nodes that were added or removed
     */
    public List<FilterNode> getAffectedNodes() {
        return affectedNodes;
    }

    /**
     * @return the parent node where the change occurred
     */
    public FilterNode getParentNode() {
        return parentNode;
    }

    /**
     * @return the indices of the affected children
     */
    public List<Integer> getChildIndices() {
        return childIndices;
    }

    /**
     * @return true if this event represents nodes being added
     */
    public boolean isNodesAdded() {
        return type == Type.NODES_ADDED;
    }

    /**
     * @return true if this event represents nodes being removed
     */
    public boolean isNodesRemoved() {
        return type == Type.NODES_REMOVED;
    }

    /**
     * @return true if this event represents a node being renamed
     */
    public boolean isNodeRenamed() {
        return type == Type.NODE_RENAMED;
    }

    @Override
    public String toString() {
        return "StructureChangedEvent{" +
                "type=" + type +
                ", affectedNodes=" + affectedNodes.size() +
                ", parentNode=" + (parentNode != null ? parentNode.getName() : "null") +
                '}';
    }
}
