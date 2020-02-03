package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleItem;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.command.source.*;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.getCommandName;

public class SourceAndConverterPopupMenu
{
	private JPopupMenu popup;
	private final SourceAndConverter[] sacs;

	 public static String[] defaultPopupActions = {
			getCommandName(BdvSourcesAdderCommand.class),
			getCommandName(BdvSourcesRemoverCommand.class),
			"Inspect Sources",
			"PopupLine",
			getCommandName(SourcesInvisibleMakerCommand.class),
			getCommandName(SourcesVisibleMakerCommand.class),
			getCommandName(BrightnessAdjusterCommand.class),
			getCommandName(SourceColorChangerCommand.class),
			getCommandName(SourceAndConverterProjectionModeChangerCommand.class),
			"PopupLine",
			getCommandName(SourcesDuplicatorCommand.class),
			getCommandName(ManualTransformCommand.class),
			getCommandName(TransformedSourceWrapperCommand.class),
			getCommandName(SourcesResamplerCommand.class),
			getCommandName(ColorSourceCreatorCommand.class),
			getCommandName(LUTSourceCreatorCommand.class),
			"PopupLine",
			getCommandName(SourcesRemoverCommand.class),
			getCommandName(XmlHDF5ExporterCommand.class),
	};

	 String[] popupActions;

	public SourceAndConverterPopupMenu( SourceAndConverter[] sacs )
	{
		this(sacs, defaultPopupActions);
	}

	public SourceAndConverterPopupMenu( SourceAndConverter[] sacs, String[] actions )
	{
		this.sacs = sacs;
		this.popupActions = actions;

		createPopupMenu();
	}


	private JPopupMenu createPopupMenu()
	{
		popup = new JPopupMenu();

		for (String actionName:popupActions){
			if (actionName.equals("PopupLine")) {
				this.addPopupLine();
			} else{
				this.addPopupAction(actionName, SourceAndConverterServices.getSourceAndConverterService().getAction(actionName));
			}
		}

		return popup;
	}

	/**
	 * Adds a separator in the popup menu
	 */
	public void addPopupLine() {
		popup.addSeparator();
	}

	/**
	 * Adds a line and an action which consumes all the selected SourceAndConverter objects
	 * in the popup Menu
	 * @param action
	 * @param actionName
	 */
	public void addPopupAction( String actionName, Consumer<SourceAndConverter[]> action ) {
		if (action==null) {
			System.err.println("No action defined for action named "+actionName);
		}
		JMenuItem menuItem = new JMenuItem(actionName);
		menuItem.addActionListener(e -> action.accept(
				sacs
		));
		popup.add(menuItem);
	}

	public JPopupMenu getPopup()
	{
		return popup;
	}
}
