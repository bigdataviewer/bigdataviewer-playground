/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.scijava.command.source;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.ManualRegistrationStarter;
import sc.fiji.bdvpg.bdv.ManualRegistrationStopper;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import javax.swing.*;
import java.awt.event.WindowEvent;

/**
 *
 * @author Nicolas Chiaruttini, EPFL 2020
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Transform>Manual Sources Transformation",
description = "Manual transformation of selected sources. Works only with a single bdv window (the active one)." +
        "The sources that are not displayed but selected are transformed. During the registration, the user is" +
        "placed in the reference of the moving sources. That's why they are not moving during the registration.")

public class ManualTransformCommand implements Command {

    @Parameter(choices = {"Mutate", "Append"})
    String mode = "Mutate";

    @Parameter(label = "Select Source(s)")
    SourceAndConverter[] sacs;

    @Parameter
    BdvHandle bdvHandle;

    public void run() {
        ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacs);
        ManualRegistrationStopper manualRegistrationStopper;

        if (mode.equals("Mutate")) {
            manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    SourceTransformHelper::mutate
            );
        } else {
            manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    SourceTransformHelper::append
            );
        }

        manualRegistrationStarter.run();

        // JFrame holding apply and cancel button
        JFrame frameStopManualTransformation = new JFrame();
        JPanel pane = new JPanel();

        JButton buttonApply = new JButton("Apply And Finish");
        buttonApply.addActionListener((e) -> {
            manualRegistrationStopper.run();
            frameStopManualTransformation.dispatchEvent(new WindowEvent(frameStopManualTransformation, WindowEvent.WINDOW_CLOSING));
        });

        JButton buttonCancel = new JButton("Cancel");
        buttonCancel.addActionListener((e) -> {
            new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    SourceTransformHelper::cancel
            ).run();
            frameStopManualTransformation.dispatchEvent(new WindowEvent(frameStopManualTransformation, WindowEvent.WINDOW_CLOSING));
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
