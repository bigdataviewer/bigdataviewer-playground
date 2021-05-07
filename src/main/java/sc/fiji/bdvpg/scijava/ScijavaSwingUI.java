/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2021 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.scijava;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleRunner;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.PluginService;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.ui.swing.widget.SwingInputPanel;
import org.scijava.widget.InputHarvester;
import org.scijava.widget.InputPanel;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * Thanks to @frauzufall, helper class which build Swing UI of Scijava Commands
 *
 * This is convenient in particular in order to put an {@link org.scijava.command.InteractiveCommand}
 * as a Card in the BdvHandle {@link bdv.ui.CardPanel}
 *
 */

public class ScijavaSwingUI {

    static public<C extends Command> JPanel getPanel(Context context, Class<C> scijavaCommand, Object... args) {
        Module module = null;
        JPanel panel = null;
        try {
            module = createModule(context, scijavaCommand, args);
            panel = createModulePanel(context, module);

        } catch (ModuleException e) {
            e.printStackTrace();
        }
        return panel;
    }

    static public Module createModule(Context context, Class commandClass, Object... args) throws ModuleException {
        Module module = context.getService(CommandService.class).getCommand(commandClass).createModule();
        context.inject(module);
        preprocessWithoutHarvesting(context,module);
        setModuleInputs(module, args);
        return module;
    }

    static private <M extends Module> void preprocessWithoutHarvesting(Context context, M module) {
        ModuleRunner moduleRunner = new ModuleRunner(context, module, preprocessorsWithoutHarvesting(context), Collections.emptyList());
        moduleRunner.preProcess();
    }

    static private List<? extends PreprocessorPlugin> preprocessorsWithoutHarvesting(Context context) {
        //remove input harvesters from preprocessing
        List<PreprocessorPlugin> preprocessors = context.getService(PluginService.class).createInstancesOfType(PreprocessorPlugin.class);
        preprocessors.removeIf(preprocessor -> preprocessor instanceof InputHarvester);
        return preprocessors;
    }

    static private void setModuleInputs(Module module, Object[] args) {
        assert(args.length % 2 == 0);
        for (int i = 0; i < args.length-1; i+=2) {
            String input = (String) args[i];
            module.setInput(input, args[i+1]);
            module.resolveInput(input);
        }
    }

    static public JPanel createModulePanel(Context context,Module module) throws ModuleException {
        SwingInputHarvester swingInputHarvester = new SwingInputHarvester();
        context.inject(swingInputHarvester);
        InputPanel<JPanel, JPanel> inputPanel = new SwingInputPanel();
        swingInputHarvester.buildPanel(inputPanel, module);
        return inputPanel.getComponent();
    }

}
