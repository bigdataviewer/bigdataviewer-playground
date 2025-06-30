/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.behaviour;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.apache.commons.lang.ArrayUtils;
import org.scijava.ui.behaviour.ClickBehaviour;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterPopupMenu;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Behaviour that shows the context menu of actions available that will act on
 * the sources provided by the supplier
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL 2020
 */

public class SourceAndConverterContextMenuClickBehaviour implements
	ClickBehaviour
{

	final BdvHandle bdv;
	final Supplier<Collection<SourceAndConverter<?>>> sourcesSupplier;
	String[] popupActions;

	/**
	 * @param bdv bdv handle
	 */
	public SourceAndConverterContextMenuClickBehaviour(BdvHandle bdv) {
		this(bdv, () -> SourceAndConverterHelper
			.getSourceAndConvertersAtCurrentMousePosition(bdv));
	}

	/**
	 * @param bdv bdv handle
	 * @param popupActions popup actions to be used in the clickbehaviour
	 */
	public SourceAndConverterContextMenuClickBehaviour(BdvHandle bdv,
		String[] popupActions)
	{
		this(bdv, () -> SourceAndConverterHelper
			.getSourceAndConvertersAtCurrentMousePosition(bdv), popupActions);
	}

	/**
	 * @param bdv bdv handle
	 * @param sourcesSupplier a function which returns the source to act on
	 */
	public SourceAndConverterContextMenuClickBehaviour(BdvHandle bdv,
		Supplier<Collection<SourceAndConverter<?>>> sourcesSupplier)
	{
		this(bdv, sourcesSupplier, SourceAndConverterPopupMenu.defaultPopupActions);
	}

	/**
	 * @param bdv bdv handle
	 * @param sourcesSupplier a function which returns the source to act on
	 * @param popupActions popup actions to be used in the clickbehaviour
	 */
	public SourceAndConverterContextMenuClickBehaviour(BdvHandle bdv,
		Supplier<Collection<SourceAndConverter<?>>> sourcesSupplier,
		String[] popupActions)
	{
		this.bdv = bdv;
		this.sourcesSupplier = sourcesSupplier;
		this.popupActions = popupActions;
	}

	/**
	 * @param x mouse position in x
	 * @param y mouse position in y
	 */
	@Override
	public void click(int x, int y) {
		showPopupMenu(bdv, x, y);
	}

	private void showPopupMenu(BdvHandle bdv, int x, int y) {
		final List<SourceAndConverter<?>> sacs = new ArrayList<>(sourcesSupplier
			.get());

		final SourceAndConverterPopupMenu popupMenu =
			new SourceAndConverterPopupMenu(() -> sacs.toArray(
				new SourceAndConverter[0]), popupActions);

		popupMenu.getPopup().show(bdv.getViewerPanel().getDisplay(), x, y);
	}

	/**
	 * Live removal of an action
	 * 
	 * @param name of the action to remove
	 */
	public synchronized void removeAction(String name) {
		final int index = ArrayUtils.indexOf(popupActions, name);
		if (index != -1) popupActions = (String[]) ArrayUtils.remove(popupActions,
			index);
	}

	/**
	 * @param name of the action to add
	 */
	public synchronized void addAction(String name) {
		final int index = ArrayUtils.indexOf(popupActions, name);
		if (index == -1) popupActions = (String[]) ArrayUtils.add(popupActions,
			name);
	}

}
