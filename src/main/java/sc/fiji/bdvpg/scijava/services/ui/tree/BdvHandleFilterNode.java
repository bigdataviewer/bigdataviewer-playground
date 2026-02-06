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

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerStateChangeListener;

/**
 * Filter node that filters sources based on their presence in a specific BdvHandle.
 *
 * <p>This node listens to the BdvHandle's viewer state and triggers filter updates
 * when sources are added or removed from the viewer.</p>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class BdvHandleFilterNode extends FilterNode {

    private final BdvHandle bdvHandle;
    private final ViewerStateChangeListener stateListener;
    private Runnable filterUpdateCallback;

    /**
     * Creates a new BdvHandleFilterNode.
     *
     * @param name the display name
     * @param bdvHandle the BdvHandle to filter for
     */
    public BdvHandleFilterNode(String name, BdvHandle bdvHandle) {
        super(name, null, false);
        this.bdvHandle = bdvHandle;
        setFilter(this::filter);

        // Listen for source changes in the BdvHandle
        stateListener = change -> {
            if ("NUM_SOURCES_CHANGED".equals(change.toString())) {
                if (filterUpdateCallback != null) {
                    filterUpdateCallback.run();
                }
            }
        };
        bdvHandle.getViewerPanel().state().changeListeners().add(stateListener);
    }

    /**
     * Filters sources that are present in the BdvHandle's viewer.
     */
    private boolean filter(SourceAndConverter<?> sac) {
        return bdvHandle.getViewerPanel().state().getSources().contains(sac);
    }

    /**
     * @return the BdvHandle this node filters for
     */
    public BdvHandle getBdvHandle() {
        return bdvHandle;
    }

    /**
     * Sets a callback to be invoked when the filter needs to be re-evaluated.
     * This is called by SourceTreeModel to handle BdvHandle state changes.
     *
     * @param callback the callback to invoke
     */
    public void setFilterUpdateCallback(Runnable callback) {
        this.filterUpdateCallback = callback;
    }

    /**
     * Cleans up resources by removing the listener from the BdvHandle.
     * This should be called when the node is removed from the tree.
     */
    public void cleanup() {
        bdvHandle.getViewerPanel().state().changeListeners().remove(stateListener);
        filterUpdateCallback = null;
    }
}
