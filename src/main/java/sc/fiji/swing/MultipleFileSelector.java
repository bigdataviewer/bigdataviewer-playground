package sc.fiji.swing;

import javax.swing.*;
import java.io.File;

public class MultipleFileSelector
{
	private static String recentDirectory = null;
	private File[] selectedFiles;

	public boolean showUI()
	{
		final JFileChooser jFileChooser = new JFileChooser( recentDirectory );
		jFileChooser.setMultiSelectionEnabled( true );
		if ( jFileChooser.showOpenDialog( null ) == JFileChooser.APPROVE_OPTION )
		{
			selectedFiles = jFileChooser.getSelectedFiles();
			recentDirectory = selectedFiles[ 0 ].getParent();
			return true;
		}
		else
		{
			return false;
		}
	}

	public String[] getSelectedFilePaths()
	{
		final String[] filePaths = new String[ selectedFiles.length ];

		for ( int i = 0; i < filePaths.length; i++ )
		{
			filePaths[ i ] = selectedFiles[ i ].getAbsolutePath();
		}

		return filePaths;
	}
}
