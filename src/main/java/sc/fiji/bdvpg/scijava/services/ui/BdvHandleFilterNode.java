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

package sc.fiji.bdvpg.scijava.services.ui;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerStateChangeListener;
import javax.swing.tree.DefaultTreeModel;

/**
 * Filter nodes which filters based on the presence or not of a
 * {@link SourceAndConverter} in the {@link BdvHandle} A listener to the state
 * of the BdvHandle {@link ViewerStateChangeListener} allows to trigger a
 * {@link FilterUpdateEvent} to the node which in turns starts to recompute the
 * downstream part of the UI tree see {@link SourceFilterNode} and
 * {@link SourceAndConverterServiceUI}
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL, 2020
 */

public class BdvHandleFilterNode extends SourceFilterNode {

	public final BdvHandle bdvh;
	final String name;

	final ViewerStateChangeListener vscl;

	public boolean filter(SourceAndConverter<?> sac) {
		return bdvh.getViewerPanel().state().getSources().contains(sac);
	}

	public BdvHandleFilterNode(DefaultTreeModel model, String name,
		BdvHandle bdvh)
	{
		super(model, name, null, false);
		this.name = name;
		this.filter = this::filter;
		this.bdvh = bdvh;

		vscl = (change) -> {
			if (change.toString().equals("NUM_SOURCES_CHANGED")) {
				update(new SourceFilterNode.FilterUpdateEvent());
			}
		};

		bdvh.getViewerPanel().state().changeListeners().add(vscl);
	}

	public void clear() { // avoid memory leak
		bdvh.getViewerPanel().state().changeListeners().remove(vscl);
	}

	public String toString() {
		return getName();
	}

	@Override
	public Object clone() {
		return new BdvHandleFilterNode(model, name, bdvh);
	}

}
