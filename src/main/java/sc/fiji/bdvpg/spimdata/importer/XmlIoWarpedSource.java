package sc.fiji.bdvpg.spimdata.importer;

import static mpicbg.spim.data.XmlKeys.SPIMDATA_TAG;
import static mpicbg.spim.data.XmlKeys.SPIMDATA_VERSION_ATTRIBUTE_CURRENT;
import static mpicbg.spim.data.XmlKeys.SPIMDATA_VERSION_ATTRIBUTE_NAME;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.codec.binary.Base64;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import bdv.img.WarpedSource;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarpInit;
import bigwarp.BigWarp.BigWarpData;
import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.XmlKeys;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.XmlIoSingleton;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.io.TpsWrapper;
import sc.fiji.bdvpg.sourceandconverter.transform.io.XmlToThinPlateSpline;

public class XmlIoWarpedSource extends XmlIoSingleton<String>
{
	public static final String WARPED_SPIMDATA_TAG = "WarpedSpimData";

	public static final String BWTRANSFORM_TAG = "BigWarpTransform";

	private XmlIoSpimData sdio = new XmlIoSpimData();

	private XmlToThinPlateSpline tpsIo = new XmlToThinPlateSpline();

	public XmlIoWarpedSource()
	{
		super( WARPED_SPIMDATA_TAG,  String.class );
	}
	
	public static void main( String[] args ) throws SpimDataException, IOException
	{
		System.out.println("start");
		
//		SpimDataFromXmlImporter sdio = new SpimDataFromXmlImporter( new File ( args[ 0 ]));
//		sdio.run();
//		AbstractSpimData data = sdio.get();

		SpimDataMinimal data = new XmlIoSpimDataMinimal().load( args[ 0 ]);
		System.out.println( "data: " + data );
		

		File xmlFile = new File("/home/john/tmp/warpedSourceXml/ws.xml");
		XmlIoWarpedSource io = new XmlIoWarpedSource();

//		ThinPlateR2LogRSplineKernelTransform tps = new ThinPlateR2LogRSplineKernelTransform( 3 );
		LandmarkTableModel ltm = new LandmarkTableModel( 3 );
		ltm.load( new File( "/home/john/tmp/t1-head_landmarks.csv" ));


//		System.out.println( " ");
//		System.out.println( e );
//		System.out.println( e.getChildren().size() );
//		System.out.println( " ");

//		Element e = io.toXml( data, ltm.getTransform(), xmlFile );
//		io.save( e, xmlFile.getCanonicalPath() );

		WarpedSource ws = io.load( xmlFile.getCanonicalPath() );
		System.out.println( " ");
		System.out.println( " ws: " + ws );

		System.out.println("done");
	}


	public WarpedSource<?> load( final String xmlFilename ) throws SpimDataException
	{
		final SAXBuilder sax = new SAXBuilder();
		Document doc;
		try
		{
			final File file = new File( xmlFilename );
			if ( file.exists() )
				doc = sax.build( file );
			else
				doc = sax.build( xmlFilename );
		}
		catch ( final Exception e )
		{
			throw new SpimDataIOException( e );
		}
		final Element root = doc.getRootElement();

		// find child that matches the transform tag
		root.getChildren();

		if ( root.getName() != WARPED_SPIMDATA_TAG )
			throw new RuntimeException( "expected <" + WARPED_SPIMDATA_TAG + "> root element. wrong file?" );

		return fromXml( root, new File( xmlFilename ) );
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public WarpedSource fromXml( final Element root, final File xmlFile ) throws SpimDataException
	{
		
		Element spimElem = root.getChild( XmlKeys.SPIMDATA_TAG );
		if ( spimElem == null )
			throw new RuntimeException( "missing <" + XmlKeys.SPIMDATA_TAG + "> tag" );
		
		// get spim data
		SpimData sd = sdio.fromXml( spimElem, xmlFile );

		Element tpsElem = root.getChild( XmlToThinPlateSpline.TPS_TAG );
		if ( tpsElem == null )
			throw new RuntimeException( "missing <" + XmlToThinPlateSpline.TPS_TAG + "> tag" );

		// read transform
		ThinplateSplineTransform xfm = new ThinplateSplineTransform( tpsIo.fromXml( tpsElem, xmlFile ).get() );

		Map<Integer, SourceAndConverter> srcMap = SourceAndConverterUtils.createSourceAndConverters( sd );

		// TODO generalize the below instead of getting the 
		SourceAndConverter<?> src = srcMap.get( srcMap.keySet().iterator().next());

		WarpedSource ws = new WarpedSource(src.asVolatile().getSpimSource(), "Transformed_"+src.asVolatile().getSpimSource().getName());
		ws.updateTransform( xfm );
		ws.setIsTransformed(true);

		return ws;
	}
	
	public <S extends AbstractSpimData<?> > Element toXml( final S spimData, ThinPlateR2LogRSplineKernelTransform tps, final File xmlFileDirectory ) throws SpimDataException
	{
		System.out.println( xmlFileDirectory );

		Element root = super.toXml();
		if (spimData instanceof SpimData) {
			System.out.println( "SpimData");
			Element spimDataElem = (new XmlIoSpimData()).toXml((SpimData)spimData, xmlFileDirectory.getParentFile() );
			root.addContent( spimDataElem );
		} else if (spimData instanceof SpimDataMinimal) {
			System.out.println( "SpimDataMinimal");
			Element spimDataElem = (new XmlIoSpimDataMinimal()).toXml((SpimDataMinimal)spimData, xmlFileDirectory.getParentFile() );
			root.addContent( spimDataElem );
		}
		else
			return null;
	
		root.addContent( tpsIo.toXml( new TpsWrapper( tps ), xmlFileDirectory ));

		return root;
	}
	
	public void save( Element elem, final String xmlFilename ) throws SpimDataException
	{
		final File xmlFileDirectory = new File( xmlFilename ).getParentFile();
		final Document doc = new Document( elem );
		final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
		try
		{
			xout.output( doc, new FileOutputStream( xmlFilename ) );
		}
		catch ( final IOException e )
		{
			throw new SpimDataIOException( e );
		}
	}

}
