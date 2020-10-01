package sc.fiji.bdvpg.bdv;

import net.imagej.ImageJ;
import org.junit.Test;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class BdvCreatorDemo
{
	public static void main( String[] args )
	{
		// Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		// Creates a BDV since none exists yet
		SourceAndConverterServices.getSourceAndConverterDisplayService().getActiveBdv();
	}

	@Test
	public void demoRunOk() {
		main(new String[]{""});
	}
}
