package sc.fiji.bdvpg.scijava.gui;


import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.display.AbstractDisplay;
import org.scijava.display.Display;
import org.scijava.plugin.Plugin;

@Plugin(type = Display.class)
public class AbstractSpimDataDisplay extends AbstractDisplay<AbstractSpimData> {
    public AbstractSpimDataDisplay() {
        super(AbstractSpimData.class);
    }
}
