package sc.fiji.bdvpg.bdv.config;

//import bdv.BehaviourTransformEventHandler3D;
//import bdv.util.BehaviourTransformEventHandlerPlanar;
import org.mastodon.app.ui.settings.ModificationListener;
import org.mastodon.app.ui.settings.SettingsPage;
import org.mastodon.app.ui.settings.SettingsPanel;
import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerConfigHelper;
import org.scijava.ui.behaviour.io.InputTriggerDescriptionsBuilder;
import org.scijava.ui.behaviour.io.gui.VisualEditorPanel;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;
import sc.fiji.bdvpg.scijava.command.bdv.ScreenShotMakerCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * BigDataViewer Playground Action -->
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
 * bdvpgsettings/
 *  bdvkeyconfig.yaml (behaviour bindings)
 *  contextmenubdv.json
 *  contextmenutree.json // only one
 *  context1/
 *    bdvkeyconfig.yaml (behaviour bindings)
 *    contextmenubdv.json
 *    context1a/
 *      bdvkeyconfig.yaml (behaviour bindings)
 *      contextmenubdv.json
 *    context1b/
 *      bdvkeyconfig.yaml (behaviour bindings)
 *      contextmenubdv.json
 *  context2/
 *    bdvkeyconfig.yaml (behaviour bindings)
 *    contextmenubdv.json
 *
 * TODO : have a look at https://github.com/bigdataviewer/bigdataviewer-vistools/blob/master/src/main/java/bdv/util/BehaviourTransformEventHandlerPlanar.java
 * work in progress
 */

public class BdvSettingsGUISetter implements Runnable {

    final String rootPath;

    public final static String defaultYamlFileName = "bdvkeyconfig.yaml";
    public final static String defaultContextMenuFileName = "contextmenu.txt";

    public final static String defaultBdvPgSettingsRootPath = "bdvpgsettings";

    public BdvSettingsGUISetter(String yamlDataLocation) {
        this.rootPath = yamlDataLocation;
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
                System.out.println("BDV Playground Directory for default settings created successfully");
            } else{
                System.err.println("Sorry couldn’t create BDV Playground Directory ("+pathDirDefaultSettings+")for settings storage");
                return;
            }
        }

        String pathDefaultYaml = dirDefaultSettings.getAbsolutePath()+File.separator+defaultYamlFileName;
        File defaultKeyConfig = new File(pathDefaultYaml);

        if (!defaultKeyConfig.exists()) {
            // ---- Default 2D Bindings "transform"
            InputTriggerConfig itc_default_2D = new InputTriggerConfig();
            //new BehaviourTransformEventHandlerPlanar(null, itc_default_2D);
            // ---- Default 3D Bindings "transform"
            InputTriggerConfig itc_default_3D = new InputTriggerConfig();
            //new BehaviourTransformEventHandler3D(null, itc_default_3D);

            // ---- Bdv Playground specific bindings

            InputTriggerConfig itc_bdvpg = new InputTriggerConfig();
            String actionScreenshotName = SourceAndConverterService.getCommandName(ScreenShotMakerCommand.class);
            String actionContextMenu = "Sources Context Menu";

            itc_bdvpg.add("not mapped", actionContextMenu, "bdvpg"); // default bindings
            itc_bdvpg.add("not mapped", actionScreenshotName, "bdvpg"); // default bindings

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
                System.out.println("Default settings file successfully created");
            } catch (IOException e) {
                System.err.println("Couldn't write default key bindings file : "+pathDefaultYaml);
                e.printStackTrace();
                return;
            }
        }

        // Now looks recursively inside the file hierarchy to load different contexts

        recursivelySearchAndAppend("bdvpg", settings, pathDirDefaultSettings);


        // ----------------------- TODO the key bindings...



        // Is there a sourceandconverter context menu file ?
        String pathDefaultContextMenuSettings = dirDefaultSettings.getAbsolutePath()+File.separator+defaultContextMenuFileName;
        File defaultContextMenuConfig = new File(pathDefaultContextMenuSettings);
        if (defaultContextMenuConfig.exists()) {
            // TODO
        }

        final JDialog dialog = new JDialog( (Frame) null, "BDV Playground Settings" );
        dialog.getContentPane().add( settings, BorderLayout.CENTER );
        dialog.pack();
        dialog.setVisible( true );

    }

    private void recursivelySearchAndAppend(String subPath, SettingsPanel settings, String pathDir) {
        File currentDir = new File(pathDir);
        assert currentDir.exists();
        assert currentDir.isDirectory();

        // Is there a key config file ?
        String pathYamlFile = pathDir+File.separator+defaultYamlFileName;
        File keyConfig = new File(pathYamlFile);

        if (keyConfig.exists()) {
            try {
                InputTriggerConfig yamlConf = new InputTriggerConfig( YamlConfigIO.read( pathYamlFile ) );
                final VisualEditorPanel yaml_keyconfEditor = new VisualEditorPanel(yamlConf);
                yaml_keyconfEditor.setButtonPanelVisible(false);
                settings.addPage(new DefaultSettingsPage(subPath+"> settings", yaml_keyconfEditor));
                yaml_keyconfEditor.addConfigChangeListener( () -> {
                    yaml_keyconfEditor.modelToConfig();
                    try {
                        YamlConfigIO.write(new InputTriggerDescriptionsBuilder(yamlConf).getDescriptions(), pathDir+File.separator+defaultYamlFileName);
                    } catch (Exception e) {
                        System.err.println("Could not create yaml file : settings will not be saved.");
                    }
                });
            } catch (IOException e) {
                System.err.println("Couldn't read default key bindings file : "+pathYamlFile);
                e.printStackTrace();
            }
        }

        // ----------------------- TODO the key bindings...

        // Are there subfolders ?
        try (Stream<Path> walk = Files.walk(Paths.get(pathDir))) {
            walk.filter(Files::isDirectory)
                    .map(x -> x.toString())
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

        private final Listeners.List< ModificationListener > modificationListeners;

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
