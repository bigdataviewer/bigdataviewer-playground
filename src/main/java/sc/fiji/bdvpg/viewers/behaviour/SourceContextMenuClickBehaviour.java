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

package sc.fiji.bdvpg.viewers.behaviour;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.ui.behaviour.ClickBehaviour;
import sc.fiji.bdvpg.scijava.services.SourcePopupMenu;
import sc.fiji.bdvpg.services.SourceServices;
import sc.fiji.bdvpg.source.SourceHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Behaviour that shows the context menu of all {@link sc.fiji.bdvpg.command.BdvPlaygroundActionCommand}
 * actions available for the sources provided by the supplier.
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL 2020
 */
public class SourceContextMenuClickBehaviour implements
	ClickBehaviour
{

	final BdvHandle bdv;
	final Supplier<Collection<SourceAndConverter<?>>> sourcesSupplier;

	/**
	 * @param bdv bdv handle; sources at the current mouse position are used
	 */
	public SourceContextMenuClickBehaviour(BdvHandle bdv) {
		this(bdv, () -> SourceHelper
			.getSourceAndConvertersAtCurrentMousePosition(bdv));
	}

	/**
	 * @param bdv bdv handle
	 * @param sourcesSupplier a function which returns the sources to act on
	 */
	public SourceContextMenuClickBehaviour(BdvHandle bdv,
										   Supplier<Collection<SourceAndConverter<?>>> sourcesSupplier)
	{
		this.bdv = bdv;
		this.sourcesSupplier = sourcesSupplier;
	}

	@Override
	public void click(int x, int y) {
		showPopupMenu(bdv, x, y);
	}

	private void showPopupMenu(BdvHandle bdv, int x, int y) {
		final List<SourceAndConverter<?>> sources = new ArrayList<>(sourcesSupplier
			.get());

		final SourcePopupMenu popupMenu =
			new SourcePopupMenu(() -> sources.toArray(
				new SourceAndConverter[0]), SourceServices.getContext());

		popupMenu.getPopup().show(bdv.getViewerPanel().getDisplay(), x, y);
	}
}