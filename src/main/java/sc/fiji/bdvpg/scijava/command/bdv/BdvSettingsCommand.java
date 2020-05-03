package sc.fiji.bdvpg.scijava.command.bdv;

import bdv.util.Prefs;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.config.BdvSettingsGUISetter;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

import java.util.Properties;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Bdv>Set Bindings",
        label = "Set actions linked to key / mouse event")
public class BdvSettingsCommand implements Command {


    @Override
    public void run() {
        String yamlLocation = "bdvkeyconfig.yaml";
        new BdvSettingsGUISetter(yamlLocation, "Behaviour Key bindings editor").run();
    }

}
