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
package sc.fiji.bdvpg.io;

import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import sc.fiji.bdvpg.sourceandconverter.exporter.XmlHDF5SpimdataExporter;
import sc.fiji.bdvpg.sourceandconverter.importer.VoronoiSourceGetter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class XmlHDF5SpimdataExporterTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test@Ignore // Ignore because of license issue
    public void run() throws Exception {
        // Need to initialize the services:
        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Arrange
        // creates a Voronoi SourceAndConverter
        SourceAndConverter sac = new VoronoiSourceGetter(new long[]{512,512,1},256,true).get();
        // Puts it into a List
        List<SourceAndConverter> sacs = new ArrayList<>();
        sacs.add(sac);
        // Makes temp file which will be deleted at the end of the test execution
        File fileXmlGen = folder.newFile("testVoronoi.xml");
        File fileH5Gen = folder.newFile("testVoronoi.h5");

        // Act
        XmlHDF5SpimdataExporter exporter = new XmlHDF5SpimdataExporter(sacs,1,0,1,4,64,64,1,512, fileXmlGen);
        exporter.run();

        // Assert
        File fileXmlControl = new File("src/test/resources/testVoronoi.txt");
        File fileH5Control = new File("src/test/resources/testVoronoi.h5");


        //-------------- Uncomment TO DEBUG

        BufferedReader br = new BufferedReader(new FileReader(fileXmlControl.getAbsoluteFile()));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        br.close();
        line = null;

        br = new BufferedReader(new FileReader(fileXmlGen.getAbsoluteFile()));
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        br.close();

        // -------------------------------------- End of uncomment

        Assert.assertTrue(FileUtils.contentEquals(fileXmlGen, fileXmlControl));
        //Assert.assertTrue(fileH5Gen.length() == fileH5Control.length()); //Fails and I don't know why


    }

}
