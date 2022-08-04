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
package sc.fiji.bdvpg.bdv.config;

import bdv.TransformEventHandler2D;
import bdv.TransformEventHandler3D;
import bdv.TransformState;
import bdv.ui.settings.ModificationListener;
import bdv.ui.settings.SettingsPage;
import bdv.ui.settings.SettingsPanel;
import org.apache.commons.io.FileUtils;
import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerConfigHelper;
import org.scijava.ui.behaviour.io.InputTriggerDescriptionsBuilder;
import org.scijava.ui.behaviour.io.gui.VisualEditorPanel;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import org.scijava.ui.behaviour.util.Behaviours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * BigDataViewer Playground Action --
 *
 * Opens a GUI which helps defining bindings i.e.
 * what a user input (key, mouse, TODO DnD, TODO key bindings (only behaviour bindings work at the moment))
 * triggers as an action
 * Uses {@link VisualEditorPanel} from scijava.ui-behaviour, which is part of Mastodon for now
 *
 * And BDV Settings
 *
 * Needs a linked yaml file
 *
 * The settings are stored as files following this hierarchy:
 * bigdataviewer.properties
 * plugins/bdvpgsettings/
 *  bdvkeyconfig.yaml (behaviour bindings)
 *  bdvpg.editor.actions.json
 *  bdvpg.tree.actions.json // only one
 *  context1/
 *    bdvkeyconfig.yaml (behaviour bindings)
 *    bdvpg.editor.actions.json
 *    context1a/
 *      bdvkeyconfig.yaml (behaviour bindings)
 *      bdvpg.editor.actions.json
 *    context1b/
 *      bdvkeyconfig.yaml (behaviour bindings)
 *      bdvpg.editor.actions.json
 *  context2/
 *    bdvkeyconfig.yaml (behaviour bindings)
 *    bdvpg.editor.actions.json
 *
 * TODO : have a look at https://github.com/bigdataviewer/bigdataviewer-vistools/blob/master/src/main/java/bdv/util/BehaviourTransformEventHandlerPlanar.java
 * work in progress
 */

public class BdvSettingsGUISetter implements Runnable {

    protected static Logger logger = LoggerFactory.getLogger(BdvSettingsGUISetter.class);

    final String rootPath;

    public final static String bdvKeyConfigFileName = "bdvkeyconfig.yaml";
    public final static String editorActionsFileName = "bdvpg.editor.actions.txt";
    public final static String defaultEditorActionsFileName = "bdvpg.editor.actions.txt.default.txt";
    public final static String treeActionsFileName = "bdvpg.tree.actions.txt";
    public final static String defaultTreeActionsFileName = "bdvpg.tree.actions.txt.default.txt";
    public final static String defaultBdvPgSettingsRootPath = "plugins"+File.separator+"bdvpgsettings";

    public BdvSettingsGUISetter(String path) {
        this.rootPath = path;
    }

    @Override
    public void run() {

        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        } catch (Exception e) {
            e.printStackTrace();
        }

        final SettingsPanel settings = new SettingsPanel();

        // ---- BdvPrefs
        final BdvPrefsSettingsPage bdvPrefsEditor = new BdvPrefsSettingsPage( "bdv prefs" );
        settings.addPage( bdvPrefsEditor );

        // --- BDV Playground settings
        String pathDirDefaultSettings = rootPath+File.separator;
        File dirDefaultSettings = new File(pathDirDefaultSettings);

        if (!dirDefaultSettings.exists()) {
            boolean bool = dirDefaultSettings.mkdir();
            if(bool){
                logger.info("BDV Playground Directory for default settings created successfully");
            } else{
                logger.warn("Sorry couldn’t create BDV Playground Directory ("+pathDirDefaultSettings+")for settings storage");
                return;
            }
        }

        String pathDefaultYaml = dirDefaultSettings.getAbsolutePath()+File.separator+ bdvKeyConfigFileName;
        File defaultKeyConfig = new File(pathDefaultYaml);

