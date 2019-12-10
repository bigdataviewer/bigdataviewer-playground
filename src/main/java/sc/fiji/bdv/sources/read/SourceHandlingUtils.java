package sc.fiji.bdv.sources.read;

import bdv.util.BdvHandle;

import javax.swing.*;

public abstract class SourceHandlingUtils
{
	public static void showFileDialogLoadAndAddSource( BdvHandle bdvHandle )
	{
		SwingUtilities.invokeLater( () -> {
			final JFileChooser jFileChooser = new JFileChooser();
			if ( jFileChooser.showOpenDialog( bdvHandle.getViewerPanel() ) == JFileChooser.APPROVE_OPTION )
			{
				final String absolutePath = jFileChooser.getSelectedFile().getAbsolutePath();
				new SourceLoaderAndAdder( bdvHandle, absolutePath ).run();
			};
		});
	}
}
