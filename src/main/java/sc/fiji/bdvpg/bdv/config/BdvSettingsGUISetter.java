package sc.fiji.bdvpg.bdv.config;

import org.mastodon.app.ui.settings.ModificationListener;
import org.mastodon.app.ui.settings.SettingsPage;
import org.mastodon.app.ui.settings.SettingsPanel;
import org.scijava.listeners.Listeners;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.scijava.ui.behaviour.io.gui.Command;
import org.scijava.ui.behaviour.io.gui.CommandDescriptionBuilder;
import org.scijava.ui.behaviour.io.gui.VisualEditorPanel;
import org.scijava.ui.behaviour.io.yaml.YamlConfigIO;

import javax.swing.*;
import java.awt.*;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
 * Action opening a GUI which helps defining bindings i.e.
 * what a user input (key, mouse, TODO DnD)
 * triggers as an action
 * Uses VisualEditorPanel from scijava.ui-behaviour
 *
 * And Bdv Settings
 *
 * Needs a linked yaml file
 */

public class BdvSettingsGUISetter implements Runnable {

    String yamlDataLocation;
    String editorWindowName = "Behaviour Key bindings editor";

    public BdvSettingsGUISetter(String yamlDataLocation, String editorWindowName) {
        this.yamlDataLocation = yamlDataLocation;
        this.editorWindowName = editorWindowName;
    }

    private static InputTriggerConfig getDemoConfig()
    {
        final StringReader reader = new StringReader( "---\n" +
                "- !mapping" + "\n" +
                "  action: fluke" + "\n" +
                "  contexts: [all]" + "\n" +
                "  triggers: [F]" + "\n" +
                "- !mapping" + "\n" +
                "  action: drag1" + "\n" +
                "  contexts: [all]" + "\n" +
                "  triggers: [button1, win G]" + "\n" +
                "- !mapping" + "\n" +
                "  action: scroll1" + "\n" +
                "  contexts: [all]" + "\n" +
                "  triggers: [scroll]" + "\n" +
                "- !mapping" + "\n" +
                "  action: scroll1" + "\n" +
                "  contexts: [trackscheme, mamut]" + "\n" +
                "  triggers: [shift D]" + "\n" +
                "- !mapping" + "\n" +
                "  action: destroy the world" + "\n" +
                "  contexts: [mamut]" + "\n" +
                "  triggers: [control A]" + "\n" +
                "" );
        final List<InputTriggerDescription> triggers = YamlConfigIO.read( reader );
        final InputTriggerConfig config = new InputTriggerConfig( triggers );
        return config;
    }

    private static Map<Command, String > getDemoCommands()
    {
        return new CommandDescriptionBuilder()
                .addCommand( "drag1", "mamut", "Move an item around the editor." )
                .addCommand( "drag1", "trackscheme", "Move an item around the editor." )
                .addCommand( "drag1", "other", "Move an item around the editor." )
                .addCommand( "Elude", "other", "Refuse to answer the question." )
                .addCommand( "scroll1", "mamut", null )
                .addCommand( "destroy the world", "all", "Make a disgusting coffee for breakfast. \n"
                        + "For this one, you are by yourself. Good luck and know that we are with you. This is a long line. Hopefully long engouh.\n"
                        + "Hey, what about we add:\n"
                        + "tabulation1\ttabulation2\n"
                        + "lalallala\ttrollololo." )
                .addCommand( "ride the dragon", "all", "Go to work by bike." )
                .addCommand( "Punish", "all", "Go to work by parisian metro." )
                .addCommand( "make some coffee", "mamut", null )
                .addCommand( "make some coffee", "trackscheme", "Make a decent coffee." )
                .get();
    }

    @Override
    public void run() {
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

            InputTriggerConfig conf = new InputTriggerConfig( YamlConfigIO.read( yamlDataLocation ) );

            /*final JFrame frame = new JFrame( "Behaviour Key bindings editor" );
            final VisualEditorPanel editorPanel = new VisualEditorPanel( conf ); //getDemoConfig(), getDemoCommands() );
            editorPanel.addConfigChangeListener( () -> System.out.println( "Config changed @ " + new java.util.Date().toString() ) );
            //SwingUtilities.updateComponentTreeUI( VisualEditorPanel.fileChooser );
            frame.getContentPane().add( editorPanel );
            frame.pack();
            frame.setVisible( true );*/

            final SettingsPanel settings = new SettingsPanel();
            final VisualEditorPanel keyconfEditor = new VisualEditorPanel( conf );
            keyconfEditor.setButtonPanelVisible( false );
            settings.addPage( new DefaultSettingsPage( "keymap", keyconfEditor ) );
            final BdvPrefsSettingsPage bdvPrefsEditor = new BdvPrefsSettingsPage( "bdv prefs" );
            settings.addPage( bdvPrefsEditor );

            //settings.addPage( new DummySettingsPage( "other" ) );
            //settings.addPage( new DummySettingsPage( "views > bdv" ) );
            settings.addPage( new DummySettingsPage( "views > trackscheme" ) );

            final JDialog dialog = new JDialog( (Frame) null, "Settings" );
            dialog.getContentPane().add( settings, BorderLayout.CENTER );
            dialog.pack();
            dialog.setVisible( true );

        } catch (final Exception e) {
            e.printStackTrace();
        }

    }

    static class DummySettingsPage implements SettingsPage
    {
        private final String treePath;

        private final JPanel panel;

        private final Listeners.List<ModificationListener> modificationListeners;

        public DummySettingsPage( final String treePath )
        {
            this.treePath = treePath;
            panel = new JPanel( new BorderLayout() );
            modificationListeners = new Listeners.SynchronizedList<>();

            final JButton button = new JButton( treePath );
            button.setEnabled( false );
            panel.add( button, BorderLayout.CENTER );
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
        }

        @Override
        public void apply()
        {
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
            System.out.println( "DefaultSettingsPage.cancel" );
        }

        @Override
        public void apply()
        {
            System.out.println( "DefaultSettingsPage.apply" );
        }
    }
}
