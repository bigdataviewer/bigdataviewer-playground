package test;

import bdv.viewer.SourceAndConverter;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
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
    public TemporaryFolder folder= new TemporaryFolder();

    @Test
    public void run() throws Exception {
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
        XmlHDF5SpimdataExporter exporter = new XmlHDF5SpimdataExporter(sacs,1,0,1,4,64,64,1,fileXmlGen);
        exporter.run();

        // Assert
        File fileXmlControl = new File("src/test/resources/testVoronoi.xml");
        File fileH5Control = new File("src/test/resources/testVoronoi.h5");

        Assert.assertTrue(FileUtils.contentEquals(fileXmlGen, fileXmlControl));
        Assert.assertTrue(fileH5Gen.length() == fileH5Control.length());

        /*
        TO DEBUG

        BufferedReader br = new BufferedReader(new FileReader(fileXmlControl.getAbsoluteFile()));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }

        br = new BufferedReader(new FileReader(fileXmlGen.getAbsoluteFile()));
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        */
    }

}
