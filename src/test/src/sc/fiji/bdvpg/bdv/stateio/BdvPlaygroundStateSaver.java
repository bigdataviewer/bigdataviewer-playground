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
package sc.fiji.bdvpg.bdv.stateio;

import org.junit.Test;
import sc.fiji.bdvpg.bdv.ProjectionModeChangerDemo;
import sc.fiji.bdvpg.bdv.sourceandconverter.bigwarp.BigWarpDemo;
import sc.fiji.bdvpg.bdv.sourceandconverter.transform.AffineTransformSourceDemo;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;

import java.io.File;

public class BdvPlaygroundStateSaver {

    public static void main( String[] args )
    {

        //ProjectionModeChangerDemo.main(args); // Test SpimData Saving
        AffineTransformSourceDemo.main(args); // Test Transformed Source Saving
        //BigWarpDemo.main(args); // Test Warped source saving

        new SourceAndConverterServiceSaver(
                new File("src/test/resources/bdvplaygroundstate.json"),
                AffineTransformSourceDemo.ij.context()
        ).run();

        System.out.println("Saved!");

    }

    @Test
    public void demoRunOk() {
        main(new String[]{""});
    }
}
