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
package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.BdvHandle;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStarter;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformSyncStopper;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * I wanted to do this as an Interactive Command but there's no callback
 * when an interactive command is closed (bug https://github.com/scijava/scijava-common/issues/379)
 * so we cannot stop the synchronization appropriately.
 *
 * Hence the dirty JFrame the user has to close to stop synchronization ...
 *
 * TODO fix potential memory leaks which could be a consequence of this extra JFrame
 *
 * author Nicolas Chiaruttini, BIOP, EPFL, 2020
 */

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV - Synchronize Views",
            description = "Synchronizes the view of a set of BDV windows. A window popup should be closed" +
                    " to stop the synchronization")
public class ViewSynchronizerCommand implements Command {

    @Parameter(label = "Select Windows to synchronize")
    BdvHandle[] bdvhs;

    @Parameter(label = "Synchronize timepoints")
    boolean synchronizeTime = true;

    ViewerTransformSyncStarter sync;

    public void run() {
        if (bdvhs.length<2) {
            System.err.println("You should select at least 2 BDV windows!");
            return;
        }

        // Starting synchronnization of selected bdvhandles
        sync = new ViewerTransformSyncStarter(bdvhs, synchronizeTime);
        sync.setBdvHandleInitialReference( SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv());
        sync.run();

        // JFrame serving the purpose of stopping synchronization when it is being closed
        JFrame frameStopSync = new JFrame();
        frameStopSync.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                new ViewerTransformSyncStopper(sync.getSynchronizers(), sync.getTimeSynchronizers()).run();
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
