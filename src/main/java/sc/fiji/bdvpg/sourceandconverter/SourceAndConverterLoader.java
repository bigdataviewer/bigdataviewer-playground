package sc.fiji.bdvpg.sourceandconverter;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.util.ArrayList;

public class SourceAndConverterLoader implements Runnable
{
	private final String filePath;
	private SpimData spimData;

	public SourceAndConverterLoader( final String filePath )
	{
		this.filePath = filePath;
	}

	@Override
	public void run()
	{
		try
		{
			spimData = new XmlIoSpimData().load( filePath );
		} catch ( SpimDataException e )
		{
			e.printStackTrace();
		}
	}

	public SpimData getSpimData()
	{
		if ( spimData == null ) run();

		return spimData;
	}

	public SourceAndConverter< ? > getSourceAndConverter( int sourceIndex )
	{
		if ( spimData == null ) run();

		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList<>();
		BigDataViewer.initSetups( spimData, converterSetups, sources );
		final SourceAndConverter< ? > sourceAndConverter = sources.get( sourceIndex );
		return sourceAndConverter;
	}

}