        if (!defaultKeyConfig.exists()) {
            // ---- Default 2D Bindings "transform"
            InputTriggerConfig itc_default_2D = new InputTriggerConfig();
            TransformEventHandler2D teh2d = new TransformEventHandler2D(TransformState.from((at3d) ->{}, (at3d) ->{}));
            Behaviours bt2d = new Behaviours(itc_default_2D, "DEFAULT_2D_NAVIGATION");
            teh2d.install(bt2d);

            // ---- Default 3D Bindings "transform"
            InputTriggerConfig itc_default_3D = new InputTriggerConfig();
            TransformEventHandler3D teh3d = new TransformEventHandler3D(TransformState.from((at3d) ->{}, (at3d) ->{}));
            Behaviours bt3d = new Behaviours(itc_default_3D, "DEFAULT_2D_NAVIGATION");
            teh3d.install(bt3d);
            // ---- Bdv Playground specific bindings

            InputTriggerConfig itc_bdvpg = new InputTriggerConfig();
            //String actionScreenshotName = SourceAndConverterService.getCommandName(ScreenShotMakerCommand.class);
            String actionContextMenu = "Sources Context Menu";

            itc_bdvpg.add("not mapped", actionContextMenu, "bdvpg"); // default bindings
            //itc_bdvpg.add("not mapped", actionScreenshotName, "bdvpg"); // default bindings

            // Create default key bindings
            // Initialise it with the default transforms bindings for 2d and 3d transformatino handlers
            InputTriggerDescriptionsBuilder builder = new InputTriggerDescriptionsBuilder();
            builder.addMap(InputTriggerConfigHelper.getInputTriggerMap(itc_default_2D),"transform_bdv_2D");
            builder.addMap(InputTriggerConfigHelper.getInputTriggerMap(itc_default_3D),"transform_bdv_3D");
            builder.addMap(InputTriggerConfigHelper.getInputTriggerMap(itc_bdvpg),"bdvpg");
            builder.addMap(InputTriggerConfigHelper.getInputTriggerMap(itc_bdvpg),"transform_bdv_2D");
            builder.addMap(InputTriggerConfigHelper.getInputTriggerMap(itc_bdvpg),"transform_bdv_3D");

            try {
                YamlConfigIO.write(builder.getDescriptions(), pathDefaultYaml);
                logger.info("Default settings file successfully created: "+pathDefaultYaml);
            } catch (IOException e) {
                logger.warn("Couldn't write default key bindings file : "+pathDefaultYaml);
                e.printStackTrace();
                return;
            }
        }

        // Now looks recursively inside the file hierarchy to load different contexts

        recursivelySearchAndAppend("bdvpg", settings, pathDirDefaultSettings);

        // ----------------------- TODO the key bindings...

        // Is there a sourceandconverter context menu file ?
        String pathDefaultContextMenuSettings = dirDefaultSettings.getAbsolutePath()+File.separator+ treeActionsFileName;
        File treeActionsConfigFile = new File(pathDefaultContextMenuSettings);
        if (treeActionsConfigFile.exists()) {
            logger.debug("Actions tree config file already exists.");
        } else {
            logger.debug("Actions tree config file not present. Duplicate default config file");
            String defaultFile = dirDefaultSettings.getAbsolutePath()+File.separator+ defaultTreeActionsFileName;
            if (new File(defaultFile).exists()) {
                try {
                    FileUtils.copyFile(new File(defaultFile), new File(pathDefaultContextMenuSettings));
                } catch (IOException e) {
                    logger.error("Error : couldn't duplicate bdvpg default config file");
                    e.printStackTrace();
                }
            } else {
                logger.warn("Default tree actions config file for bigdataviewer-playground not present!");
            }
        }

        if (treeActionsConfigFile.exists()) {
            SettingsPage spTreeActions = new BdvPlaygroundContextualMenuSettingsPage("tree actions", treeActionsConfigFile);
            settings.addPage(spTreeActions);
        }

