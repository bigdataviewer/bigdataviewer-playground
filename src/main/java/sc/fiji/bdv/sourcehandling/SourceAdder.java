package sc.fiji.bdv.sourcehandling;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import sc.fiji.bdv.BDVSingleton;

import javax.swing.*;

public class SourceAdder implements Runnable
{
	private final BdvHandle bdvHandle;
	private final Source source;

	public SourceAdder( BdvHandle bdvHandle, Source source )
	{
		this.bdvHandle = bdvHandle;
		this.source = source;
	}

	@Override
	public void run()
	{
		BdvFunctions.show( source, BdvOptions.options().addTo( bdvHandle ) );
	}
}
