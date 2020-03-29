package sc.fiji.bdvpg.behaviour;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RealPoint;
import org.scijava.ui.behaviour.ClickBehaviour;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterPopupMenu;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.List;
import java.util.stream.Collectors;

public class SourceAndConverterContextMenuClickBehaviour implements ClickBehaviour
{
	private final BdvHandle bdv;

	public SourceAndConverterContextMenuClickBehaviour( BdvHandle bdv )
	{
		this.bdv = bdv;
	}

	@Override
	public void click( int x, int y )
	{
		showPopupMenu( bdv, x, y );
	}

	private static void showPopupMenu( BdvHandle bdv, int x, int y )
	{
		// Gets mouse location in space (global 3D coordinates) and time
		final RealPoint mousePosInBdv = new RealPoint( 3 );
		bdv.getBdvHandle().getViewerPanel().getGlobalMouseCoordinates( mousePosInBdv );
		int timePoint = bdv.getViewerPanel().getState().getCurrentTimepoint();

		final List< SourceAndConverter > sacs =
		SourceAndConverterServices.getSourceAndConverterDisplayService().getSourceAndConverterOf(bdv)
				.stream()
				.filter(sac -> SourceAndConverterUtils.isSourcePresentAt(sac,timePoint, mousePosInBdv))
				.filter(sac -> SourceAndConverterServices.getSourceAndConverterDisplayService().isVisible(sac,bdv))
				.collect(Collectors.toList());

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

		SourceAndConverter[] sacArray = new SourceAndConverter[sacs.size()];
		sacArray = sacs.toArray(sacArray);

		final SourceAndConverterPopupMenu popupMenu = new SourceAndConverterPopupMenu( sacArray );

		popupMenu.getPopup().show( bdv.getViewerPanel().getDisplay(), x, y );
	}

}
