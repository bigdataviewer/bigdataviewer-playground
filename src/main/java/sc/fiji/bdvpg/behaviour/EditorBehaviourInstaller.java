package sc.fiji.bdvpg.behaviour;

import bdv.ui.SourcesTransferable;
import bdv.util.BdvHandle;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import ch.epfl.biop.bdv.select.ToggleListener;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesAdderCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.command.source.*;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;

import javax.swing.*;
import java.awt.event.MouseEvent;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.getCommandName;

public class EditorBehaviourInstaller implements Runnable {

    final SourceSelectorBehaviour ssb;
    BdvHandle bdvh;

    String[] editorPopupActions = {
                getCommandName(BasicTransformerCommand.class),
                getCommandName(BdvSourcesRemoverCommand.class),
                "Inspect Sources",
                "PopupLine",
                getCommandName(SourcesInvisibleMakerCommand.class),
                getCommandName(BrightnessAdjusterCommand.class),
                getCommandName(SourceColorChangerCommand.class),
                getCommandName(SourceAndConverterProjectionModeChangerCommand.class),
                "PopupLine",
                getCommandName(SourcesRemoverCommand.class)
    };

    public EditorBehaviourInstaller(SourceSelectorBehaviour ssb) {
        this.ssb = ssb;
        this.bdvh = ssb.getBdvHandle();
    }

    @Override
    public void run() {
        Behaviours editor = new Behaviours(new InputTriggerConfig());

        ClickBehaviour delete = (x, y) -> bdvh.getViewerPanel().state().removeSources(ssb.getSelectedSources());

        editor.behaviour(delete, "remove-sources-from-bdv", new String[]{"DELETE"});

        // Custom Drag support
        if (bdvh.getViewerPanel().getTransferHandler() instanceof BdvTransferHandler) {
            System.out.println("Dragging support enabled");
            BdvTransferHandler handler = (BdvTransferHandler) bdvh.getViewerPanel().getTransferHandler();
            handler.setTransferableFunction(c -> new SourcesTransferable(ssb.getSelectedSources()));
            editor.behaviour(new DragNDSourcesBehaviour(), "drag-selected-sources", new String[]{"alt button1"});
        }

        editor.behaviour(new SourceAndConverterContextMenuClickBehaviour( bdvh, ssb::getSelectedSources, editorPopupActions ), "Sources Context Menu", "button3");

        // One way to chain the behaviour : install and uninstall on source selector toggling:
        // The delete key will act only when the source selection mode is on
        ssb.addToggleListener(new ToggleListener() {

            @Override
            public void isEnabled() {
                bdvh.getViewerPanel().showMessage("Editor Mode");
                //bdvh.getViewerPanel().showMessage(ssb.getSelectedSources().size()+" sources selected");
                // Enable the editor behaviours when the selector is enabled
                editor.install(bdvh.getTriggerbindings(), "sources-editor");
            }

            @Override
            public void isDisabled() {
                bdvh.getViewerPanel().showMessage("Navigation Mode");
                // Disable the editor behaviours the selector is disabled
                bdvh.getTriggerbindings().removeInputTriggerMap("sources-editor");
                bdvh.getTriggerbindings().removeBehaviourMap("sources-editor");
            }
        });
    }

    class DragNDSourcesBehaviour implements DragBehaviour {

        @Override
        public void init(int x, int y) {
            bdvh.getViewerPanel().getTransferHandler().exportAsDrag(bdvh.getViewerPanel(), new MouseEvent(bdvh.getViewerPanel(), 0, 0, 0, 100, 100, 1, false), TransferHandler.MOVE);
        }

        @Override
        public void drag(int x, int y) {

        }

        @Override
        public void end(int x, int y) {

        }
    }
}
