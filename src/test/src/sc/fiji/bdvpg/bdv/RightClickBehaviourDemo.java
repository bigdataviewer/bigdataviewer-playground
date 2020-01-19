package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import org.scijava.command.CommandService;
import org.scijava.ui.behaviour.ClickBehaviour;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.scijava.services.ui.BdvSourceServiceUI;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterPopupMenu;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Demonstrates visualisation of two spimData sources.
 *
 */
public class RightClickBehaviourDemo
{
	public static void main( String[] args )
	{
		// Initializes static SourceService and Display Service
		SourceAndConverterServices.InitScijavaServices();

		// Gets active BdvHandle instance
		BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

		// Import SpimData
		new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").run();
		new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();

		// Show all SourceAndConverter associated with above SpimData
		SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters().forEach( sac -> {
			SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac);
			new ViewerTransformAdjuster(bdvHandle, sac).run();
			new BrightnessAutoAdjuster(sac, 0).run();
		});


		// Add context menu to bdv, with trigger "C"
		final ClickBehaviourInstaller installer = new ClickBehaviourInstaller( bdvHandle, new ClickBehaviour()
		{
			@Override
			public void click( int x, int y )
			{
				showPopUp( bdvHandle, x, y );
			}
		} );

		installer.install( "Sources context menu", "C" );
	}

	private static void showPopUp( BdvHandle bdv, int x, int y )
	{
		final List< SourceAndConverter > sourceAndConverters = SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters();

		// TODO: have a convenience for returning an array in SourceAndConverterService?
		// TODO: make issue about using List or [] !
		SourceAndConverter[] sacs = new SourceAndConverter[sourceAndConverters.size()];
		sacs = sourceAndConverters.toArray(sacs);

		final SourceAndConverterPopupMenu popupMenu = new SourceAndConverterPopupMenu( sacs );

		popupMenu.getPopup().show( bdv.getViewerPanel().getDisplay(), x, y );

	}


}
