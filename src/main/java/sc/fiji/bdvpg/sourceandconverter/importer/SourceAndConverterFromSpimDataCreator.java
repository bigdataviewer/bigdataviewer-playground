package sc.fiji.bdvpg.sourceandconverter.importer;

import bdv.SpimSource;
import bdv.ViewerImgLoader;
import bdv.ViewerSetupImgLoader;
import bdv.VolatileSpimSource;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import net.imglib2.converter.Converter;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import spimdata.util.Displaysettings;
import spimdata.util.DisplaysettingsHelper;

import java.util.HashMap;
import java.util.Map;

public class SourceAndConverterFromSpimDataCreator
{
	private final AbstractSpimData asd;
	private Map< Integer, SourceAndConverter > setupIdToSourceAndConverter;
	private Map< SourceAndConverter, String > sourceAndConverterToBlendingMode;

	public SourceAndConverterFromSpimDataCreator( AbstractSpimData asd )
	{
		this.asd = asd;
		createSourceAndConverters();
	}

	public Map< Integer, SourceAndConverter > getSetupIdToSourceAndConverter()
	{
		return setupIdToSourceAndConverter;
	}

	public String getBlendingMode( SourceAndConverter< ? > sourceAndConverter)
	{
		return sourceAndConverterToBlendingMode.get( sourceAndConverter );
	}

	private void createSourceAndConverters()
	{
		setupIdToSourceAndConverter = new HashMap<>();
		sourceAndConverterToBlendingMode = new HashMap<>();

		boolean nonVolatile = WrapBasicImgLoader.wrapImgLoaderIfNecessary( asd );

		if ( nonVolatile )
		{
			System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
		}

		final AbstractSequenceDescription< ?, ?, ? > seq = asd.getSequenceDescription();

		final ViewerImgLoader imgLoader = ( ViewerImgLoader ) seq.getImgLoader();
		for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() ) {

			final int setupId = setup.getId();

			ViewerSetupImgLoader vsil = imgLoader.getSetupImgLoader(setupId);

			String sourceName = createSetupName(setup);

			final Object type = vsil.getImageType();

			if (type instanceof RealType ) {

				createRealTypeSourceAndConverter( nonVolatile, setupId, sourceName );

			} else if (type instanceof ARGBType ) {

				createARGBTypeSourceAndConverter( setupId, sourceName );

			} else {
				SourceAndConverterHelper.errlog.accept("Cannot open Spimdata with Source of type "+type.getClass().getSimpleName());
			}

			fetchAndApplyDisplaySettings( setup, setupId );
		}

		WrapBasicImgLoader.removeWrapperIfPresent( asd );
	}

	private void createRealTypeSourceAndConverter( boolean nonVolatile, int setupId, String sourceName )
	{
		final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

		Converter nonVolatileConverter = SourceAndConverterHelper.createConverterRealType((RealType)s.getType()); // IN FACT THE CASTING IS NECESSARY!!

		if (!nonVolatile ) {

			final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );

			Converter volatileConverter = SourceAndConverterHelper.createConverterRealType((RealType)vs.getType());

			setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter, new SourceAndConverter<>(vs, volatileConverter)));

		} else {

			setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter));
		}
	}

	private void createARGBTypeSourceAndConverter( int setupId, String sourceName )
	{
		final VolatileSpimSource vs = new VolatileSpimSource<>( asd, setupId, sourceName );
		final SpimSource s = new SpimSource<>( asd, setupId, sourceName );

		Converter nonVolatileConverter = SourceAndConverterHelper.createConverterARGBType(s);
		if (vs!=null) {
			Converter volatileConverter = SourceAndConverterHelper.createConverterARGBType(vs);
			setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter, new SourceAndConverter<>(vs, volatileConverter)));
		} else {
			setupIdToSourceAndConverter.put( setupId, new SourceAndConverter(s, nonVolatileConverter));
		}
	}

	private void fetchAndApplyDisplaySettings( BasicViewSetup setup, int setupId )
	{
		if ( setup.getAttribute(Displaysettings.class)!=null) {
			final String blendingMode = DisplaysettingsHelper.PullDisplaySettings( setupIdToSourceAndConverter.get( setupId ), setup.getAttribute(Displaysettings.class));
			sourceAndConverterToBlendingMode.put( setupIdToSourceAndConverter.get( setupId ), blendingMode );
		}
	}

	private static String createSetupName( final BasicViewSetup setup ) {
		if ( setup.hasName() ) {
			if (!setup.getName().trim().equals("")) {
				return setup.getName();
			}
		}

		String name = "";

		final Angle angle = setup.getAttribute( Angle.class );
		if ( angle != null )
			name += ( name.isEmpty() ? "" : " " ) + "a " + angle.getName();

		final Channel channel = setup.getAttribute( Channel.class );
		if ( channel != null )
			name += ( name.isEmpty() ? "" : " " ) + "c " + channel.getName();

		if ((channel == null)&&(angle == null)) {
			name += "id "+setup.getId();
		}

		return name;
	}

}
