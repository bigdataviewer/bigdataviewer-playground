package sc.fiji.bdv.sourcehandling;

import bdv.util.BdvHandle;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import sc.fiji.bdv.BDVSingleton;
import sc.fiji.bdv.BdvMenuUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AddSourceFromFileMenuEntry implements ActionListener
{
	private final BdvHandle bdvHandle;

	public AddSourceFromFileMenuEntry( BdvHandle bdvHandle )
	{
		this.bdvHandle = bdvHandle;
	}

	public void add( String menuText, String menuItemText )
	{
		final JMenu jMenu = createMenuItem( menuText, menuItemText );
		BdvMenuUtils.addMenu( bdvHandle, jMenu );
	}

	public JMenu createMenuItem( String menuText, String menuItemText )
	{
		final JMenu jMenu = new JMenu( menuText );
		final JMenuItem jMenuItem = new JMenuItem( menuItemText );
		jMenuItem.addActionListener( this );
		jMenu.add(jMenuItem);
		return jMenu;
	}

	@Override
	public void actionPerformed( ActionEvent e )
	{
		SourceHandlingUtils.showFileDialogLoadAndAddSource( bdvHandle );
	}

	public static void main( String[] args ) throws SpimDataException
	{
		final BdvHandle bdvHandle = BDVSingleton.getInstance( new XmlIoSpimData().load( "/Users/tischer/Documents/bigdataviewer-playground/src/test/resources/mri-stack.xml" ) );
		new AddSourceFromFileMenuEntry( bdvHandle ).add("Source", "Add from File");
	}
}

