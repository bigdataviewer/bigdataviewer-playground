package sc.fiji.bdvpg.sourceandconverter.exporter;

import bdv.export.*;
import bdv.ij.util.ProgressWriterIJ;
import bdv.img.hdf5.Hdf5ImageLoader;
import bdv.img.hdf5.Partition;
import bdv.spimdata.SequenceDescriptionMinimal;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.util.SourceToUnsignedShortConverter;
import bdv.util.sourceimageloader.ImgLoaderFromSources;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.display.ColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.omg.CORBA.INTERNAL;
import spimdata.util.DisplaySettings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 *
 * Export a set of Sources into a new Xml/Hdf5 bdv dataset
 *
 * Mipmaps are recomputed. Do not work with RGB images.
 * Other pixel types are truncated to their int value between 0 and 65535
 *
 * Deeply copied from
 * https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusPlugIn.java
 *
 * https://github.com/tischi/bdv-utils/blob/master/src/main/java/de/embl/cba/bdv/utils/io/BdvRaiVolumeExport.java#L38
 *
 * This export does not take advantage of potentially already computed mipmaps TODO take advantage of this, whenever possible
 *
 */

public class XmlHDF5SpimdataExporter implements Runnable {

    List<SourceAndConverter> sources;

    int nThreads;

    int timePointBegin;

    int timePointEnd;

    int scaleFactor;

    int blockSizeX;

    int blockSizeY;

    int blockSizeZ;

    File xmlFile;

    public XmlHDF5SpimdataExporter(List<SourceAndConverter> sources,
                                   int nThreads,
                                   int timePointBegin,
                                   int timePointEnd,
                                   int scaleFactor,
                                   int blockSizeX,
                                   int blockSizeY,
                                   int blockSizeZ,
                                   File xmlFile) {
        this.sources = sources;
        this.nThreads = nThreads;
        this.timePointBegin = timePointBegin;
        this.timePointEnd = timePointEnd;
        this.scaleFactor = scaleFactor;

        this.blockSizeX = blockSizeX;
        this.blockSizeY = blockSizeY;
        this.blockSizeZ = blockSizeZ;

        this.xmlFile = xmlFile;

    }

    AbstractSpimData spimData;

