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
import sc.fiji.bdvpg.bdv.projector.BlendingMode;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import spimdata.util.Displaysettings;
import spimdata.util.DisplaysettingsHelper;

import java.util.HashMap;
import java.util.Map;

public class SourceAndConverterFromSpimDataCreator
{
	private final AbstractSpimData asd;
	private final Map< Integer, SourceAndConverter > setupIdToSourceAndConverter;
	private final Map< SourceAndConverter, Map< String, Object > > sourceAndConverterToMetadata;

	public SourceAndConverterFromSpimDataCreator( AbstractSpimData asd )
	{
		this.asd = asd;
		setupIdToSourceAndConverter = new HashMap<>();
		sourceAndConverterToMetadata = new HashMap<>();
		createSourceAndConverters();
	}

	public Map< Integer, SourceAndConverter > getSetupIdToSourceAndConverter()
	{
		return setupIdToSourceAndConverter;
	}

	public Map< String, Object > getMetadata( SourceAndConverter< ? > sourceAndConverter)
	{
		return sourceAndConverterToMetadata.get( sourceAndConverter );
	}

	private void createSourceAndConverters()
	{
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

			sourceAndConverterToMetadata.put( setupIdToSourceAndConverter.get( setupId ), new HashMap<>() );
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
			if ( blendingMode != null)
				sourceAndConverterToMetadata.get( setupIdToSourceAndConverter.get( setupId ) ).put(  BlendingMode.BLENDING_MODE, blendingMode );
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
