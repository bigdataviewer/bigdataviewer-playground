package sc.fiji.bdv.sourcehandling;

import bdv.util.BdvHandle;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.MenuAdder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AddSourceFromFileMenuEntryAdder implements ActionListener
{
	private final BdvHandle bdvHandle;

	public AddSourceFromFileMenuEntryAdder( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	public void add( String menuText, String menuItemText )
	{
		final MenuAdder menuAdder = new MenuAdder( bdvHandle, this );
		menuAdder.addMenu( menuText, menuItemText );
	}

	@Override
	public void actionPerformed( ActionEvent e )
	{
		SourceHandlingUtils.showFileDialogLoadAndAddSource( bdvHandle );
	}

	public static void main( String[] args ) throws SpimDataException
	{
		final BdvHandle bdvHandle = BDVSingleton.getInstance( new XmlIoSpimData().load( "/Users/tischer/Documents/bigdataviewer-playground/src/test/resources/mri-stack.xml" ) );
		new AddSourceFromFileMenuEntryAdder( bdvHandle ).add( "Source", "Open" );
	}
}

