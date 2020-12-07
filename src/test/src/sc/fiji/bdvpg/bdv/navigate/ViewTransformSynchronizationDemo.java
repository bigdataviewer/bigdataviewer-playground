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
package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.junit.Test;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

/**
 * ViewTransformSynchronizationDemo
 * <p>
 * <p>
 * <p>
 * Author: Nicolas Chiaruttini
 * 01 2020
 */
public class ViewTransformSynchronizationDemo {

    static boolean isSynchronizing;

    public static void main(String[] args) {

        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // load and convert an image
        ImagePlus imp = IJ.openImage("src/test/resources/blobs.tif");
        RandomAccessibleInterval rai = ImageJFunctions.wrapReal(imp);
        // Adds a third dimension because BDV needs 3D
        rai = Views.addDimension( rai, 0, 0 );

        // Makes BDV Source
        Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");
        SourceAndConverter sac = SourceAndConverterUtils.createSourceAndConverter(source);

        // Creates a BdvHandle
        BdvHandle bdvHandle1 = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();
        // Creates a BdvHandle
        BdvHandle bdvHandle2 = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();
        // Creates a BdvHandles
        BdvHandle bdvHandle3 = SourceAndConverterServices.getSourceAndConverterDisplayService().getNewBdv();

        BdvHandle[] bdvhs = new BdvHandle[]{bdvHandle1,bdvHandle2,bdvHandle3};

        ViewerTransformSyncStarter syncstart = new ViewerTransformSyncStarter(bdvhs, false);
        ViewerTransformSyncStopper syncstop = new ViewerTransformSyncStopper(syncstart.getSynchronizers(), null);

        syncstart.run();
        isSynchronizing = true;

        for (BdvHandle bdvHandle:bdvhs) {
            // Show the sourceandconverter
            SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac);

            // Adjust view on sourceandconverter
            new ViewerTransformAdjuster(bdvHandle, sac).run();

            new ClickBehaviourInstaller(bdvHandle, (x,y) -> {
                if (isSynchronizing) {
                    syncstop.run();
                } else {
                    syncstart.setBdvHandleInitialReference(bdvHandle);
                    syncstart.run();
                }
                isSynchronizing = !isSynchronizing;
            }).install("Toggle Synchronization", "ctrl S");
        }

    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }

}
