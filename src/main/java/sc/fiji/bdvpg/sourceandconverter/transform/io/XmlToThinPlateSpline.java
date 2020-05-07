package sc.fiji.bdvpg.sourceandconverter.transform.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.codec.binary.Base64;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import bigwarp.landmarks.LandmarkTableModel;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.SpimDataIOException;
import mpicbg.spim.data.generic.base.XmlIoSingleton;

public class XmlToThinPlateSpline extends XmlIoSingleton< TpsWrapper >
{
	
	public static final String TPS_TAG = "ThinPlateSplineTransform";
	public static final String DATA_TAG = "tpsdata";

	public XmlToThinPlateSpline()
	{
		super(TPS_TAG, TpsWrapper.class);
	}

	public static void main(String[] args) throws IOException, SpimDataException
	{
		XmlToThinPlateSpline io = new XmlToThinPlateSpline();
		System.out.println( io );
		
		LandmarkTableModel ltm = new LandmarkTableModel( 3 );
		ltm.load( new File( "/home/john/tmp/t1-head_landmarks.csv" ));
		ltm.getTransform();

		File f = new File( "/home/john/tmp/warpedSourceXml/tps.xml" );

		Element elem = io.toXml( new TpsWrapper( ltm.getTransform() ), f );

		final Document doc = new Document( elem );
		final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
		try
		{
			xout.output( doc, new FileOutputStream( f ) );
		}
		catch ( final IOException e )
		{
			throw new SpimDataIOException( e );
		}

		TpsWrapper tps = io.load( f.getCanonicalPath() );
		ThinPlateR2LogRSplineKernelTransform tpsXfm = tps.get();
		System.out.println( tpsXfm );

	}
	
	public TpsWrapper load( final String xmlFilename ) throws SpimDataException
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

		if ( root.getName() != TPS_TAG )
			throw new RuntimeException( "expected <" + TPS_TAG + "> root element. wrong file?" );

