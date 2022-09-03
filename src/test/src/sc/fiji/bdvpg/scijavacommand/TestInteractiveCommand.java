package sc.fiji.bdvpg.scijavacommand;

import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Test>Test Interactive Command")
public class TestInteractiveCommand extends InteractiveCommand {

    @Parameter
    String a_string;

    @Override
    public void run() {
        // nothing
    }

    public static void main(String... args) throws Exception {

        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ij.command().run(TestInteractiveCommand .class, true);

    }
}