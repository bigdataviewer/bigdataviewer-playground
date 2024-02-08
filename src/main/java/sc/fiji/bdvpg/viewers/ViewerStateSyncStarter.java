/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2024 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.viewers;

import bdv.util.BdvHandle;
import bdv.util.PlaceHolderSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerStateChange;
import bdv.viewer.ViewerStateChangeListener;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ViewerStateSyncStarter implements Runnable {

	/**
	 * Array of BdvHandles to synchronize
	 */
	final ViewerAdapter[] handles;

	public ViewerStateSyncStarter(ViewerAdapter... handles) {
		this.handles = handles;
	}

	/**
	 * Map which links each BdvHandle to the TransformListener which has been
	 * added for synchronization purpose. This object contains all what's needed
	 * to stop the synchronization
	 */
	final Map<ViewerAdapter, ViewerStateChangeListener> handleToStateListener =
		new HashMap<>();

	final AtomicBoolean isPropagating = new AtomicBoolean();

	@Override
	public void run() {
		isPropagating.set(false);
		for (ViewerAdapter handle : handles) {
			ViewerStateChangeListener stateListener = new BasicStateListener(handle,
				isPropagating);
			handle.state().changeListeners().add(stateListener);
			handleToStateListener.put(handle, stateListener);
		}
	}

	public Map<ViewerAdapter, ViewerStateChangeListener> getSynchronizers() {
		return handleToStateListener;
	}

	ViewerAdapter currentPropagating = null;

	void updateState(ViewerAdapter adapter) {
		isPropagating.set(true); // to ignore subsequent changes
		currentPropagating = adapter;
		for (ViewerAdapter adapterTest : handles) {
			if (!adapter.equals(adapterTest)) {
				HashSet<SourceAndConverter<?>> stateToCopy = new HashSet<>(adapter
					.state().getSources());
				HashSet<SourceAndConverter<?>> stateToAdapt = new HashSet<>(adapterTest
					.state().getSources());
				stateToAdapt.removeIf(source -> source
					.getSpimSource() instanceof PlaceHolderSource);// Get rid of overlays
				stateToCopy.removeIf(source -> source
					.getSpimSource() instanceof PlaceHolderSource); // Get rid of overlays
				// Is there any missing source in state to adapt ?
				stateToCopy.removeAll(stateToAdapt);
				if (stateToCopy.size() != 0) {
					// System.out.println("adding "+stateToCopy.size()+" source to
					// "+adapterTest);
					if (adapterTest.bvvPanel != null) {
						for (SourceAndConverter<?> source : stateToCopy) {
							adapterTest.bvvPanel.getConverterSetups().put(source,
								SourceAndConverterServices.getSourceAndConverterService()
									.getConverterSetup(source));
						}
					}
					else {
						// TODO : improve!
						SourceAndConverterServices.getBdvDisplayService().getDisplays()
							.forEach(bdvh -> {
								for (SourceAndConverter<?> source : stateToCopy) {
									bdvh.getConverterSetups().put(source,
										SourceAndConverterServices.getSourceAndConverterService()
											.getConverterSetup(source));
								}
							});
					}
					adapterTest.state().addSources(stateToCopy);
					for (SourceAndConverter<?> source : stateToCopy) {
						// if (adapter.state().isSourceActive(source)) {
						adapterTest.state().setSourceActive(source, true);
						// } else {
						// adapterTest.state().setSourceActive(source, false);
						// }
					}
				}

				// Is there any source in extra in the one to adapt ?
				stateToAdapt.removeAll(new HashSet<>(adapter.state().getSources()));
				if (stateToAdapt.size() != 0) {
					// System.out.println("removing "+stateToAdapt.size()+" source to
					// "+adapterTest);
					adapterTest.state().removeSources(stateToAdapt);
				}
			}
		}
		currentPropagating = null;
		isPropagating.set(false);
	}

	class BasicStateListener implements ViewerStateChangeListener {

		final ViewerAdapter current;
		final AtomicBoolean isPropagating;

		BasicStateListener(ViewerAdapter current, AtomicBoolean isPropagating) {
			this.current = current;
			this.isPropagating = isPropagating;
		}

		@Override
		public void viewerStateChanged(ViewerStateChange change) {
			switch (change) {
				case NUM_SOURCES_CHANGED:
					if ((!isPropagating.get()) || (currentPropagating == current)) {
						updateState(current);
					}
					break;
			}
		}
	}
}
