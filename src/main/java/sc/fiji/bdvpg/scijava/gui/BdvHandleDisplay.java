package sc.fiji.bdvpg.scijava.gui;

import bdv.util.BdvHandle;
import org.scijava.display.AbstractDisplay;
import org.scijava.display.Display;
import org.scijava.plugin.Plugin;

@Plugin(type = Display.class)
public class BdvHandleDisplay extends AbstractDisplay<BdvHandle> {
    public BdvHandleDisplay() {
        super(BdvHandle.class);
    }
}