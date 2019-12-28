package sc.fiji.bdvpg.scijava.services;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.SciJavaService;
import org.scijava.service.Service;
import org.scijava.ui.UIService;
import sc.fiji.bdvpg.scijava.gui.ARGBColorConverterSetup;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

@Plugin(type=Service.class)
public class BdvSourceService extends AbstractService implements SciJavaService {

    @Parameter
    private ObjectService objectService;

    /**
     * Map containing objects that are 1 to 1 linked to a Source
     */
    Map<Source, Map<String, Object>> data;

    final public static String  SPIMDATALIST = "SPIMDATALIST";

    @Parameter
    ScriptService scriptService;

    @Parameter
    UIService uiService;

    @Parameter
    EventService es;

    public static Consumer<String> log = (str) -> System.out.println(BdvSourceService.class.getSimpleName()+":"+str);

    public static Consumer<String> errlog = (str) -> System.err.println(BdvSourceService.class.getSimpleName()+":"+str);

    public boolean isRegistered(Source src) {
        return data.containsKey(src);
    }

    public void registerSource(Source src) {
        if (data.containsKey(src)) {
            log.accept("Source already registered");
            return;
        }

        // Essentially copy What's in BdvVisTools
        final int numTimepoints = 1;


        if ( src.getType() instanceof RealType ) {
            registerRealTypeSource(src);//addSourceToListsRealType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
		/*else if ( type instanceof ARGBType )
			addSourceToListsARGBType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
		else if ( type instanceof VolatileARGBType )
			addSourceToListsVolatileARGBType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );*/
        } else {
            errlog.accept("Cannot register Source of type "+src.getType().getClass().getSimpleName());
            return;
        }

        objectService.addObject(src);

        if (uiAvailable) ui.update(src);

    }

    public < T extends RealType< T >> void registerRealTypeSource(Source<T> source) {
        log.accept("Real Typed Source Registration...");
        final T type = Util.getTypeFromInterval( source.getSource( 0, 0 ) );
        final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
        final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
        final RealARGBColorConverter< T > converter ;
        if ( source.getType() instanceof Volatile)
            converter = new RealARGBColorConverter.Imp0<>( typeMin, typeMax );
        else
            converter = new RealARGBColorConverter.Imp1<>( typeMin, typeMax );
        converter.setColor( new ARGBType( 0xffffffff ) );

        final ARGBColorConverterSetup setup = new ARGBColorConverterSetup( converter );

        Map<String, Object> sourceData = new HashMap<>();
        //sourceData.put(CONVERTER, converter);
        //sourceData.put(CONVERTERSETUP, setup);
        data.put(source, sourceData);

        log.accept("Real Typed Source Registered");

    }

    UIBdvSourceService ui;

    boolean uiAvailable = false;

    public UIBdvSourceService getUI() {
        return ui;
    }

    @Override
    public void initialize() {
        scriptService.addAlias(Source.class);
        scriptService.addAlias(RealTransform.class);
        scriptService.addAlias(AffineTransform3D.class);
        scriptService.addAlias(AbstractSpimData.class);
        data = new HashMap<>();

        if (uiService!=null) {
            log.accept("uiService detected : Constructing JPanel for BdvSourceService");
            ui = new UIBdvSourceService(this);
            uiAvailable = true;
        }

        log.accept("Service initialized.");

        es.subscribe(this);
    }

    @EventHandler
    public void listen() {
        log.accept("listen method triggered");
    }

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

        /* public void addConverterSetupControl(Source src) {
            JSlider slider = new JSlider();
            slider.setMinimum(0);
            slider.setMaximum(65535);
            slider.addChangeListener(e -> {
                JSlider s = (JSlider) e.getSource();
                System.out.println(s.getValue());
            });
            panel.add(slider);
        } */
    }

}
