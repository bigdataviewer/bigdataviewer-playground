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
package sc.fiji.bdvpg.scijava.processors;

import bdv.util.BdvHandle;
import bvv.util.BvvHandle;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.BvvHandleHelper;
import sc.fiji.bdvpg.scijava.services.GuavaWeakCacheService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

import java.util.function.Consumer;

/**
 * Ensures BdvHandle is stored into ObjectService
 * and all containing Sources as well are stored into the BdvSourceAndConverterDisplayService and
 * BdvSourceAndConverterService
 * Also fix BDV Close operation
 */

@Plugin(type = PostprocessorPlugin.class)
public class BvvHandlePostprocessor extends AbstractPostprocessorPlugin {

    @Parameter
    SourceAndConverterBdvDisplayService bsds;

    @Parameter
    ObjectService os;

    @Parameter
    GuavaWeakCacheService cacheService;

    public static Consumer<String> log = (str) -> System.out.println(BvvHandlePostprocessor.class.getSimpleName()+":"+str);

    @Override
    public void process(Module module) {

        module.getOutputs().forEach((name, object)-> {
            if (object instanceof BvvHandle) {
                BvvHandle bvvh = (BvvHandle) object;
                log.accept("BdvHandle found.");
                //------------ Register BdvHandle in ObjectService
                os.addObject(bvvh);
                //------------ Allows to remove the BdvHandle from the objectService when closed by the user
                BvvHandleHelper.setBvvHandleCloseOperation(bvvh, cacheService,  os, bsds, true);
                //------------ Renames window to ensure unicity
                String windowTitle = BvvHandleHelper.getWindowTitle(bvvh);
                windowTitle = BvvHandleHelper.getUniqueWindowTitle(os, windowTitle);
                BvvHandleHelper.setWindowTitle(bvvh, windowTitle);
                //for (int i=0;i<bdvh.getViewerPanel().getState().numSources();i++) {
                //    bsds.registerBdvSource(bdvh,i);
                //}
                module.resolveOutput(name);
            }
        });

    }

}
