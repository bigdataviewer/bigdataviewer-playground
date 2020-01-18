package sc.fiji.bdvpg.bdv.scijava;

import net.imagej.ImageJ;
import sc.fiji.bdvpg.scijava.command.spimdata.MultipleSpimDataImporterCommand;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;

public class MultipleSpimDataImporterCommandDemo
{
	public static void main( String[] args )
	{
		SourceAndConverterServices.InitScijavaServices();
		final ImageJ imageJ = SourceAndConverterServices.getIJ();
		final File[] files = new File[ 2 ];
		files[0] = new File("src/test/resources/mri-stack.xml");
		files[1] = new File("src/test/resources/mri-stack-shiftedX.xml");
		imageJ.command().run( MultipleSpimDataImporterCommand.class, true, "files", files);
	}
}
