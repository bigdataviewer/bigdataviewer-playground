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
package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Test;
import org.scijava.util.VersionUtils;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

/**
 * ViewTransformSynchronizationDemo
 * <p>
 * <p>
 * <p>
 * Author: Nicolas Chiaruttini
 * 01 2020
 */
public class OrthoViewDemo {

    static boolean isSynchronizing;

    static ImageJ ij;

    public static void main(String[] args) {

        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ij = new ImageJ();
        ij.ui().showUI();

        // Makes BDV Source
        System.out.println(VersionUtils.getVersion(BigWarp.class));

        new SpimDataFromXmlImporter( "src/test/resources/mri-stack.xml" ).run();

        //Source source = new RandomAccessibleIntervalSource(rai, Util.getTypeFromInterval(rai), "blobs");
        //SourceAndConverter sac = SourceAndConverterUtils.createSourceAndConverter(source);

        // Creates a BdvHandle
        BdvHandle bdvHandleX = SourceAndConverterServices.getBdvDisplayService().getNewBdv();
        // Creates a BdvHandle
        BdvHandle bdvHandleY = SourceAndConverterServices.getBdvDisplayService().getNewBdv();
        // Creates a BdvHandles
        BdvHandle bdvHandleZ = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        BdvHandle[] bdvhs = new BdvHandle[]{bdvHandleX,bdvHandleY,bdvHandleZ};

        // Get a handle on the sacs
        final List< SourceAndConverter > sacs = SourceAndConverterServices.getSourceAndConverterService().getSourceAndConverters();

        ViewerOrthoSyncStarter syncstart = new ViewerOrthoSyncStarter(bdvHandleX,bdvHandleY,bdvHandleZ, false);
        ViewerTransformSyncStopper syncstop = new ViewerTransformSyncStopper(syncstart.getSynchronizers(), null);

        syncstart.run();
        isSynchronizing = true;

        for (BdvHandle bdvHandle:bdvhs) {

            sacs.forEach( sac -> {
                SourceAndConverterServices.getBdvDisplayService().show(bdvHandle, sac);
                new ViewerTransformAdjuster(bdvHandle, sac).run();
                new BrightnessAutoAdjuster(sac, 0).run();
            });

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

    @After
    public void closeFiji() {
        TestHelper.closeFijiAndBdvs(ij);
    }
}
