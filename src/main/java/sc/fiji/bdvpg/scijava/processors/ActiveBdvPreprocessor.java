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
package sc.fiji.bdvpg.scijava.processors;

import bdv.util.BdvHandle;
import net.imagej.display.process.SingleInputPreprocessor;
import org.scijava.Priority;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;
import sc.fiji.bdvpg.scijava.services.GuavaWeakCacheService;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Fills single, unresolved module inputs with the active {@link BdvHandle},
 * <em>or a newly created one if none</em>.
 *
 * @author Curtis Rueden, Nicolas Chiaruttini
 */
@Plugin(type = PreprocessorPlugin.class, priority = Priority.VERY_HIGH)
public class ActiveBdvPreprocessor extends SingleInputPreprocessor<BdvHandle>  {

    @Parameter
    ObjectService os;

    @Parameter
    CommandService cs;

    @Parameter
    GuavaWeakCacheService cacheService;

    public ActiveBdvPreprocessor() {
        super( BdvHandle.class );
    }

    // -- SingleInputProcessor methods --

    @Override
    public BdvHandle getValue() {

        List<BdvHandle> bdvhs = os.getObjects(BdvHandle.class);

        if ((bdvhs == null)||(bdvhs.size()==0)) {
             try
            {

                Module module = cs.moduleService()
                        .createModule(cs.getCommand(BdvWindowCreatorCommand.class));

                cs.moduleService().loadInputs(module);

                return (BdvHandle)
                        cs.moduleService()
                                .run(module, true, module.getInputs())
                                .get()
                                .getOutput("bdvh");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        assert bdvhs != null;
        if (bdvhs.size()==1) {
            return bdvhs.get(0);
        } else {

            // Get the one with the most recent focus ?
            Optional<BdvHandle> bdvh = bdvhs.stream().filter(b -> b.getViewerPanel().hasFocus()).findFirst();
            if (bdvh.isPresent()) {
                return bdvh.get();
            } else {
                if (cacheService.get("LAST_ACTIVE_BDVH")!=null) {
                    WeakReference<BdvHandle> wr_bdv_h = (WeakReference<BdvHandle>) cacheService.get("LAST_ACTIVE_BDVH");
                    return wr_bdv_h.get();
                } else {
                    return null;
                }
            }
        }
    }

}
