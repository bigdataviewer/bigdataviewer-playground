package sc.fiji.bdvpg.scijava.command.source;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.ManualRegistrationStarter;
import sc.fiji.bdvpg.bdv.ManualRegistrationStopper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.BdvService;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Sources>Manual Sources Transformation")
public class ManualTransformCommand implements Command {

    @Parameter(choices = {"Mutate", "Append"})
    String mode = "Mutate";

    @Parameter
    SourceAndConverter[] sacs;

    @Parameter
    BdvHandle bdvHandle;

    public void run() {
        ManualRegistrationStarter manualRegistrationStarter = new ManualRegistrationStarter(bdvHandle, sacs);
        ManualRegistrationStopper manualRegistrationStopper;

        if (mode.equals("Mutate")) {
            manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                ManualRegistrationStopper::mutate
            );
        } else {
            manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    ManualRegistrationStopper::append
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
                    ManualRegistrationStopper::cancel
            ).run();
            frameStopManualTransformation.dispatchEvent(new WindowEvent(frameStopManualTransformation, WindowEvent.WINDOW_CLOSING));
        });

        pane.add(buttonApply);
        pane.add(buttonCancel);
        frameStopManualTransformation.add(pane);

        frameStopManualTransformation.setTitle("Registration");
        frameStopManualTransformation.pack();
        frameStopManualTransformation.setVisible(true);

    }
}
