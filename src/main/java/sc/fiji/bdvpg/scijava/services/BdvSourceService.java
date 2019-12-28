package sc.fiji.bdvpg.scijava.services;

import bdv.viewer.Source;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Scijava Service which centralizes Bdv Sources, independently of their display
 * Bdv Sources can be registered to this Service.
 * This service adds the Source to the ObjectService, but on top of it,
 * It contains a Map which contains any object which can be linked to the source.
 *
 * It also handles SpimData object, but split all Sources into individual ones
 *
 * The most interesting of these objects are actually created by the BdvSourceDisplayService:
 * - Converter to ARGBType, ConverterSetup, and Volatile view
 *
 * TODO : Think more carefully : maybe the volatile source should be done here...
 * Because when multiply wrapped source end up here, it maybe isn't possible to make the volatile view
 */

@Plugin(type=Service.class)
public class BdvSourceService extends AbstractService implements SciJavaService {

    /**
     * Standard logger
     */
    public static Consumer<String> log = (str) -> System.out.println(BdvSourceService.class.getSimpleName()+":"+str);

    /**
     * Error logger
     */
    public static Consumer<String> errlog = (str) -> System.err.println(BdvSourceService.class.getSimpleName()+":"+str);

    /**
     * Scijava Object Service : will contain all the sources
     */
    @Parameter
    private ObjectService objectService;

    /**
     * Scriptservice : used for adding Source alias to help for scripting
     */
    @Parameter
    ScriptService scriptService;

    /**
     * uiService : used ot check if an UI is available to create a Swing Panel
     */
    @Parameter
    UIService uiService;

    /**
     * Map containing objects that are 1 to 1 linked to a Source
     * TODO : ask if it should contain a WeakReference to Source keys (Potential Memory leak ?)
     */
    Map<Source, Map<String, Object>> data;

    /**
     * Reserved key for the data map. data.get(source).get(SPIMDATALIST)
     * is expected to return a List of Spimdata Objects which refer to this source
     * whether a list of necessary is not obvious at the moment
     * TODO : make an example
     */
    final public static String  SPIMDATALIST = "SPIMDATALIST";

    /**
     * Test if a Source is already registered in the Service
     * @param src
     * @return
     */
    public boolean isRegistered(Source src) {
        return data.containsKey(src);
    }

    /**
     * Register a Bdv Source in this Service.
     * Called in the BdvSourcePostProcessor
     * @param src
     */
    public void registerSource(Source src) {
        if (data.containsKey(src)) {
            log.accept("Source already registered");
            return;
        }
        final int numTimepoints = 1;
        Map<String, Object> sourceData = new HashMap<>();
        data.put(src, sourceData);
        objectService.addObject(src);
        if (uiAvailable) ui.update(src);

    }

    /**
     * Inner Swing UI for this Service, exists only if an UI is available in the current execution context
     */
    UIBdvSourceService ui;

    /**
     * Flags if the Inner UI exists
     */
    boolean uiAvailable = false;

    public UIBdvSourceService getUI() {
        return ui;
    }

    /**
     * Service initialization
     */
    @Override
    public void initialize() {
        scriptService.addAlias(Source.class);
        // -- TODO Check whether it's a good idea to add this here
        scriptService.addAlias(RealTransform.class);
        scriptService.addAlias(AffineTransform3D.class);
        scriptService.addAlias(AbstractSpimData.class);
        // -- End of to check$

        data = new HashMap<>();
        if (uiService!=null) {
            log.accept("uiService detected : Constructing JPanel for BdvSourceService");
            ui = new UIBdvSourceService(this);
            uiAvailable = true;
        }
        log.accept("Service initialized.");
    }

    /**
     * Inner Swing UI for Bdv Source
     * Really really basic at the moment
     * TODO : improve UI
     */
    public class UIBdvSourceService {

        BdvSourceService bss;
        JFrame frame;
        JPanel panel;
        JTextArea textArea;

        public UIBdvSourceService(BdvSourceService bss) {
                this.bss = bss;
                frame = new JFrame("Bdv Sources");
                panel = new JPanel();
                textArea = new JTextArea();
                textArea.setText("Bdv Sources will be displayed here.");
                panel.add(textArea);
                frame.add(panel);
                frame.pack();
                frame.setVisible(true);
        }

        Set<Source> displayedSource = new HashSet<>();

        public void update(Source src) {
            if (displayedSource.contains(src)) {
                // Need to update
            } else {
                String text = textArea.getText();
                text+= "\n"+src.getName();
                textArea.setText(text);
            }
        }
    }

}
