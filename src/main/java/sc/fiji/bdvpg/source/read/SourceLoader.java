package sc.fiji.bdvpg.source.read;

import bdv.BigDataViewer;
import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;

import java.util.ArrayList;

public class SourceLoader implements Runnable
{
	private final String filePath;
	private SpimData spimData;

	public SourceLoader( final String filePath )
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

	/**
	 * TODO: think about converter
	 *
	 * @param sourceIndex
	 * @return
	 */
	public Source getSource( int sourceIndex )
	{
		if ( spimData == null ) run();

		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList<>();
		BigDataViewer.initSetups( spimData, converterSetups, sources );
		final Source source = sources.get( sourceIndex ).getSpimSource();
		return source;
	}

}
