package sc.fiji.bdvpg.scijava.command.bdv;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.config.BdvSettingsGUISetter;
import sc.fiji.bdvpg.scijava.ScijavaBdvDefaults;

@Plugin(type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"BDV>BDV Preferences - Set (Key) Bindings",
        label = "Set actions linked to key / mouse event in BDV")
public class BdvSettingsCommand implements Command {

    @Override
    public void run() {
        String yamlLocation = BdvSettingsGUISetter.defaultBdvPgSettingsRootPath;
        new BdvSettingsGUISetter(yamlLocation).run();
    }

}