    public void run() {

        // Gets Concrete SpimSource
        List<Source> srcs = sources.stream().map(sac -> sac.getSpimSource()).collect(Collectors.toList());
        Map<Source, Integer> idxSourceToSac = new HashMap<>();

        // Convert To UnsignedShortType (limitation of current xml/hdf5 implementation)
        srcs.replaceAll(src -> SourceToUnsignedShortConverter.convertSource(src));

        for (int i=0;i<srcs.size();i++) {
            idxSourceToSac.put(srcs.get(i), i);
        }

        ImgLoaderFromSources<?> imgLoader = new ImgLoaderFromSources(srcs);

        final int numTimepoints = this.timePointEnd - this.timePointBegin;

        final int numSetups = srcs.size();

        final ArrayList<TimePoint> timepoints = new ArrayList<>(numTimepoints);

        for (int t = timePointBegin; t < timePointEnd; ++t)
            timepoints.add(new TimePoint(t));

        final HashMap<Integer, BasicViewSetup> setups = new HashMap<>(numSetups);

        final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal(new TimePoints(timepoints), setups, imgLoader, null);

        Map<Integer, ExportMipmapInfo> perSetupExportMipmapInfo = new HashMap<>();

        int idx_current_src = 0;


        for (Source<?> src: srcs) {
            RandomAccessibleInterval<?> refRai = src.getSource(0, 0);

            if (true) {
                final VoxelDimensions voxelSize = src.getVoxelDimensions();
                long[] imgDims = new long[]{refRai.dimension(0), refRai.dimension(1), refRai.dimension(2)};
                final FinalDimensions imageSize = new FinalDimensions(imgDims);

                // propose mipmap settings
                final ExportMipmapInfo mipmapSettings;

                final BasicViewSetup basicviewsetup = new BasicViewSetup(idx_current_src, src.getName(), imageSize, voxelSize);

                if (((imgDims[0] <= 2) || (imgDims[1] <= 2) || (imgDims[2] <= 2))) {//||(autoMipMap==false)) {// automipmap fails if one dimension is below or equal to 2
                    int nLevels = 1;
                    long maxDimension = Math.max(Math.max(imgDims[0], imgDims[1]), imgDims[2]);
                    while (maxDimension > 512) {
                        nLevels++;
                        maxDimension /= scaleFactor + 1;
                    }
                    int[][] resolutions = new int[nLevels][3];
                    int[][] subdivisions = new int[nLevels][3];

                    for (int iMipMap = 0; iMipMap < nLevels; iMipMap++) {
                        resolutions[iMipMap][0] = imgDims[0]<=1?1:(int) Math.pow(scaleFactor, iMipMap);
                        resolutions[iMipMap][1] = imgDims[1]<=1?1:(int) Math.pow(scaleFactor, iMipMap);
                        resolutions[iMipMap][2] = imgDims[2]<=1?1:(int) Math.pow(scaleFactor, iMipMap);

                        subdivisions[iMipMap][0] = (long) ((double) imgDims[0] / (double) resolutions[iMipMap][0]) > 1 ? blockSizeX : 1;
                        subdivisions[iMipMap][1] = (long) ((double) imgDims[1] / (double) resolutions[iMipMap][1]) > 1 ? blockSizeY : 1;
                        subdivisions[iMipMap][2] = (long) ((double) imgDims[2] / (double) resolutions[iMipMap][2]) > 1 ? blockSizeZ : 1;

                        // 2D dimension = 0 fix
                        subdivisions[iMipMap][0] = Math.max(1,subdivisions[iMipMap][0]);
                        subdivisions[iMipMap][1] = Math.max(1,subdivisions[iMipMap][1]);
                        subdivisions[iMipMap][2] = Math.max(1,subdivisions[iMipMap][2]);
                    }

                    mipmapSettings = new ExportMipmapInfo(resolutions, subdivisions);
                } else {
                    // AutoMipmap
                    if (basicviewsetup.getVoxelSize()==null) {
                        System.out.println("No voxel size specified!");
                        if (scaleFactor<1) {
                            System.out.println("Using scale factor = 4");
                            scaleFactor=4;
                        }
                        int nLevels = 1;
                        long maxDimension = Math.max(Math.max(imgDims[0], imgDims[1]), imgDims[2]);
                        while (maxDimension > 512) {
                            nLevels++;
                            maxDimension /= scaleFactor + 1;
                        }
                        int[][] resolutions = new int[nLevels][3];
                        int[][] subdivisions = new int[nLevels][3];

                        for (int iMipMap = 0; iMipMap < nLevels; iMipMap++) {
                            resolutions[iMipMap][0] = imgDims[0]<=1?1:(int) Math.pow(scaleFactor, iMipMap);
                            resolutions[iMipMap][1] = imgDims[1]<=1?1:(int) Math.pow(scaleFactor, iMipMap);
                            resolutions[iMipMap][2] = imgDims[2]<=1?1:(int) Math.pow(scaleFactor, iMipMap);

                            subdivisions[iMipMap][0] = (long) ((double) imgDims[0] / (double) resolutions[iMipMap][0]) > 1 ? blockSizeX : 1;
                            subdivisions[iMipMap][1] = (long) ((double) imgDims[1] / (double) resolutions[iMipMap][1]) > 1 ? blockSizeY : 1;
                            subdivisions[iMipMap][2] = (long) ((double) imgDims[2] / (double) resolutions[iMipMap][2]) > 1 ? blockSizeZ : 1;

                            // 2D dimension = 0 fix
                            subdivisions[iMipMap][0] = Math.max(1,subdivisions[iMipMap][0]);
                            subdivisions[iMipMap][1] = Math.max(1,subdivisions[iMipMap][1]);
                            subdivisions[iMipMap][2] = Math.max(1,subdivisions[iMipMap][2]);
                        }

                        mipmapSettings = new ExportMipmapInfo(resolutions, subdivisions);
                    } else {
                        mipmapSettings = ProposeMipmaps.proposeMipmaps(basicviewsetup);
                    }
                }

                basicviewsetup.setAttribute(new Channel(1));

                DisplaySettings ds = new DisplaySettings(idx_current_src);
                SourceAndConverter sac = sources.get(idxSourceToSac.get(src));
                // Color + min max
                if (sac.getConverter() instanceof ColorConverter) {
                    ColorConverter cc = (ColorConverter) sac.getConverter();
                    ds.setName("vs:" + idx_current_src);
                    int colorCode = cc.getColor().get();
                    ds.color = new int[]{
                            ARGBType.red(colorCode),
                            ARGBType.green(colorCode),
                            ARGBType.blue(colorCode),
                            ARGBType.alpha(colorCode)};
                    ds.min = cc.getMin();
                    ds.max = cc.getMax();
                    ds.isSet = true;
                } else {
                    System.err.println("Converter is of class :"+sac.getConverter().getClass().getSimpleName()+" -> Display settings cannot be stored.");
                }

                basicviewsetup.setAttribute(ds);

                setups.put(idx_current_src, basicviewsetup); // Hum hum, order according to hashmap size TODO check

                final ExportMipmapInfo mipmapInfo =
                        new ExportMipmapInfo(
                                mipmapSettings.getExportResolutions(),
                                mipmapSettings.getSubdivisions());

                perSetupExportMipmapInfo.put( basicviewsetup.getId(), mipmapInfo);

                idx_current_src = idx_current_src+1;
            }
        }
        //---------------------- End of setup handling


        final int numCellCreatorThreads = Math.max( 1, nThreads - 1 );

        final ExportScalePyramid.LoopbackHeuristic loopbackHeuristic = new ExportScalePyramid.DefaultLoopbackHeuristic();

        final ExportScalePyramid.AfterEachPlane afterEachPlane = usedLoopBack -> { };

        final ArrayList<Partition> partitions;
        partitions = null;

        final ProgressWriter progressWriter = new ProgressWriterIJ();
        System.out.println( "Starting export..." );

        String seqFilename = xmlFile.getAbsolutePath();//.getParent();
        if ( !seqFilename.endsWith( ".xml" ) )
            seqFilename += ".xml";
        final File seqFile = new File( seqFilename );
        final File parent = seqFile.getParentFile();
        if ( parent == null || !parent.exists() || !parent.isDirectory() )
        {
            System.err.println( "Invalid export filename " + seqFilename );
        }
        final String hdf5Filename = seqFilename.substring( 0, seqFilename.length() - 4 ) + ".h5";
        final File hdf5File = new File( hdf5Filename );
        boolean deflate = false;
        {
            WriteSequenceToHdf5.writeHdf5File( seq, perSetupExportMipmapInfo, deflate, hdf5File, loopbackHeuristic, afterEachPlane, numCellCreatorThreads, new SubTaskProgressWriter( progressWriter, 0, 0.95 ) );
        }

        // write xml sequence description
        final SequenceDescriptionMinimal seqh5 = new SequenceDescriptionMinimal( seq, null );
        final Hdf5ImageLoader hdf5Loader = new Hdf5ImageLoader( hdf5File, partitions, seqh5, false );
        seqh5.setImgLoader(hdf5Loader);

        final ArrayList<ViewRegistration> registrations = new ArrayList<>();
        for ( int t = 0; t < numTimepoints; ++t )
            for ( int s = 0; s < numSetups; ++s )
                registrations.add( new ViewRegistration( t, s, getSrcTransform(srcs.get(s),t,0)));

        final File basePath = seqFile.getParentFile();

        spimData = new SpimDataMinimal( basePath, seqh5, new ViewRegistrations( registrations ) );

        try
        {
            new XmlIoSpimDataMinimal().save( (SpimDataMinimal) spimData, seqFile.getAbsolutePath() );
            progressWriter.setProgress( 1.0 );
        }
        catch ( final Exception e )
        {
            throw new RuntimeException( e );
        }

        System.out.println( "Done!" );

    }

    public AbstractSpimData get() {
        return spimData;
    }

    public File getFile() {
        return xmlFile;
    }

    static public AffineTransform3D getSrcTransform(Source< ? > src, int timepoint, int level) {
        AffineTransform3D at = new AffineTransform3D();
        src.getSourceTransform(timepoint, level,at);
        return at;
    }

}
