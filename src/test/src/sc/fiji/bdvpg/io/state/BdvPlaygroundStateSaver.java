/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.io.state;

import net.imagej.ImageJ;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.AffineTransformSourceDemo;
import sc.fiji.bdvpg.BigWarpDemo;
import sc.fiji.bdvpg.ResamplingDemo;
import sc.fiji.bdvpg.TestHelper;
import sc.fiji.bdvpg.WarpedSourceDemo;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;

import java.io.File;

public class BdvPlaygroundStateSaver {

    protected static final Logger logger = LoggerFactory.getLogger(BdvPlaygroundStateSaver.class);

    static ImageJ ij;

    public static void main( String[] args )
    {
        // Initializes static SourceService and Display Service
        ij = new ImageJ();
        TestHelper.startFiji(ij);//ij.ui().showUI();

        createSacs();

        new SourceAndConverterServiceSaver(
                new File("src/test/resources/bdvplaygroundstate.json"),
                ij.context()
        ).run();

        logger.info("Bdv Playground state saved!");
        System.out.println("Bdv Playground state saved!");
    }

    public static void createSacs() {
       // Creates demo Warped Sources
       BigWarpDemo.demo2d(ij);
       BigWarpDemo.demo3d(ij);
       AffineTransformSourceDemo.demo(ij,2);
       ResamplingDemo.demo();
       try {
           WarpedSourceDemo.demo();
       } catch (Exception e) {
           e.printStackTrace();
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
