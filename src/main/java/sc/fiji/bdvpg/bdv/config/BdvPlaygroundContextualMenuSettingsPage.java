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
package sc.fiji.bdvpg.bdv.config;

import org.mastodon.app.ui.settings.ModificationListener;
import org.mastodon.app.ui.settings.SettingsPage;
import org.scijava.listeners.Listeners;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterPopupMenu;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO WIP : link keys and actions available in bigdataviewer playground
 * NOT WORKING
 * SettingsPage comes from Mastodon
 */

public class BdvPlaygroundContextualMenuSettingsPage implements SettingsPage {

    private final String treePath;

    private final JPanel panel;

    private final Listeners.List<ModificationListener> modificationListeners;


    public BdvPlaygroundContextualMenuSettingsPage( final String treePath )
    {
        this.treePath = treePath;
        panel = new BdvPgContextMenuEditor();
        modificationListeners = new Listeners.SynchronizedList<>();
    }

    @Override
    public String getTreePath() {
        return treePath;
    }

    @Override
    public JPanel getJPanel() {
        return panel;
    }

    @Override
    public Listeners<ModificationListener> modificationListeners() {
        return modificationListeners;
    }

    /**
     * Restores current panel settings with the one present in bigdataviewer.properties file
     */
    @Override
    public void cancel() {

    }

    /**
     * Stores current panel settings in bigdataviewer.properties file if it exists,
     * to default ones otherwise
     */
    @Override
    public void apply() {

        String[] actions = ((BdvPgContextMenuEditor)panel).getActions();
        List<String> filteredActions = new ArrayList<>();

        for (int i=0;i<actions.length;i++) {
            actions[i] = actions[i].trim();
            if (actions[i]!=null) {
                if (actions[i].equals("")) {
                    filteredActions.add("PopupLine");
                } else {
                    filteredActions.add(actions[i]);
                }
            }
        }

        SourceAndConverterPopupMenu.setDefaultSettings(filteredActions.toArray(new String[filteredActions.size()]));
        // Stores
    }

    class BdvPgContextMenuEditor extends JPanel {
        private JTextArea contextMenuActions;
        public BdvPgContextMenuEditor() {
            this.setLayout(new BorderLayout());
            String[] allActionKeys =
                    new ArrayList<String>(SourceAndConverterServices
                            .getSourceAndConverterService()
                            .getActionsKeys()).toArray(new String[0]);

            JList allActions = new JList(allActionKeys);

            contextMenuActions = new JTextArea();//<>(allActionKeys);
            contextMenuActions.setFont(contextMenuActions.getFont().deriveFont(10f));
            allActions.setDragEnabled(true);
            contextMenuActions.setDragEnabled(true);
            contextMenuActions.setDropMode(DropMode.INSERT);

            this.add(new JScrollPane(contextMenuActions), BorderLayout.CENTER);
            this.add(new JScrollPane(allActions), BorderLayout.EAST);
        }

        String[] getActions() {
            String actions = contextMenuActions.getText();
            actions+="\n"; // avoids ignoring last line
            return actions.split("\n");
        }
    }

}
