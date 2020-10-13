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
