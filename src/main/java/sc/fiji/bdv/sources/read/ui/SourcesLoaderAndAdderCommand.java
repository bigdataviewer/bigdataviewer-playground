package sc.fiji.bdv.sources.read.ui;

import bdv.util.BdvHandle;
import mpicbg.spim.data.SpimData;
import net.imagej.ImageJ;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.BdvUtils;
import sc.fiji.bdv.MenuAdder;
import sc.fiji.bdv.sources.read.SourceLoader;
import sc.fiji.bdv.sources.read.SourcesLoaderAndAdder;
import sc.fiji.util.Util;

import java.io.File;

@Plugin( type = Command.class, menuPath = "Plugins>BigDataViewer>Tools>Load Multiple XML/HDF5" )
public class SourcesLoaderAndAdderCommand implements Command
{
	/**
	 * TODO: Add bdvHandle as a Parameter?
	 */
	public static BdvHandle bdvHandle;

	/**
	 * TODO:
	 * This message needs to be there due to a bug in the File[] Parameter.
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
		final String[] filePaths = Util.fileArrayToStringArray( files );

		new SourcesLoaderAndAdder( bdvHandle, filePaths ).run();
	}
}