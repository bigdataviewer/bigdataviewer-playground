/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import net.imglib2.RealPoint;
import org.scijava.ui.behaviour.ClickBehaviour;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterPopupMenu;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Behaviour that shows the context menu of actions available that will act on the sources
 * provided by the supplier
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL 2020
 */

public class SourceAndConverterContextMenuClickBehaviour implements ClickBehaviour
{
	final BdvHandle bdv;
	final Supplier<Collection<SourceAndConverter<?>>> sourcesSupplier;
	final String[] popupActions;

	public SourceAndConverterContextMenuClickBehaviour( BdvHandle bdv )
	{
		this(bdv,
		() -> {
			// Gets mouse location in space (global 3D coordinates) and time
			final RealPoint mousePosInBdv = new RealPoint( 3 );
			bdv.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( mousePosInBdv );
			int timePoint = bdv.getViewerPanel().state().getCurrentTimepoint();

			return SourceAndConverterServices.getSourceAndConverterDisplayService().getSourceAndConverterOf(bdv)
				.stream()
				.filter(sac -> SourceAndConverterUtils.isSourcePresentAt(sac,timePoint, mousePosInBdv))
				.filter(sac -> SourceAndConverterServices.getSourceAndConverterDisplayService().isVisible(sac,bdv))
				.collect(Collectors.toList());
		});
	}

	public SourceAndConverterContextMenuClickBehaviour( BdvHandle bdv, Supplier<Collection<SourceAndConverter<?>>> sourcesSupplier )
	{
		this(bdv, sourcesSupplier, SourceAndConverterPopupMenu.defaultPopupActions);
	}

	public SourceAndConverterContextMenuClickBehaviour( BdvHandle bdv, Supplier<Collection<SourceAndConverter<?>>> sourcesSupplier, String[] popupActions )
	{
		this.bdv = bdv;
		this.sourcesSupplier = sourcesSupplier;
		this.popupActions = popupActions;
	}

	@Override
	public void click( int x, int y )
	{
		showPopupMenu( bdv, x, y );
	}

	private void showPopupMenu( BdvHandle bdv, int x, int y )
	{

		final List< SourceAndConverter > sacs = new ArrayList<>();
		sacs.addAll(sourcesSupplier.get());

		if ( sacs.size() == 0 )
			return;

		String message = "";
		if (sacs.size()>1) {
			message +=  sacs.size()+" sources selected";
		} else {
			for (SourceAndConverter sac:sacs) {
				message += sac.getSpimSource().getName(); //"["+sac.getSpimSource().getClass().getSimpleName()+"]");
			}
		}

		bdv.getViewerPanel().showMessage(message);

		final SourceAndConverterPopupMenu popupMenu = new SourceAndConverterPopupMenu( () -> sacs.toArray(new SourceAndConverter[sacs.size()]), popupActions );

		popupMenu.getPopup().show( bdv.getViewerPanel().getDisplay(), x, y );
	}

}
