/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesShowCommand;
import sc.fiji.bdvpg.scijava.command.source.*;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.getCommandName;

public class SourceAndConverterPopupMenu
{
	private JPopupMenu popup;
	private final Supplier<SourceAndConverter[]> sacs_supplier;

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

	public SourceAndConverterPopupMenu( Supplier<SourceAndConverter[]> sacs_supplier )
	{
		this.sacs_supplier = sacs_supplier;
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

	public SourceAndConverterPopupMenu( Supplier<SourceAndConverter[]> sacs_supplier, String[] actions )
	{
		this.sacs_supplier = sacs_supplier;
		this.popupActions = actions;
		createPopupMenu();
	}


	private void createPopupMenu()
	{
		popup = new JPopupMenu();

		for (String actionName:popupActions){
			if (actionName.equals("PopupLine")) {
				this.addPopupLine();
			} else{
				this.addPopupAction(actionName, SourceAndConverterServices.getSourceAndConverterService().getAction(actionName));
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
	 * Adds a line and an action which consumes all the selected SourceAndConverter objects
	 * in the popup Menu
	 * @param action action method
	 * @param actionName action name
	 */
	public void addPopupAction( String actionName, Consumer<SourceAndConverter[]> action ) {
		JMenuItem menuItem = new JMenuItem(actionName);
		if (action == null) {
			menuItem.addActionListener(e -> System.err.println("No action defined for action named "+actionName));
		} else {
			menuItem.addActionListener(e -> action.accept(sacs_supplier.get()));
		}
		popup.add(menuItem);
	}

	public JPopupMenu getPopup()
	{
		return popup;
	}
}
