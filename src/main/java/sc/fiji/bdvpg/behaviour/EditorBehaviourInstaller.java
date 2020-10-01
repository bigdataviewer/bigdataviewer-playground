package sc.fiji.bdvpg.behaviour;

import bdv.util.BdvHandle;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import ch.epfl.biop.bdv.select.ToggleListener;
import com.google.gson.Gson;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesRemoverCommand;
import sc.fiji.bdvpg.scijava.command.bdv.BdvSourcesShowCommand;
import sc.fiji.bdvpg.scijava.command.source.*;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.getCommandName;

/**
 * BDV Actions called by default on each BDV Window being created
 * See {@link EditorBehaviourUnInstaller} to remove the default editor and replace by a custom if necessary
 *
 */

public class EditorBehaviourInstaller implements Runnable {

    final SourceSelectorBehaviour ssb;
    BdvHandle bdvh;

    private ToggleListener toggleListener;

    public static String[] getEditorPopupActions() {
        File f = new File("Contents"+File.separator+"bdvpgsettings"+File.separator+"DefaultEditorActions.json");
        String[] editorPopupActions = {
                getCommandName(BdvSourcesShowCommand.class),
                getCommandName(BasicTransformerCommand.class),
                getCommandName(BdvSourcesRemoverCommand.class),
                "Inspect Sources",
                "PopupLine",
                getCommandName(SourcesInvisibleMakerCommand.class),
                getCommandName(BrightnessAdjusterCommand.class),
                getCommandName(SourceColorChangerCommand.class),
                getCommandName(SourceAndConverterProjectionModeChangerCommand.class),
                "PopupLine",
                getCommandName(SourcesRemoverCommand.class),
                "PopupLine"};

        Gson gson = new Gson();
        if (f.exists()) {
            try {
                editorPopupActions = gson.fromJson(new FileReader(f.getAbsoluteFile()), String[].class);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return editorPopupActions;
    }


    public EditorBehaviourInstaller(SourceSelectorBehaviour ssb) {
        this.ssb = ssb;
        this.bdvh = ssb.getBdvHandle();
    }

    @Override
    public void run() {
        Behaviours editor = new Behaviours(new InputTriggerConfig());

        ClickBehaviour delete = (x, y) -> bdvh.getViewerPanel().state().removeSources(ssb.getSelectedSources());

        editor.behaviour(delete, "remove-sources-from-bdv", new String[]{"DELETE"});

        editor.behaviour(new SourceAndConverterContextMenuClickBehaviour( bdvh, ssb::getSelectedSources, getEditorPopupActions() ), "Sources Context Menu", "button3");

        toggleListener = new ToggleListener() {
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
        };

        // One way to chain the behaviour : install and uninstall on source selector toggling:
        // The delete key will act only when the source selection mode is on
        ssb.addToggleListener(toggleListener);

        // Provides a way to retrieve this installer -> can be used to deinstalling it {@link EditorBehaviourUninstaller}
        SourceAndConverterServices.getSourceAndConverterDisplayService().setDisplayMetadata(
                bdvh, EditorBehaviourInstaller.class.getSimpleName(), this);

    }

    public ToggleListener getToggleListener() {
        return toggleListener;
    }

}
