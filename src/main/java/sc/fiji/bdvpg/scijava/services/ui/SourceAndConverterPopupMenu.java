package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleItem;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesShowCommand;
import sc.fiji.bdvpg.scijava.command.bdv.ScreenShotMakerCommand;
import sc.fiji.bdvpg.scijava.command.source.*;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.function.Consumer;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.getCommandName;

public class SourceAndConverterPopupMenu
{
	private JPopupMenu popup;
	private final SourceAndConverter[] sacs;

	 public static String[] defaultPopupActions = {
			getCommandName(BdvSourcesAdderCommand.class),
			getCommandName(BdvSourcesShowCommand.class),
			getCommandName(BdvSourcesRemoverCommand.class),
			"Inspect Sources",
			"PopupLine",
			getCommandName(SourcesInvisibleMakerCommand.class),
			getCommandName(SourcesVisibleMakerCommand.class),
			getCommandName(BrightnessAdjusterCommand.class),
			getCommandName(SourceColorChangerCommand.class),
			getCommandName(SourceAndConverterProjectionModeChangerCommand.class),
			"PopupLine",
			getCommandName(BasicTransformerCommand.class),
			getCommandName(SourcesDuplicatorCommand.class),
			getCommandName(ManualTransformCommand.class),
			getCommandName(TransformedSourceWrapperCommand.class),
			getCommandName(SourcesResamplerCommand.class),
			getCommandName(ColorSourceCreatorCommand.class),
			getCommandName(LUTSourceCreatorCommand.class),
			"PopupLine",
			getCommandName(SourcesRemoverCommand.class),
			getCommandName(XmlHDF5ExporterCommand.class),
			"PopupLine"
	 };

	 String[] popupActions;

	public static synchronized void setDefaultSettings(String[] newDefaults) {
		defaultPopupActions = newDefaults.clone();
	}

	public SourceAndConverterPopupMenu( SourceAndConverter[] sacs )
	{
		this.sacs = sacs;
		this.popupActions = defaultPopupActions;

		File f = new File("bdvpgsettings"+File.separator+"DefaultPopupActions.json");
		if (f.exists()) {
			try {
				Gson gson = new Gson();
				popupActions = gson.fromJson(new FileReader(f.getAbsoluteFile()), String[].class);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		createPopupMenu();
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
		JMenuItem menuItem = new JMenuItem(actionName);
		if (action == null) {
			menuItem.addActionListener(e -> System.err.println("No action defined for action named "+actionName));
		} else {
			menuItem.addActionListener(e -> action.accept(sacs));
		}
		popup.add(menuItem);
	}

	public JPopupMenu getPopup()
	{
		return popup;
	}
}
