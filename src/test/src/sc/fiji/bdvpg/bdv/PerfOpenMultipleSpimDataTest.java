package sc.fiji.bdvpg.bdv;

import bdv.util.BdvHandle;
import net.imagej.ImageJ;
import org.junit.Assert;
import org.junit.Test;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.time.Duration;
import java.time.Instant;

/**
 * Test of opening multiple datasets
 *
 * The limiting performance factor is the construction of the tree UI which scales
 * very badly. For now, its speed it user-ok for a number of sources below ~600
 *
 * 300 datasets ~ 3 secs
 *
 * 3000 sources takes ~ 120 secs
 *
 * TODO : fix performance issue if several thousands of sources are necessary (could be coming sooner than expected)
 *
 */

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
		for (int i=0;i<100;i++) {
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

		System.out.println("It took "+timeElapsedInS+" s to open 300 datasets");
	}

	@Test
	public void demoRunOk() {
		main(new String[]{""});
		Assert.assertTrue(timeElapsedInS<4);
	}
}
