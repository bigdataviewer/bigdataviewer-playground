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

        // Show sources in BdvHandle
        /*for (SourceAndConverter sac : sacs) {
            BdvService.getSourceAndConverterDisplayService().show(bdvHandle, sac);
        }*/

        if (mode.equals("Mutate")) {
            manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                // What to do with the new registration:
                //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                ManualRegistrationStopper::mutate
            );
        } else {
            manualRegistrationStopper = new ManualRegistrationStopper(manualRegistrationStarter,
                    // What to do with the new registration:
                    //  (BiFunction<AffineTransform3D, SourceAndConverter, SourceAndConverter>)
                    ManualRegistrationStopper::append
            );
        }

        manualRegistrationStarter.run();

        // JFrame serving the purpose of stopping synchronization when it is being closed
        JFrame frameStopManualTransformation = new JFrame();
        frameStopManualTransformation.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                manualRegistrationStopper.run();
                e.getWindow().dispose();
            }
        });

        JPanel pane = new JPanel();
        JTextArea textArea = new JTextArea("Manual Transform In progress...");
        textArea.setEditable(false);
        pane.add(textArea);
        frameStopManualTransformation.add(pane);

        frameStopManualTransformation.setTitle("Close window to stop transformation");
        frameStopManualTransformation.pack();
        frameStopManualTransformation.setVisible(true);

    }
}
