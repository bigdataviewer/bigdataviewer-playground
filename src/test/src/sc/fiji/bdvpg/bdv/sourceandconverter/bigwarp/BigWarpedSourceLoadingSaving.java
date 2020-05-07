package sc.fiji.bdvpg.bdv.sourceandconverter.bigwarp;

import java.io.File;
import java.io.IOException;

import org.jdom2.Element;

import bdv.img.WarpedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bigwarp.landmarks.LandmarkTableModel;
import net.imagej.ImageJ;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;
import sc.fiji.bdvpg.spimdata.importer.XmlIoWarpedSource;

/**
 * 2020 May
 * @author John Bogovic
 */
public class BigWarpedSourceLoadingSaving {
    public static void main(String... args) throws SpimDataException, IOException {

        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // Creates a BdvHandle
        BdvHandle bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();

		File warpedSourceXml = new File( "src/test/resources/mri-stack_warped.xml" );

		// The original source
        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml");
        AbstractSpimData asd = importer.get();
        SourceAndConverter sac = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(asd)
                .get(0);

 
        // Show the source
        SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, sac );

		XmlIoWarpedSource io = new XmlIoWarpedSource();

        /*
         * The code below creats / saves an xml storing 
         */
		File bigwarpLandmarksFile = new File("src/test/resources/mri-stack_bigwarpLandmarks.csv" );
		LandmarkTableModel bwLtm = new LandmarkTableModel( 3 );
		bwLtm.load( bigwarpLandmarksFile );
		Element root = io.toXml( asd, bwLtm.getTransform(), warpedSourceXml );
		io.save( root, warpedSourceXml.getCanonicalPath() );


		WarpedSource ws = io.load( warpedSourceXml.getCanonicalPath() );
        SourceAndConverter warpedSac = SourceAndConverterUtils.createSourceAndConverter( ws );

        // This looks correct
//        BdvFunctions.show( ws );

        // Show the warped source
        SourceAndConverterServices.getSourceAndConverterDisplayService().show(bdvHandle, warpedSac );

        // Adjust Bdv View on the source
//        new ViewerTransformAdjuster(bdvHandle, sac ).run();
        new ViewerTransformAdjuster(bdvHandle, warpedSac ).run();
        
    }

}
