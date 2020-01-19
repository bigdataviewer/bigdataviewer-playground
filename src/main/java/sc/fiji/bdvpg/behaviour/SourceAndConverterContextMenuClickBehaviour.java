package sc.fiji.bdvpg.behaviour;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.ui.behaviour.ClickBehaviour;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterPopupMenu;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import java.util.List;

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
		final List< SourceAndConverter > sacs = SourceAndConverterUtils.getSacsAtMousePosition( bdv );

		SourceAndConverter[] sacArray = new SourceAndConverter[sacs.size()];
		sacArray = sacs.toArray(sacArray);

		final SourceAndConverterPopupMenu popupMenu = new SourceAndConverterPopupMenu( sacArray );
		popupMenu.getPopup().show( bdv.getViewerPanel().getDisplay(), x, y );
	}

}
