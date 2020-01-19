package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleItem;
import sc.fiji.bdvpg.scijava.command.source.BrightnessAdjusterCommand;
import sc.fiji.bdvpg.scijava.command.source.SourceColorChangerCommand;
import sc.fiji.bdvpg.scijava.command.source.SourcesInvisibleMakerCommand;
import sc.fiji.bdvpg.scijava.command.source.SourcesVisibleMakerCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class SourceAndConverterPopupMenu
{
	private JPopupMenu popup;
	private final CommandService commandService;
	private final SourceAndConverter[] sacs;

	public SourceAndConverterPopupMenu( SourceAndConverter[] sacs )
	{
		this.commandService = SourceAndConverterServices.getIJ().command();;
		this.sacs = sacs;

		createPopupMenu();
	}



	private JPopupMenu createPopupMenu()
	{
		popup = new JPopupMenu();

		addCommand( SourcesInvisibleMakerCommand.class );
		addCommand( SourcesVisibleMakerCommand.class);
		addCommand( BrightnessAdjusterCommand.class);
		addCommand( SourceColorChangerCommand.class);

		return popup;
	}

	private void addCommand( Class< ? extends Command > commandClass )
	{
		addSacCommandToJComponent( commandClass, commandService, sacs, popup );
	}


	public static void addSacActionToJComponent( final Consumer< SourceAndConverter[] > action, final String actionName, final SourceAndConverter[] sourceAndConverters, final JComponent component ) {
		JMenuItem menuItem = new JMenuItem(actionName);
		menuItem.addActionListener(e -> action.accept( sourceAndConverters ));
		component.add(menuItem);
	}

	public static void addSacCommandToJComponent( Class<? extends Command > commandClass, CommandService commandService, SourceAndConverter[] sourceAndConverters, JPopupMenu component ) {

		final CommandInfo commandInfo = commandService.getCommand( commandClass );

		for (ModuleItem input : commandInfo.inputs()) {
			if (input.getType().equals(SourceAndConverter.class)) {
				// Single Sac Command
				addSacActionToJComponent(
						(sacs) -> {
							for (SourceAndConverter sac:sacs)
								commandService.run( commandInfo, true, input.getName(), sac );
						}, commandInfo.getTitle(), sourceAndConverters, component );
			}
			if (input.getType().equals(SourceAndConverter[].class)) {
				// Multiple Sac Command
				addSacActionToJComponent(
						(sacs) -> {
							commandService.run( commandInfo, true, input.getName(), sacs);
						}, commandInfo.getTitle(), sourceAndConverters, component );
			}
		}
	}

	public JPopupMenu getPopup()
	{
		return popup;
	}
}
