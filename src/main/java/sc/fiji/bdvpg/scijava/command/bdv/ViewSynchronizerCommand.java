package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStarter;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * I wanted to do this as an Interactive Command but there's no callback
 * when an interactive command is closed (bug https://github.com/scijava/scijava-common/issues/379)
 * -> we cannot stop the synchronization appropriately.
 *
 * Hence the dirty JFrame the user has to close to stop synchronization ...
 *
 * author Nicolas Chiaruttini
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Synchronize Views")
public class ViewSynchronizerCommand implements Command {

    @Parameter(label = "Select Windows to synchronize")
    BdvHandle[] bdvhs;

    ViewerTransformSyncStarter sync;

    public void run() {
        // Starting synchronnization of selected bdvhandles
        sync = new ViewerTransformSyncStarter(bdvhs);
        sync.setBdvHandleInitialReference( SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv());
        sync.run();

        // JFrame serving the purpose of stopping synchronization when it is being closed
        JFrame frameStopSync = new JFrame();
        frameStopSync.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                new ViewerTransformSyncStopper(sync.getSynchronizers()).run();
                e.getWindow().dispose();
            }
        });
        frameStopSync.setTitle("Close window to stop synchronization");

        // Building JFrame with a simple panel and textarea
        String text = "";
        for (BdvHandle bdvh:bdvhs) {
            text+= BdvHandleHelper.getWindowTitle(bdvh)+"\n";
        }

        JPanel pane = new JPanel();
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        pane.add(textArea);
        frameStopSync.add(pane);
        frameStopSync.setPreferredSize(new Dimension(600,100));

        frameStopSync.pack();
        frameStopSync.setVisible(true);
    }

}
