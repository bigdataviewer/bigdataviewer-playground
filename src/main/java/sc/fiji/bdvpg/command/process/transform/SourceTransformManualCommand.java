/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.command.process.transform;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.viewers.bdv.ManualRegistrationStarter;
import sc.fiji.bdvpg.viewers.bdv.ManualRegistrationStopper;
import sc.fiji.bdvpg.scijava.BdvPgMenus;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.source.SourceHelper;
import sc.fiji.bdvpg.source.transform.SourceTransformHelper;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.event.WindowEvent;

/**
 * @author Nicolas Chiaruttini, EPFL 2020
 */

@SuppressWarnings({ "CanBeFinal", "unused" }) // Because SciJava command fields
																							// are set by SciJava
																							// pre-processors

@Plugin(type = BdvPlaygroundActionCommand.class,
	menu = {
			@Menu(label = BdvPgMenus.L1),
			@Menu(label = BdvPgMenus.L2),
			@Menu(label = BdvPgMenus.ProcessMenu, weight = BdvPgMenus.ProcessW),
			@Menu(label = "Transform"),
			@Menu(label = "Source - Manual Transformation", weight = 3)
	},
	description = "Manual transformation of selected sources. Works only with a single bdv window (the active one)." +
		"The sources that are not displayed but selected are transformed. During the registration, the user is" +
		"placed in the reference of the moving sources. That's why they are not moving during the registration.")

public class SourceTransformManualCommand implements BdvPlaygroundActionCommand {

	@Parameter(label = "Mode",
			description = "How to apply the transformation: Mutate modifies existing transform, Append adds a new transform layer, Wrap creates a new TransformedSource, Log outputs the transformation matrix",
			choices = { "Mutate", "Append", "Append (all timepoints)",
		"Append (timepoints before)", "Append (timepoints after)", "Wrap", "Log" })
	String mode = "Mutate";

	@Parameter(label = "Select Source(s)",
			description = "The source(s) to manually transform")
	SourceAndConverter<?>[] sources;

	@Parameter(label = "Select BDV Window",
			description = "The BigDataViewer window used for manual registration")
	BdvHandle bdvh;

	public void run() {
		ManualRegistrationStarter manualRegistrationStarter =
			new ManualRegistrationStarter(bdvh, sources);
		ManualRegistrationStopper manualRegistrationStopper;
		switch (mode) {
			case "Mutate":
				manualRegistrationStopper = new ManualRegistrationStopper(
					manualRegistrationStarter, SourceTransformHelper::mutate);
				break;
			case "Append":
				manualRegistrationStopper = new ManualRegistrationStopper(
					manualRegistrationStarter, SourceTransformHelper::append);
				break;
			case "Append (all timepoints)":
				manualRegistrationStopper = new ManualRegistrationStopper(
					manualRegistrationStarter, SourceTransformHelper::append);
				manualRegistrationStopper.setTimeRange(0, SourceHelper
					.getMaxTimepoint(sources));
				break;
			case "Append (timepoints before)":
				manualRegistrationStopper = new ManualRegistrationStopper(
					manualRegistrationStarter, SourceTransformHelper::append);
				manualRegistrationStopper.setTimeRange(0, bdvh.getViewerPanel().state()
					.getCurrentTimepoint() + 1);
				break;
			case "Append (timepoints after)":
				manualRegistrationStopper = new ManualRegistrationStopper(
					manualRegistrationStarter, SourceTransformHelper::append);
				manualRegistrationStopper.setTimeRange(bdvh.getViewerPanel().state()
					.getCurrentTimepoint(), SourceHelper.getMaxTimepoint(
						sources));
				break;
			case "Log":
				manualRegistrationStopper = new ManualRegistrationStopper(
					manualRegistrationStarter, (transform,
						source) -> SourceTransformHelper.log(transform, source, IJ::log));
				break;
			default:
				manualRegistrationStopper = new ManualRegistrationStopper(
					manualRegistrationStarter,
					SourceTransformHelper::createNewTransformedSourceAndConverter);
		}

		manualRegistrationStarter.run();

		// JFrame holding apply and cancel button
		JFrame frameStopManualTransformation = new JFrame();
		JPanel pane = new JPanel();

		JButton buttonApply = new JButton("Apply And Finish");
		buttonApply.addActionListener((e) -> {
			manualRegistrationStopper.run();
			frameStopManualTransformation.dispatchEvent(new WindowEvent(
				frameStopManualTransformation, WindowEvent.WINDOW_CLOSING));
		});

		JButton buttonCancel = new JButton("Cancel");
		buttonCancel.addActionListener((e) -> {
			new ManualRegistrationStopper(manualRegistrationStarter,
				// What to do with the new registration:
				// (BiFunction<AffineTransform3D, SourceAndConverter,
				// SourceAndConverter>)
				SourceTransformHelper::cancel).run();
			frameStopManualTransformation.dispatchEvent(new WindowEvent(
				frameStopManualTransformation, WindowEvent.WINDOW_CLOSING));
			frameStopManualTransformation.dispose();
		});

		pane.add(buttonApply);
		pane.add(buttonCancel);
		frameStopManualTransformation.add(pane);

		frameStopManualTransformation.setTitle("Registration");
		frameStopManualTransformation.pack();
		frameStopManualTransformation.setVisible(true);

	}
}
