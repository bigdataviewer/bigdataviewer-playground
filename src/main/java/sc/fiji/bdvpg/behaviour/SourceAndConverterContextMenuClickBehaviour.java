package sc.fiji.bdvpg.behaviour;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.RealPoint;
import org.scijava.ui.behaviour.ClickBehaviour;
import sc.fiji.bdvpg.bdv.BdvUtils;
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
		RealPoint bdvMouseLocation = BdvUtils.getPhysicalMouseCoordinates(bdv);
		int timePoint = bdv.getViewerPanel().getState().getCurrentTimepoint();

		final List< SourceAndConverter > sacs = //SourceAndConverterUtils.getSacsAtMousePosition( bdv );
		SourceAndConverterServices.getSourceAndConverterDisplayService().getSourceAndConverterOf(bdv)
				.stream().filter(sac -> SourceAndConverterUtils.isSourcePresentAt(sac,timePoint, bdvMouseLocation))
				.collect(Collectors.toList());

		if ( sacs.size() == 0 )
			return;

		SourceAndConverter[] sacArray = new SourceAndConverter[sacs.size()];
		sacArray = sacs.toArray(sacArray);

		final SourceAndConverterPopupMenu popupMenu = new SourceAndConverterPopupMenu( sacArray );

		popupMenu.getPopup().show( bdv.getViewerPanel().getDisplay(), x, y );
	}

}
