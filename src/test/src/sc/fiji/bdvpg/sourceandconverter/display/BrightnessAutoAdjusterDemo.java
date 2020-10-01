package sc.fiji.bdvpg.sourceandconverter.display;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import org.junit.Test;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.sourceandconverter.SourceAdder;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.importer.VoronoiSourceGetter;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.util.List;

/**
 *
 * Demo of opening two sources, adjusting the location of the Bdv Window + adjusting brightness and contrast
 *
 * TODO : solves the brightness adjuster which does not work with the voronoi source, see {@link BrightnessAutoAdjuster}
 *
 */

public class BrightnessAutoAdjusterDemo
{

    static BdvHandle bdvHandle;

	public static void main( String[] args ) {
        // Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
        ImageJ ij = new ImageJ();
        ij.ui().showUI();

		// Creates a BdvHandle
		bdvHandle = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();


        AbstractSpimData asd = new SpimDataFromXmlImporter("src/test/resources/mri-stack.xml").get();

        List<SourceAndConverter> sourcesFromSpimData = SourceAndConverterServices.getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(asd);

        addSource(bdvHandle, sourcesFromSpimData.get(0));

		// Voronoi
		final SourceAndConverter voronoiSource = new VoronoiSourceGetter( new long[]{ 512, 512, 1 }, 256, true ).get();
		addSource( bdvHandle, voronoiSource );

	}

	public static void addSource(BdvHandle bdvHandle, SourceAndConverter sourceandconverter )
	{
		new SourceAdder( bdvHandle, sourceandconverter ).run();
		new ViewerTransformAdjuster( bdvHandle, sourceandconverter ).run();
		new BrightnessAutoAdjuster( sourceandconverter,0 ).run();
	}

	@Test
    public void demoRunOk() {
	    main(new String[]{""});
	    bdvHandle.close();
    }

}
