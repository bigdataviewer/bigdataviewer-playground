package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import org.junit.Assert;
import org.junit.Test;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;
import sc.fiji.bdvpg.sourceandconverter.display.ProjectionModeChanger;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class PerfOpenMultipleSpimDataTest
{
	public static void main( String[] args )
	{
		// Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// Gets active BdvHandle instance
		BdvHandle bdv = SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();
		tic();
		for (int i=0;i<1000;i++) {
			// Import SpimData
			new SpimDataFromXmlImporter( "src/test/resources/mri-stack.xml" ).run();
			new SpimDataFromXmlImporter("src/test/resources/mri-stack-shiftedX.xml").run();
			new SpimDataFromXmlImporter( "src/test/resources/mri-stack-shiftedY.xml" ).run();
			System.out.println(i);
		}
		toc();
	}

	static Instant start;

	public static void tic() {
		start = Instant.now();
	}

	static long timeElapsedInS;

	public static void toc() {
		Instant finish = Instant.now();
		timeElapsedInS = Duration.between(start, finish).toMillis()/1000;

		System.out.println("It took "+timeElapsedInS+" s to open 100 datasets");
	}

	@Test
	public void demoRunOk() {
		main(new String[]{""});
		Assert.assertTrue(timeElapsedInS<4);
	}
}
