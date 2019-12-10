package sc.fiji.bdv;

import bdv.util.BdvHandle;
import bdv.util.BdvHandleFrame;

import javax.swing.*;

public class BdvMenuUtils
{
	public static void addMenu( BdvHandle bdvHandle, JMenu jMenu )
	{
		final JMenuBar bdvMenuBar = ( ( BdvHandleFrame ) bdvHandle ).getBigDataViewer().getViewerFrame().getJMenuBar();
		bdvMenuBar.add( jMenu );
		bdvMenuBar.updateUI();
	}
}
