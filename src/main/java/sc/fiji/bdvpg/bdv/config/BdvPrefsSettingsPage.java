package sc.fiji.bdvpg.bdv.config;

import bdv.util.Prefs;
import org.mastodon.app.ui.settings.ModificationListener;
import org.mastodon.app.ui.settings.SettingsPage;
import org.scijava.listeners.Listeners;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class BdvPrefsSettingsPage implements SettingsPage {

    private final String treePath;

    private final JPanel panel;

    private final Listeners.List<ModificationListener> modificationListeners;

    public BdvPrefsSettingsPage( final String treePath )
    {
        this.treePath = treePath;
        panel = new BdvPrefsEditorPanel();//JPanel( new BorderLayout() );
        modificationListeners = new Listeners.SynchronizedList<>();

        final JButton button = new JButton( treePath );
        button.setEnabled( false );
        panel.add( button, BorderLayout.CENTER );
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

    @Override
    public void cancel() {
        // Do something ?
    }

    @Override
    public void apply() {
        // save to default place
    }

    class BdvPrefsEditorPanel extends JPanel {

        Prefs prefs;

        final Properties props;

        final JComponent[] components = {
                new JLabel("show-scale-bar"),
                new JLabel("show-multibox-overlay"),
                new JLabel("show-text-overlay"),
                new JLabel("show-scale-bar-in-movie"),
                new JLabel("scale-bar-color"),
                new JLabel("scale-bar-bg-color"),
                new JLabel(""),
                new JLabel(""),
                new JLabel(""),
                new JLabel(""),
                new JLabel(""),
                new JLabel("")};

        public BdvPrefsEditorPanel() {

            Prefs.scaleBarBgColor(78);

            props = Prefs.getDefaultProperties();

            this.setLayout(new GridLayout(components.length/2,2));

            for (Component c : components) {
                this.add(c);
            }

        }
    }
}
