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
package sc.fiji.bdvpg.sciview;

import bdv.util.BdvFunctions;
import bdv.viewer.SourceAndConverter;
import bvv.util.BvvFunctions;
import bvv.util.BvvStackSource;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import sc.fiji.bdvpg.scijava.command.sciview.SciviewSourceAndConverterAdderCommand;
import sc.iview.SciView;

import java.util.Random;
import java.util.concurrent.ExecutionException;

public class SciViewDemo {

    static ImageJ imagej;

    public static void main(String... args) throws Error, ExecutionException, InterruptedException {
        SciView sciview = null;
        try {
            sciview = SciView.create();
        } catch (Exception e) {
            e.printStackTrace();
        }

        imagej = new ImageJ(sciview.getScijavaContext());
        // Arrange
        // create the ImageJ application context with all available services
        //final ImageJ ij = new ImageJ();
        imagej.ui().showUI();

        Random random = new Random();
        Img<UnsignedShortType> img = ArrayImgs.unsignedShorts(10, 10, 10);
        img.forEach(t -> t.set(random.nextInt()));

        BdvFunctions.show(img, "Random box");

        BvvStackSource<?> bss = BvvFunctions.show( img, "Random Box");//, bvvOptions );

        bss.getConverterSetups().get(0).setDisplayRange(0,65535);

        SourceAndConverter sac = bss.getSources().get(0);

        imagej.command()
                .run(SciviewSourceAndConverterAdderCommand.class,
                        true,
                        "sacs", new SourceAndConverter[]{sac},
                        "numtimepoints", 1
                        ).get();

        System.out.println("Source displayed in sciview");

    }
}