        final JDialog dialog = new JDialog( (Frame) null, "BDV Playground Settings" );
        dialog.getContentPane().add( settings, BorderLayout.CENTER );
        dialog.pack();
        dialog.setVisible( true );
    }

    static public File getActionFile(String path, String context) {
        if (!path.endsWith(File.separator)) {path+=File.separator;}
        return new File(defaultBdvPgSettingsRootPath+File.separator+path+"bdvpg."+context+".actions.txt");
    }

    private void recursivelySearchAndAppend(String subPath, SettingsPanel settings, String pathDir) {
        File currentDir = new File(pathDir);
        assert currentDir.exists();
        assert currentDir.isDirectory();

        // Is there a key config file ?
        String pathYamlFile = pathDir+File.separator+ bdvKeyConfigFileName;
        File keyConfig = new File(pathYamlFile);

        if (keyConfig.exists()) {
            try {
                InputTriggerConfig yamlConf = new InputTriggerConfig( YamlConfigIO.read( pathYamlFile ) );
                final VisualEditorPanel yaml_keyconfEditor = new VisualEditorPanel(yamlConf);
                yaml_keyconfEditor.setButtonPanelVisible(false);
                settings.addPage(new DefaultSettingsPage(subPath+"> settings", yaml_keyconfEditor));
                yaml_keyconfEditor.modelChangedListeners().add( () -> {
                    yaml_keyconfEditor.modelToConfig();
                    try {
                        YamlConfigIO.write(new InputTriggerDescriptionsBuilder(yamlConf).getDescriptions(), pathDir+File.separator+ bdvKeyConfigFileName);
                    } catch (Exception e) {
                        logger.error("Could not create yaml file : settings will not be saved.");
                    }
                });
            } catch (IOException e) {
                logger.error("Couldn't read default key bindings file : "+pathYamlFile);
                e.printStackTrace();
            }
        }

        // Is there an editor config file ?
        String pathEditorFile = pathDir+File.separator + editorActionsFileName;
        File editorConfig = new File(pathEditorFile);

        if (!editorConfig.exists()) {
            String pathDefaultEditorFile = pathDir+File.separator + editorActionsFileName + ".default.txt";
            File editorDefaultConfig = new File(pathDefaultEditorFile);
            if (editorDefaultConfig.exists()) {
                try {
                    FileUtils.copyFile(editorDefaultConfig, editorConfig);
                } catch (IOException e) {
                    logger.error("Error : couldn't duplicate bdvpg default config file");
                    e.printStackTrace();
                }
            }
        }

        if (editorConfig.exists()) {
            settings.addPage(new BdvPlaygroundContextualMenuSettingsPage(subPath+"> editor", editorConfig));
        }

        // ----------------------- TODO the key bindings...

        // Are there subfolders ?
        try (Stream<Path> walk = Files.walk(Paths.get(pathDir))) {
            walk.filter(Files::isDirectory)
                    .map(Path::toString)
                    .filter(folderPath -> !(new File(folderPath).equals(new File(pathDir))))
                    .forEach(folderPath -> recursivelySearchAndAppend(subPath+">"+new File(folderPath).getName(), settings, folderPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static class DefaultSettingsPage implements SettingsPage
    {
        private final String treePath;

        private final JPanel panel;

        private final Listeners.List<ModificationListener> modificationListeners;

        public DefaultSettingsPage( final String treePath, final JPanel panel )
        {
            this.treePath = treePath;
            this.panel = panel;
            modificationListeners = new Listeners.SynchronizedList<>();
        }

        @Override
        public String getTreePath()
        {
            return treePath;
        }

        @Override
        public JPanel getJPanel()
        {
            return panel;
        }

        @Override
        public Listeners< ModificationListener > modificationListeners()
        {
            return modificationListeners;
        }

        @Override
        public void cancel()
        {
            //System.out.println( "DefaultSettingsPage.cancel" );
        }

        @Override
        public void apply()
        {
            //System.out.println( "DefaultSettingsPage.apply" );
        }
    }
}
