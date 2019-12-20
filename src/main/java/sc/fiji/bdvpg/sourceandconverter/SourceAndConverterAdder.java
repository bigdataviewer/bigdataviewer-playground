package sc.fiji.bdvpg.sourceandconverter;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvStackSource;
import bdv.viewer.SourceAndConverter;

import java.util.ArrayList;
import java.util.List;

public class SourceAndConverterAdder< T > implements Runnable
{
	private final BdvHandle bdvHandle;
	private final SourceAndConverter< T > sourceAndConverter;
	private BdvStackSource< T > bdvStackSource;

	public SourceAndConverterAdder( BdvHandle bdvHandle, SourceAndConverter< T > sourceAndConverter )
	{
		this.bdvHandle = bdvHandle;
		this.sourceAndConverter = sourceAndConverter;
	}

	@Override
	public void run()
	{
		final int numTimePoints = 1; // TODO: ...

		bdvStackSource = BdvFunctions.show( sourceAndConverter, numTimePoints );
	}

	public BdvStackSource< T > getBdvStackSource()
	{
		return bdvStackSource;
	}
}
