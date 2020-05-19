package bdv.util;

import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.ManualTransformationEditor;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import org.scijava.ui.behaviour.util.InputActionBindings;
import org.scijava.ui.behaviour.util.TriggerBehaviourBindings;

import java.util.List;
import java.util.function.Consumer;

/**
 * Wraps a {@link bdv.BigDataViewer} instance into a {@link BdvHandle}
 * This class NEEDS to be in bdv.util or else it cannot implement the createViewer method
 *
 * Class used in practive to wrap {@link bigwarp.BigWarp} BigDataViewer instances,
 * this has very limited functionalities apart from this
 */

public class ViewerPanelHandle extends BdvHandle {

    Consumer<String> errlog = s -> System.err.println(this.getClass()+" error: "+s);

    public String name;

    public ViewerPanelHandle(ViewerPanel viewerPanel, SetupAssignments sa, String name) {
        super(BdvOptions.options());
        this.viewer = viewerPanel;
        this.name = name;
        this.setupAssignments = sa;
    }

    @Override
    public void close() {
        // TODO : implement this ?
    }

    @Override
    public ManualTransformationEditor getManualTransformEditor() {
        errlog.accept("Unsupported getManualTransformEditor call in ViewerPanel wrapped BdvHandle");
        return null;
    }

    @Override
    public InputActionBindings getKeybindings() {
        errlog.accept("Unsupported getKeybindings call in ViewerPanel wrapped BdvHandle");
        return null;
    }

    @Override
    public TriggerBehaviourBindings getTriggerbindings() {
        System.err.println("Unsupported getTriggerbindings call in ViewerPanel wrapped BdvHandle");
        return null;
    }

    boolean createViewer(
            List< ? extends ConverterSetup> converterSetups,
            List< ? extends SourceAndConverter< ? >> sources,
            int numTimepoints ) {
        errlog.accept("Cannot add sources in ViewerPanel wrapped BdvHandle BdvHandle");
        return false;
    }


    @Override
    public String toString() {
        return name;
    }

}