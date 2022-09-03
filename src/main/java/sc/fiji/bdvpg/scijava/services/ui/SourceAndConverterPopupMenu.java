/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.scijava.services.ui;

import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import sc.fiji.bdvpg.bdv.config.BdvSettingsGUISetter;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesShowCommand;
import sc.fiji.bdvpg.scijava.command.source.BasicTransformerCommand;
import sc.fiji.bdvpg.scijava.command.source.ColorSourceCreatorCommand;
import sc.fiji.bdvpg.scijava.command.source.InteractiveBrightnessAdjusterCommand;
import sc.fiji.bdvpg.scijava.command.source.LUTSourceCreatorCommand;
import sc.fiji.bdvpg.scijava.command.source.MakeMetadataFilterNodeCommand;
import sc.fiji.bdvpg.scijava.command.source.ManualTransformCommand;
import sc.fiji.bdvpg.scijava.command.source.SourceColorChangerCommand;
import sc.fiji.bdvpg.scijava.command.source.SourcesDuplicatorCommand;
import sc.fiji.bdvpg.scijava.command.source.SourcesInvisibleMakerCommand;
import sc.fiji.bdvpg.scijava.command.source.SourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.command.source.SourcesResamplerCommand;
import sc.fiji.bdvpg.scijava.command.source.SourcesVisibleMakerCommand;
import sc.fiji.bdvpg.scijava.command.source.TransformedSourceWrapperCommand;
import sc.fiji.bdvpg.scijava.command.source.XmlHDF5ExporterCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.getCommandName;

public class SourceAndConverterPopupMenu {

	private JPopupMenu popup;
	private final Supplier<SourceAndConverter<?>[]> sacs_supplier;

	final public static String[] defaultPopupActions = { getCommandName(
		BdvSourcesAdderCommand.class), getCommandName(BdvSourcesShowCommand.class),
		getCommandName(BdvSourcesRemoverCommand.class), "Inspect Sources",
		"PopupLine", getCommandName(SourcesInvisibleMakerCommand.class),
		getCommandName(SourcesVisibleMakerCommand.class), getCommandName(
			InteractiveBrightnessAdjusterCommand.class), getCommandName(
				SourceColorChangerCommand.class), "PopupLine", getCommandName(
					BasicTransformerCommand.class), getCommandName(
						SourcesDuplicatorCommand.class), getCommandName(
							ManualTransformCommand.class), getCommandName(
								TransformedSourceWrapperCommand.class), getCommandName(
									SourcesResamplerCommand.class), getCommandName(
										ColorSourceCreatorCommand.class), getCommandName(
											LUTSourceCreatorCommand.class), "PopupLine",
		getCommandName(SourcesRemoverCommand.class), getCommandName(
			XmlHDF5ExporterCommand.class), "PopupLine", getCommandName(
				MakeMetadataFilterNodeCommand.class)

	};

	String[] popupActions;

	public SourceAndConverterPopupMenu(
		Supplier<SourceAndConverter<?>[]> sacs_supplier, String path,
		String context)
	{
		this.sacs_supplier = sacs_supplier;
		this.popupActions = defaultPopupActions;

		File f = BdvSettingsGUISetter.getActionFile(path, context);
		if (f.exists()) {
			try {
				Gson gson = new Gson();
				popupActions = gson.fromJson(new FileReader(f.getAbsoluteFile()),
					String[].class);
				if ((popupActions == null) || (popupActions.length == 0)) {
					popupActions = new String[] { "Warning: Empty " + f
						.getAbsolutePath() + " config file." };
				}
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			File fdefault = new File(f.getAbsolutePath() + ".default.txt");
			if (fdefault.exists()) {
				try {
					Gson gson = new Gson();
					popupActions = gson.fromJson(new FileReader(fdefault
						.getAbsoluteFile()), String[].class);
					if ((popupActions == null) || (popupActions.length == 0)) {
						popupActions = new String[] { "Warning: Empty " + fdefault
							.getAbsolutePath() + " config file." };
					}
				}
				catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			else {
				System.err.println("Bdv Playground actions settings File " + f
					.getAbsolutePath() + " does not exist.");
				System.err.println("Bdv Playground default actions settings File " +
					fdefault.getAbsolutePath() + " does not exist.");

			}
		}

		createPopupMenu();
	}

	public SourceAndConverterPopupMenu(
		Supplier<SourceAndConverter<?>[]> sacs_supplier)
	{
		this(sacs_supplier, "", "tree");
	}

	public SourceAndConverterPopupMenu(
		Supplier<SourceAndConverter<?>[]> sacs_supplier, String[] actions)
	{
		this.sacs_supplier = sacs_supplier;
		this.popupActions = actions;
		createPopupMenu();
	}

	private void createPopupMenu() {
		popup = new JPopupMenu();

		for (String actionName : popupActions) {
			if (actionName.equals("PopupLine")) {
				this.addPopupLine();
			}
			else {
				this.addPopupAction(actionName, SourceAndConverterServices
					.getSourceAndConverterService().getAction(actionName));
			}
		}
	}

	/**
	 * Adds a separator in the popup menu
	 */
	public void addPopupLine() {
		popup.addSeparator();
	}

	/**
	 * Adds a line and an action which consumes all the selected
	 * SourceAndConverter objects in the popup Menu
	 * 
	 * @param action action method
	 * @param actionName action name
	 */
	public void addPopupAction(String actionName,
		Consumer<SourceAndConverter<?>[]> action)
	{
		JMenuItem menuItem = new JMenuItem(actionName);
		if (action == null) {
			menuItem.addActionListener(e -> System.err.println(
				"No action defined for action named " + actionName));
		}
		else {
			menuItem.addActionListener(e -> action.accept(sacs_supplier.get()));
		}
		popup.add(menuItem);
	}

	public JPopupMenu getPopup() {
		return popup;
	}
}
