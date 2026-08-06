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
import bdv.viewer.SynchronizedViewerState;
import bdv.viewer.ViewerStateChangeListener;
import sc.fiji.bdvpg.viewer.bdv.BdvHandleHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter node that filters sources based on their presence in a specific BdvHandle.
 *
 * <p>This node listens to the BdvHandle's viewer state and triggers filter updates
 * when sources are added or removed from the viewer.</p>
 *
 * <h2>Lock ordering</h2>
 *
 * <p>{@link SynchronizedViewerState} notifies its change listeners <em>while holding
 * its own monitor</em>, and this node's listener then asks {@link SourceTreeModel} to
 * refresh, which takes the model write lock. The resulting order is
 * {@code viewer state monitor -> tree model lock}. To avoid a lock inversion (and the
 * deadlock it caused between the EDT adding an overlay source and a SciJava thread
 * registering a BdvHandle), the filter must <em>never</em> take the viewer state
 * monitor while the tree model lock is held. It therefore tests against
 * {@link #viewerSources()}, a snapshot refreshed outside of the model lock, rather
 * than querying the live viewer state.</p>
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class BdvHandleFilterNode extends FilterNode {

    private final BdvHandle bdvHandle;
    private final ViewerStateChangeListener stateListener;
    private Runnable filterUpdateCallback;

    /**
     * Snapshot of the sources currently displayed in the BdvHandle. Read by
     * {@link #filter} (possibly under the tree model lock), written only from
     * {@link #refreshViewerSources()}, which is called outside of that lock.
     */
    private volatile Set<SourceAndConverter<?>> viewerSources = Collections.emptySet();

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
        setDynamicFilter(true);

        // Listen for source changes in the BdvHandle
        stateListener = change -> {
            if ("NUM_SOURCES_CHANGED".equals(change.toString())) {
                // Called with the viewer state monitor held: refresh the snapshot
                // here (reentrant, no extra lock) so that the model refresh below
                // never has to query the viewer state under the model lock.
                refreshViewerSources();
                if (filterUpdateCallback != null) {
                    filterUpdateCallback.run();
                }
            }
        };
        bdvHandle.getViewerPanel().state().changeListeners().add(stateListener);
        refreshViewerSources();
    }

    /**
     * Re-reads the sources of the BdvHandle into the snapshot used by {@link #filter}.
     *
     * <p><b>Must not be called while the {@link SourceTreeModel} lock is held</b>: it
     * acquires the viewer state monitor, and doing so under the model lock would
     * invert the {@code viewer state monitor -> tree model lock} order used by the
     * viewer state change listener.</p>
     */
    public final void refreshViewerSources() {
        final SynchronizedViewerState state = bdvHandle.getViewerPanel().state();
        // getSources() returns a collection backed by the state, so the copy has to
        // be made while holding the state monitor.
        synchronized (state) {
            viewerSources = new HashSet<>(state.getSources());
        }
    }

    /**
     * @return the last known snapshot of the sources displayed in the BdvHandle
     */
    public Set<SourceAndConverter<?>> viewerSources() {
        return viewerSources;
    }

    /**
     * Returns the live window title of the BdvHandle, so the tree label can never
     * hold a stale copy of the title (single source of truth = the BDV window).
     *
     * <p>The value stored via {@link #setName} is kept only as a fallback and is
     * not used here. After the window title changes, the model must still fire a
     * {@code NODE_RENAMED} event so the Swing view re-reads this value.</p>
     *
     * @return the current window title of the BdvHandle
     */
    @Override
    public String name() {
        return BdvHandleHelper.getWindowTitle(bdvHandle);
    }

    @Override
    public String toString() {
        return name();
    }

    /**
     * Filters sources that are present in the BdvHandle's viewer.
     *
     * <p>Tests against the {@link #viewerSources()} snapshot on purpose: this method
     * runs under the {@link SourceTreeModel} lock and must not take the viewer state
     * monitor (see the lock ordering note in the class javadoc).</p>
     */
    private boolean filter(SourceAndConverter<?> source) {
        return viewerSources.contains(source);
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
