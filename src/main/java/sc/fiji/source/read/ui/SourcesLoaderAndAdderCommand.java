package sc.fiji.source.read.ui;

import bdv.util.BdvHandle;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.scijava.ScijavaBdvDefaults;
import sc.fiji.source.read.SourcesLoaderAndAdder;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

@Plugin( type = Command.class, menuPath = ScijavaBdvDefaults.RootMenu+"Tools>Load Multiple XML/HDF5" )
public class SourcesLoaderAndAdderCommand implements Command
{

	@Parameter(type = ItemIO.BOTH)
	BdvHandle bdvHandle;

	/**
	 * TODO:
	 * This message needs to be there due to a bug in the File[] command.
	 * The user interface for selecting multiple files only shows up if there
	 * is at least one parameter above it.
	 */
	@Parameter ( visibility = ItemVisibility.MESSAGE  )
	String msg = "";

	@Parameter
	private File[] files;

	@Override
	public void run()
	{
		final String[] filePaths = (String[]) Arrays.stream(files).map(f->f.getAbsolutePath()).collect(Collectors.toList()).toArray();//Util.fileArrayToStringArray( files );

		new SourcesLoaderAndAdder( bdvHandle, filePaths ).run();
	}

}