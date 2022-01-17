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
package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.img.WarpedSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.RealTransform;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.function.Function;

public class SourceRealTransformer implements Runnable, Function<SourceAndConverter,SourceAndConverter> {

    SourceAndConverter sourceIn;
    final RealTransform rt;
    SourceAndConverter sourceOut;

    public SourceRealTransformer(SourceAndConverter src, RealTransform rt) {
        this.sourceIn = src;
        this.rt = rt;
    }

    /**
     * Constructor without any source argument in order to use the functional interface only
     * @param rt real transform object
     */
    public SourceRealTransformer(RealTransform rt) {
        this.rt = rt;
    }

    @Override
    public void run() {
        sourceOut = apply(sourceIn);
    }

    public SourceAndConverter getSourceOut() {
        return sourceOut;
    }

    public SourceAndConverter apply(SourceAndConverter in) {
        WarpedSource src = new WarpedSource(in.getSpimSource(), "Transformed_"+in.getSpimSource().getName(), () -> false);
        src.updateTransform(rt);
        src.setIsTransformed(true);
        if (in.asVolatile()!=null) {
            WarpedSource vsrc = new WarpedSource(in.asVolatile().getSpimSource(), "Transformed_"+in.asVolatile().getSpimSource().getName(), () -> false);//f.apply(in.asVolatile().getSpimSource());
            vsrc.updateTransform(rt);
            vsrc.setIsTransformed(true);
            SourceAndConverter vout = new SourceAndConverter<>(vsrc, SourceAndConverterHelper.cloneConverter(in.asVolatile().getConverter(), in.asVolatile()));
            return new SourceAndConverter(src, SourceAndConverterHelper.cloneConverter(in.getConverter(), in), vout);
        } else {
            return new SourceAndConverter(src, SourceAndConverterHelper.cloneConverter(in.getConverter(), in));
        }
    }
}
