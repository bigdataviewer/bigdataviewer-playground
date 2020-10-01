package sc.fiji.bdvpg.bdv.scijava;

import net.imagej.ImageJ;
import org.junit.Test;
import sc.fiji.bdvpg.scijava.command.spimdata.MultipleSpimDataImporterCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;

public class MultipleSpimDataImporterCommandDemo
{
	public static void main( String[] args )
	{
		// Create the ImageJ application context with all available services; necessary for SourceAndConverterServices creation
		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final File[] files = new File[ 2 ];
		files[0] = new File("src/test/resources/mri-stack.xml");
		files[1] = new File("src/test/resources/mri-stack-shiftedX.xml");
		ij.command().run( MultipleSpimDataImporterCommand.class, true, "files", files);
	}

	@Test
	public void demoRunOk() {
		main(new String[]{""});
	}
}
