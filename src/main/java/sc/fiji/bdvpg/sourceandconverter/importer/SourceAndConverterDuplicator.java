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
package sc.fiji.bdvpg.sourceandconverter.importer;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.function.Function;

    public class SourceAndConverterDuplicator implements Runnable, Function<SourceAndConverter, SourceAndConverter> {

    SourceAndConverter sac_in;

    public SourceAndConverterDuplicator(SourceAndConverter sac) {
        sac_in = sac;
    }

    @Override
    public void run() {
        // Nothing
    }

    public SourceAndConverter get() {
        return apply(sac_in);
    }

    @Override
    public SourceAndConverter apply(SourceAndConverter sourceAndConverter) {
        SourceAndConverter sac;
        if (sourceAndConverter.asVolatile() != null) {
            sac = new SourceAndConverter(
                    sourceAndConverter.getSpimSource(),
                    SourceAndConverterHelper.cloneConverter(sourceAndConverter.getConverter(), sourceAndConverter),
                    new SourceAndConverter(sourceAndConverter.asVolatile().getSpimSource(),
                            SourceAndConverterHelper.cloneConverter(sourceAndConverter.asVolatile().getConverter(), sourceAndConverter.asVolatile()))
            );
        } else {
            sac = new SourceAndConverter(
                    sourceAndConverter.getSpimSource(),
                    SourceAndConverterHelper.cloneConverter(sourceAndConverter.getConverter(), sourceAndConverter));
        }
        SourceAndConverterServices.getSourceAndConverterService().register(sac);
        return sac;
    }

}
