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
package sc.fiji.bdvpg.bdv.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mastodon.app.ui.settings.ModificationListener;
import org.mastodon.app.ui.settings.SettingsPage;
import org.scijava.listeners.Listeners;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterPopupMenu;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
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

    final File jsonActionFile;

    public BdvPlaygroundContextualMenuSettingsPage( final String treePath, File jsonEditorActions )
    {
        this.treePath = treePath;
        jsonActionFile = jsonEditorActions;
        String[] iniActions = new String[]{};
        if (jsonActionFile.exists()) {
            try {
                Gson gson = new Gson();
                iniActions = gson.fromJson(new FileReader(jsonActionFile.getAbsoluteFile()), String[].class);
                if (iniActions==null) iniActions = new String[]{};
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Bdv Playground actions settings File "+jsonActionFile.getAbsolutePath()+" does not exist.");
        }

        panel = new BdvPgContextMenuEditor(iniActions);
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
     * Stores current panel settings in the specified input file if it exists,
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

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String actionsString = gson.toJson(filteredActions.toArray(new String[0]));

        try {
            PrintWriter out = new PrintWriter(jsonActionFile);
            out.println(actionsString);
            out.close();
        } catch (FileNotFoundException e) {
            System.err.println("Could not print actions settings file "+jsonActionFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    static class BdvPgContextMenuEditor extends JPanel {
        private final JTextArea contextMenuActions;
        public BdvPgContextMenuEditor(String[] initialState) {
            this.setLayout(new GridLayout(0,2));
            String[] allActionKeys =
                    new ArrayList<>(SourceAndConverterServices
                            .getSourceAndConverterService()
                            .getActionsKeys()).toArray(new String[0]);

            JList<String> allActions = new JList<>(allActionKeys);

            contextMenuActions = new JTextArea();
            contextMenuActions.setFont(contextMenuActions.getFont().deriveFont(10f));
            allActions.setDragEnabled(true);
            contextMenuActions.setDragEnabled(true);
            contextMenuActions.setDropMode(DropMode.INSERT);
            StringBuilder sb = new StringBuilder();
            for (String action:initialState) {
                sb.append(action+"\n");
            }
            contextMenuActions.setText(sb.toString());

            this.add(new JScrollPane(contextMenuActions), BorderLayout.WEST);
            this.add(new JScrollPane(allActions), BorderLayout.EAST);
        }

        String[] getActions() {
            String actions = contextMenuActions.getText();
            actions+="\n"; // avoids ignoring last line
            return actions.split("\n");
        }
    }

}
