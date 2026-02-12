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

/**
 * Listener interface for receiving updates from {@link SourceTreeModel}.
 * Implementations should handle updates on the appropriate thread (e.g., EDT for Swing).
 *
 * <p>The model guarantees that events are fired with all necessary information
 * to perform incremental updates rather than full tree reloads.</p>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public interface SourceTreeModelListener {

    /**
     * Called when sources are added, removed, or updated in the model.
     *
     * <p>For efficient UI updates, implementations should use the
     * {@link SourcesChangedEvent#getAffectedNodes()} map to determine which
     * specific nodes need updating, rather than refreshing the entire tree.</p>
     *
     * @param event the event describing the changes
     */
    void sourcesChanged(SourcesChangedEvent event);

    /**
     * Called when the structure of the tree changes (nodes added/removed).
     *
     * <p>This is called when SpimData is registered/unregistered, creating or
     * removing entire subtrees, or when filter nodes are manually added/removed.</p>
     *
     * @param event the event describing the structural changes
     */
    void structureChanged(StructureChangedEvent event);
}
