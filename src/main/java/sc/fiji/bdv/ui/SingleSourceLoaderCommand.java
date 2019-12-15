package sc.fiji.bdv.ui;

import bdv.util.BdvHandle;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import net.imagej.ImageJ;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.MenuAdder;
import sc.fiji.bdv.navigate.ViewerTransformAdjuster;
import sc.fiji.bdv.sources.display.BrightnessAdjuster;
import sc.fiji.bdv.sources.read.SourceAdder;
import sc.fiji.bdv.sources.read.SourceLoader;
import sc.fiji.bdv.sources.read.SourcesLoaderAndAdder;
import sc.fiji.util.Util;

import java.io.File;

@Plugin( type = Command.class, menuPath = "Plugins>BigDataViewer>Tools>Add Single XML/HDF5" )
public class SingleSourceLoaderCommand implements Command
{
	/**
	 * TODO: Can we have a bdvHandle as a Parameter?
	 */
	public static BdvHandle bdvHandle;

	@Parameter ( label = "XML/HDF5 Image Source File")
	private File file;

	@Parameter ( label = "Automatically Adjust Brightness")
	private boolean autoAdjustBrightness;

	@Parameter ( label = "Automatically Adjust Viewer Transform")
	private boolean autoAdjustViewerTransform;

	private Source source;

	@Override
	public void run()
	{
		loadSource();
		addSource();
		if ( autoAdjustBrightness ) adjustBrightness();
		if ( autoAdjustViewerTransform ) adjustViewerTransform();
	}

	public void adjustViewerTransform()
	{
		new ViewerTransformAdjuster( bdvHandle, source ).run();
	}

	public void adjustBrightness()
	{
		new BrightnessAdjuster( bdvHandle, source, 0.01, 0.99 ).run();
	}

	public void addSource()
	{
		new SourceAdder( bdvHandle, source ).run();
	}

	public void loadSource()
	{
		final SourceLoader sourceLoader = new SourceLoader( file.getAbsolutePath() );
		sourceLoader.run();
		source = sourceLoader.getSource( 0 );
	}

	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final String filePath = "/Users/tischer/Documents/bigdataviewer-playground/src/test/resources/mri-stack.xml";

		final SourceLoader sourceLoader = new SourceLoader( filePath );
		sourceLoader.run();
		final SpimData spimData = sourceLoader.getSpimData();

		final BdvHandle bdvHandle = BDVSingleton.getInstance( spimData );

		//BdvUtils.initBrightness( bdvHandle, 0.01, 0.99, 0 );

		final MenuAdder menuAdder = new MenuAdder( bdvHandle, e ->
		{
			SingleSourceLoaderCommand.bdvHandle = bdvHandle;
			ij.command().run( SingleSourceLoaderCommand.class, true );
		} );
		menuAdder.addMenu( "Sources", "Load Sources" );
	}
}