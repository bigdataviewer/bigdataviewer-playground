package sc.fiji.bdv;

import bdv.util.BdvHandle;
import bdv.util.BdvHandleFrame;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import sc.fiji.bdv.sourcehandling.AddSourceFromFileMenuEntryAdder;

import javax.swing.*;
import java.awt.event.ActionListener;

public class MenuAdder
{
	private final BdvHandle bdvHandle;
	private final ActionListener actionListener;

	public MenuAdder( BdvHandle bdvHandle, ActionListener actionListener )
	{
		this.bdvHandle = bdvHandle;
		this.actionListener = actionListener;
	}

	public void addMenu( String menuText, String menuItemText )
	{
		final JMenu jMenu = createMenuItem( menuText, menuItemText );
		final JMenuBar bdvMenuBar = ( ( BdvHandleFrame ) bdvHandle ).getBigDataViewer().getViewerFrame().getJMenuBar();
		bdvMenuBar.add( jMenu );
		bdvMenuBar.updateUI();
	}

	public JMenu createMenuItem( String menuText, String menuItemText )
	{
		final JMenu jMenu = new JMenu( menuText );
		final JMenuItem jMenuItem = new JMenuItem( menuItemText );
		jMenuItem.addActionListener( actionListener );
		jMenu.add( jMenuItem );
		return jMenu;
	}

	public static void main( String[] args ) throws SpimDataException
	{
		final BdvHandle bdvHandle = BDVSingleton.getInstance( new XmlIoSpimData().load( "/Users/tischer/Documents/bigdataviewer-playground/src/test/resources/mri-stack.xml" ) );
		new AddSourceFromFileMenuEntryAdder( bdvHandle ).add("Source", "Open" );
	}
}
