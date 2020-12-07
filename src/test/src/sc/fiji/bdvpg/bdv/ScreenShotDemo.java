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
package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import org.junit.Test;
import sc.fiji.bdvpg.bdv.ScreenShotMaker;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ProjectionModeChanger;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

/**
 * ScreenShotDemo
 * <p>
 * <p>
 * <p>
 * Author: Tischi
 * 12 2019
 */
public class ScreenShotDemo
{
    public static void main(String[] args)
    {
        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Gets active BdvHandle instance
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

        // Import SpimData
        new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").run();
        new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();

        final ISourceAndConverterService sacService = SourceAndConverterServices.getSourceAndConverterService();

        final List< SourceAndConverter > sacs = sacService.getSourceAndConverters();

        showSacs( bdvHandle, sacs );

        new ProjectionModeChanger( new SourceAndConverter[]{ sacs.get( 0 ) }, Projection.PROJECTION_MODE_AVG, true ).run();

        ScreenShotMaker screenShotMaker = new ScreenShotMaker( bdvHandle );
        screenShotMaker.setPhysicalPixelSpacingInXY( 0.5, "micron" );
        screenShotMaker.getRgbScreenShot().show();
        screenShotMaker.getRawScreenShot().show();
    }

    public static void showSacs( BdvHandle bdvHandle, List< SourceAndConverter > sacs )
    {
        sacs.forEach( sac -> {
                SourceAndConverterServices.getSourceAndConverterDisplayService().show( bdvHandle, sac );
                new ViewerTransformAdjuster( bdvHandle, sac ).run();
                new BrightnessAutoAdjuster( sac, 0 ).run();
        } );
    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }
}