		return fromXml( root, new File( xmlFilename ) );
	}

	public void save( final Element root, final File xmlFile ) throws FileNotFoundException, IOException
	{
		final Document doc = new Document( root );
		final XMLOutputter xout = new XMLOutputter( Format.getPrettyFormat() );
		xout.output( doc, new FileOutputStream( xmlFile ) );
	}

	public TpsWrapper fromXml( final Element root, final File xmlFile )
	{
		String dataString = root.getAttributeValue(DATA_TAG);
		TpsWrapper wrapper = new TpsWrapper();
		wrapper.set( init( dataString )); 
		return wrapper;
	}

	public Element toXml( final TpsWrapper tps, final File xmlFileDirectory ) throws SpimDataException
	{
		final Element root = super.toXml();
		root.setAttribute( DATA_TAG, toDataString(tps.get() ));
		return root;
	}
	
	/*
	 *  methods below were stolen from trakem2-transform 'ThinPlateSplineTransform'
	 */
	static private String encodeBase64(final double[] src) {
		final byte[] bytes = new byte[src.length * 8];
		for (int i = 0, j = -1; i < src.length; ++i) {
			final long bits = Double.doubleToLongBits(src[i]);
			bytes[++j] = (byte)(bits >> 56);
			bytes[++j] = (byte)((bits >> 48) & 0xffL);
			bytes[++j] = (byte)((bits >> 40) & 0xffL);
			bytes[++j] = (byte)((bits >> 32) & 0xffL);
			bytes[++j] = (byte)((bits >> 24) & 0xffL);
			bytes[++j] = (byte)((bits >> 16) & 0xffL);
			bytes[++j] = (byte)((bits >> 8) & 0xffL);
			bytes[++j] = (byte)(bits & 0xffL);
		}
		final Deflater deflater = new Deflater();
		deflater.setInput(bytes);
		deflater.finish();
		final byte[] zipped = new byte[bytes.length];
		final int n = deflater.deflate(zipped);
		if (n == bytes.length)
			return '@' + Base64.encodeBase64String(bytes);
		else
			return Base64.encodeBase64String(Arrays.copyOf(zipped, n));
	}

	static private double[] decodeBase64(final String src, final int n)
			throws DataFormatException {
		final byte[] bytes;
		if (src.charAt(0) == '@') {
			bytes = Base64.decodeBase64(src.substring(1));
		} else {
			bytes = new byte[n * 8];
			final byte[] zipped = Base64.decodeBase64(src);
			final Inflater inflater = new Inflater();
			inflater.setInput(zipped, 0, zipped.length);

			inflater.inflate(bytes);
			inflater.end();
		}
		final double[] doubles = new double[n];
		for (int i = 0, j = -1; i < n; ++i) {
			long bits = 0L;
			bits |= (bytes[++j] & 0xffL) << 56;
			bits |= (bytes[++j] & 0xffL) << 48;
			bits |= (bytes[++j] & 0xffL) << 40;
			bits |= (bytes[++j] & 0xffL) << 32;
			bits |= (bytes[++j] & 0xffL) << 24;
			bits |= (bytes[++j] & 0xffL) << 16;
			bits |= (bytes[++j] & 0xffL) << 8;
			bits |= bytes[++j] & 0xffL;
			doubles[i] = Double.longBitsToDouble(bits);
		}
		return doubles;
	}

	public ThinPlateR2LogRSplineKernelTransform init(final String data) throws NumberFormatException {

		final String[] fields = data.split("\\s+");

		int i = 0;

		final int ndims = Integer.parseInt(fields[++i]);
		final int nLm = Integer.parseInt(fields[++i]);

		double[][] aMtx = null;
		double[] bVec = null;
		if (fields[i + 1].equals("null")) {
			// System.out.println(" No affines " );
			++i;
		} else {
			aMtx = new double[ndims][ndims];
			bVec = new double[ndims];

			final double[] values;
			try {
				values = decodeBase64(fields[++i], ndims * ndims + ndims);
			} catch (final DataFormatException e) {
				throw new NumberFormatException("Failed decoding affine matrix.");
			}
			int l = -1;
			for (int k = 0; k < ndims; k++)
				for (int j = 0; j < ndims; j++) {
					aMtx[k][j] = values[++l];
				}
			for (int j = 0; j < ndims; j++) {
				bVec[j] = values[++l];
			}
		}

		final double[] values;
		try {
			values = decodeBase64(fields[++i], 2 * nLm * ndims);
		} catch (final DataFormatException e) {
			throw new NumberFormatException("Failed decoding landmarks and weights.");
		}
		int k = -1;

		// parse control points
		final double[][] srcPts = new double[ndims][nLm];
		for (int l = 0; l < nLm; l++)
			for (int d = 0; d < ndims; d++) {
				srcPts[d][l] = values[++k];
			}

		// parse control point coordinates
		int m = -1;
		final double[] dMtxDat = new double[nLm * ndims];
		for (int l = 0; l < nLm; l++)
			for (int d = 0; d < ndims; d++) {
				dMtxDat[++m] = values[++k];
			}

		return new ThinPlateR2LogRSplineKernelTransform(srcPts, aMtx, bVec, dMtxDat);
	}

	private final String toDataString( final ThinPlateR2LogRSplineKernelTransform tps )
	{
		final StringBuilder data = new StringBuilder();
		data.append("ThinPlateSplineR2LogR");

		final int ndims = tps.getNumDims();
		final int nLm = tps.getNumLandmarks();

		data.append(' ').append(ndims); // dimensions
		data.append(' ').append(nLm); // landmarks

		if (tps.getAffine() == null) {
			data.append(' ').append("null"); // aMatrix
		} else {
			final double[][] aMtx = tps.getAffine();
			final double[] bVec = tps.getTranslation();

			final double[] buffer = new double[ndims * ndims + ndims];
			int k = -1;
			for (int i = 0; i < ndims; i++)
				for (int j = 0; j < ndims; j++) {
					buffer[++k] = aMtx[i][j];
				}
			for (int i = 0; i < ndims; i++) {
				buffer[++k] = bVec[i];
			}

			data.append(' ').append(encodeBase64(buffer));
		}

		final double[][] srcPts = tps.getSourceLandmarks();
		final double[] dMtxDat = tps.getKnotWeights();

		final double[] buffer = new double[2 * nLm * ndims];
		int k = -1;
		for (int l = 0; l < nLm; l++)
			for (int d = 0; d < ndims; d++)
				buffer[++k] = srcPts[d][l];

		for (int i = 0; i < ndims * nLm; i++) {
			buffer[++k] = dMtxDat[i];
		}
		data.append(' ').append(encodeBase64(buffer));
		return data.toString();
	}

}
