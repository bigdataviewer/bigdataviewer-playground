/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
 * @author Nicolas Chiaruttini, BIOP, EPFL 2020
 */

public class EditorBehaviourInstaller implements Runnable {

    final SourceSelectorBehaviour ssb;
    BdvHandle bdvh;

    private ToggleListener toggleListener;

    public static String[] getEditorPopupActions() {
        // TODO : fix the file path containing default actions
        File f = new File("bdvpgsettings"+File.separator+"DefaultEditorActions.json");
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

        editor.behaviour(delete, "remove-sources-from-bdv", "DELETE");

        editor.behaviour(new SourceAndConverterContextMenuClickBehaviour( bdvh, ssb::getSelectedSources, getEditorPopupActions() ), "Sources Context Menu", "button3");

        toggleListener = new ToggleListener() {
            @Override
            public void isEnabled() {
                bdvh.getViewerPanel().showMessage("Editor Mode");
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

        // Provides a way to retrieve this installer -> can be used to uninstalling it {@link EditorBehaviourUninstaller}
        SourceAndConverterServices.getSourceAndConverterDisplayService().setDisplayMetadata(
                bdvh, EditorBehaviourInstaller.class.getSimpleName(), this);

    }

    public ToggleListener getToggleListener() {
        return toggleListener;
    }

}
