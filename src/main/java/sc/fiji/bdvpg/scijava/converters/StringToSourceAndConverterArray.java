/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.scijava.converters;

import bdv.viewer.SourceAndConverter;
import org.scijava.convert.AbstractConverter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import javax.swing.tree.TreePath;


/**
 * TODO : allows multiple Paths split by a character ? Not sure it's fool proof
 * @param <I> String class TODO understand this
 */

@Plugin(type = org.scijava.convert.Converter.class)
public class StringToSourceAndConverterArray<I extends String> extends AbstractConverter<I, SourceAndConverter<?>[]> {

    @Parameter
    SourceAndConverterService sacsService;

    @Override
    public <T> T convert(Object src, Class<T> dest) {
        String str = (String) src;
        TreePath tp = sacsService.getUI().getTreePathFromString(str);
        if (tp!=null) {
            return (T) sacsService.getUI().getSourceAndConvertersFromTreePath(tp).toArray(new SourceAndConverter[0]);//sacsService.getUI().getSourceAndConvertersFromTreePath(tp).toArray(new SourceAndConverter[0]);
        } else {
            return null;
        }
    }

    @Override
    public Class getOutputType() {
        return SourceAndConverter[].class;
    }

    @Override
    public Class<I> getInputType() {
        return (Class<I>) String.class;
    }
}
