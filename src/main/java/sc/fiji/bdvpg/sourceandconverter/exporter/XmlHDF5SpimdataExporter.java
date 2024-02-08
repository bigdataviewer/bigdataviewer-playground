/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2024 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.bdvpg.sourceandconverter.exporter;

import bdv.export.ExportMipmapInfo;
import bdv.export.ExportScalePyramid;
import bdv.export.ProgressWriter;
import bdv.export.ProposeMipmaps;
import bdv.export.SubTaskProgressWriter;
import bdv.export.WriteSequenceToHdf5;
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
import mpicbg.spim.data.sequence.Tile;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Export a set of Sources into a new Xml/Hdf5 BDV dataset Mipmaps are
 * recomputed. Do not work with RGB images. Other pixel types are truncated to
 * their int value between 0 and 65535 Deeply copied from
 * https://github.com/bigdataviewer/bigdataviewer_fiji/blob/master/src/main/java/bdv/ij/ExportImagePlusPlugIn.java
 * https://github.com/tischi/bdv-utils/blob/master/src/main/java/de/embl/cba/bdv/utils/io/BdvRaiVolumeExport.java#L38
 * This export does not take advantage of potentially already computed mipmaps
 * TODO take advantage of this, whenever possible
 */

public class XmlHDF5SpimdataExporter implements Runnable {

	protected static final Logger logger = LoggerFactory.getLogger(
		XmlHDF5SpimdataExporter.class);

	final List<SourceAndConverter<?>> sources;

	final int nThreads;

	final int timePointBegin;

	final int timePointEnd;

	int scaleFactor;

	final int blockSizeX;

	final int blockSizeY;

	final int blockSizeZ;

	final int thresholdSizeForMipmap;

	final File xmlFile;

	final String entityType;

	public XmlHDF5SpimdataExporter(List<SourceAndConverter<?>> sources,
		String entityType, int nThreads, int timePointBegin, int timePointEnd,
		int scaleFactor, int blockSizeX, int blockSizeY, int blockSizeZ,
		int thresholdSizeForMipmap, File xmlFile)
	{
		this(sources, entityType, nThreads, timePointBegin, timePointEnd,
			scaleFactor, blockSizeX, blockSizeY, blockSizeZ, thresholdSizeForMipmap,
			xmlFile, (source, viewsetup) -> {});

	}

	public XmlHDF5SpimdataExporter(List<SourceAndConverter<?>> sources,
		String entityType, int nThreads, int timePointBegin, int timePointEnd,
		int scaleFactor, int blockSizeX, int blockSizeY, int blockSizeZ,
		int thresholdSizeForMipmap, File xmlFile,
		BiConsumer<SourceAndConverter<?>, BasicViewSetup> attributeAdder)
	{
		this.sources = sources;
		this.entityType = entityType;
		this.nThreads = nThreads;
		this.timePointBegin = timePointBegin;
		this.timePointEnd = timePointEnd;
		this.scaleFactor = scaleFactor;

		this.blockSizeX = blockSizeX;
		this.blockSizeY = blockSizeY;
		this.blockSizeZ = blockSizeZ;

		this.thresholdSizeForMipmap = thresholdSizeForMipmap;

		this.xmlFile = xmlFile;
		this.attributeAdder = attributeAdder;

	}

	AbstractSpimData<?> spimData;

	final BiConsumer<SourceAndConverter<?>, BasicViewSetup> attributeAdder;

	public void run() {

		// Gets Concrete SpimSource
		Map<Source<?>, Integer> idxSourceToSac = new HashMap<>();

		List<Source<UnsignedShortType>> srcs = sources.stream().map(
			SourceAndConverter::getSpimSource).map(
				SourceToUnsignedShortConverter::convertSource) // Convert To
																												// UnsignedShortType
																												// (limitation of
																												// current xml/hdf5
																												// implementation)
			.collect(Collectors.toList());

		for (int i = 0; i < srcs.size(); i++) {
			idxSourceToSac.put(srcs.get(i), i);
		}

		ImgLoaderFromSources<UnsignedShortType> imgLoader =
			new ImgLoaderFromSources<>(srcs);

		final int numTimepoints = this.timePointEnd - this.timePointBegin;

		final int numSetups = srcs.size();

		final ArrayList<TimePoint> timepoints = new ArrayList<>(numTimepoints);

		for (int t = timePointBegin; t < timePointEnd; ++t)
			timepoints.add(new TimePoint(t));

		final HashMap<Integer, BasicViewSetup> setups = new HashMap<>(numSetups);

		final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal(
			new TimePoints(timepoints), setups, imgLoader, null);

		Map<Integer, ExportMipmapInfo> perSetupExportMipmapInfo = new HashMap<>();

		int idx_current_src = 0;

		for (Source<?> src : srcs) {
			RandomAccessibleInterval<?> refRai = src.getSource(0, 0);

			if (true) {
				final VoxelDimensions voxelSize = src.getVoxelDimensions();
				long[] imgDims = new long[] { refRai.dimension(0), refRai.dimension(1),
					refRai.dimension(2) };
				final FinalDimensions imageSize = new FinalDimensions(imgDims);

				// propose mipmap settings
				final ExportMipmapInfo mipmapSettings;

				final BasicViewSetup basicviewsetup = new BasicViewSetup(
					idx_current_src, src.getName(), imageSize, voxelSize);

				if (true) {// ((imgDims[0] <= 2) || (imgDims[1] <= 2) || (imgDims[2] <=
										// 2))) {//||(autoMipMap==false)) {// automipmap fails if
										// one dimension is below or equal to 2
					int nLevels = 1;
					long maxDimension = Math.max(Math.max(imgDims[0], imgDims[1]),
						imgDims[2]);

					while (maxDimension > thresholdSizeForMipmap) {
						nLevels++;
						maxDimension /= scaleFactor + 1;
					}

					int[][] resolutions = new int[nLevels][3];
					int[][] subdivisions = new int[nLevels][3];

					for (int iMipMap = 0; iMipMap < nLevels; iMipMap++) {
						resolutions[iMipMap][0] = imgDims[0] <= 1 ? 1 : (int) Math.pow(
							scaleFactor, iMipMap);
						resolutions[iMipMap][1] = imgDims[1] <= 1 ? 1 : (int) Math.pow(
							scaleFactor, iMipMap);
						resolutions[iMipMap][2] = imgDims[2] <= 1 ? 1 : (int) Math.pow(
							scaleFactor, iMipMap);

						subdivisions[iMipMap][0] = (long) ((double) imgDims[0] /
							(double) resolutions[iMipMap][0]) > 1 ? blockSizeX : 1;
						subdivisions[iMipMap][1] = (long) ((double) imgDims[1] /
							(double) resolutions[iMipMap][1]) > 1 ? blockSizeY : 1;
						subdivisions[iMipMap][2] = (long) ((double) imgDims[2] /
							(double) resolutions[iMipMap][2]) > 1 ? blockSizeZ : 1;

						// 2D dimension = 0 fix
						subdivisions[iMipMap][0] = Math.max(1, subdivisions[iMipMap][0]);
						subdivisions[iMipMap][1] = Math.max(1, subdivisions[iMipMap][1]);
						subdivisions[iMipMap][2] = Math.max(1, subdivisions[iMipMap][2]);
					}

					mipmapSettings = new ExportMipmapInfo(resolutions, subdivisions);
				}
				else {
					// AutoMipmap
					if (basicviewsetup.getVoxelSize() == null) {
						logger.info("No voxel size specified in viewsetup " + basicviewsetup
							.getId());
						if (scaleFactor < 1) {
							logger.info("Scale factor below 1, using scale factor 4 instead");
							scaleFactor = 4;
						}
						int nLevels = 1;

						long maxDimension = Math.max(Math.max(imgDims[0], imgDims[1]),
							imgDims[2]);

						while (maxDimension > thresholdSizeForMipmap) {
							nLevels++;
							maxDimension /= scaleFactor + 1;
						}
						int[][] resolutions = new int[nLevels][3];
						int[][] subdivisions = new int[nLevels][3];

						for (int iMipMap = 0; iMipMap < nLevels; iMipMap++) {
							resolutions[iMipMap][0] = imgDims[0] <= 1 ? 1 : (int) Math.pow(
								scaleFactor, iMipMap);
							resolutions[iMipMap][1] = imgDims[1] <= 1 ? 1 : (int) Math.pow(
								scaleFactor, iMipMap);
							resolutions[iMipMap][2] = imgDims[2] <= 1 ? 1 : (int) Math.pow(
								scaleFactor, iMipMap);

							subdivisions[iMipMap][0] = (long) ((double) imgDims[0] /
								(double) resolutions[iMipMap][0]) > 1 ? blockSizeX : 1;
							subdivisions[iMipMap][1] = (long) ((double) imgDims[1] /
								(double) resolutions[iMipMap][1]) > 1 ? blockSizeY : 1;
							subdivisions[iMipMap][2] = (long) ((double) imgDims[2] /
								(double) resolutions[iMipMap][2]) > 1 ? blockSizeZ : 1;

							// 2D dimension = 0 fix
							subdivisions[iMipMap][0] = Math.max(1, subdivisions[iMipMap][0]);
							subdivisions[iMipMap][1] = Math.max(1, subdivisions[iMipMap][1]);
							subdivisions[iMipMap][2] = Math.max(1, subdivisions[iMipMap][2]);
						}

						mipmapSettings = new ExportMipmapInfo(resolutions, subdivisions);
					}
					else {
						mipmapSettings = ProposeMipmaps.proposeMipmaps(basicviewsetup);
					}
				}

				if (entityType.equals("Channel")) {
					basicviewsetup.setAttribute(new Channel(idx_current_src + 1));
				}
				else if (entityType.equals("Tile")) {
					basicviewsetup.setAttribute(new Tile(idx_current_src + 1));
				}

				SourceAndConverter<?> sac = sources.get(idxSourceToSac.get(src));

				attributeAdder.accept(sac, basicviewsetup);

				setups.put(idx_current_src, basicviewsetup); // Hum hum, order according
																											// to hashmap size TODO
																											// check

				final ExportMipmapInfo mipmapInfo = new ExportMipmapInfo(mipmapSettings
					.getExportResolutions(), mipmapSettings.getSubdivisions());

				perSetupExportMipmapInfo.put(basicviewsetup.getId(), mipmapInfo);

				idx_current_src = idx_current_src + 1;
			}
		}
		// ---------------------- End of setup handling

		final int numCellCreatorThreads = Math.max(1, nThreads - 1);

		final ExportScalePyramid.LoopbackHeuristic loopbackHeuristic =
			new ExportScalePyramid.DefaultLoopbackHeuristic();

		final ProgressWriter progressWriter = new ProgressWriterIJ();
		logger.info("Starting export...");

		final ExportScalePyramid.AfterEachPlane afterEachPlane = usedLoopBack -> {

		};

		final ArrayList<Partition> partitions;
		partitions = null;

		String seqFilename = xmlFile.getAbsolutePath();// .getParent();
		if (!seqFilename.endsWith(".xml")) seqFilename += ".xml";
		final File seqFile = new File(seqFilename);
		final File parent = seqFile.getParentFile();
		if (parent == null || !parent.exists() || !parent.isDirectory()) {
			logger.error("Invalid export filename " + seqFilename);
		}
		final String hdf5Filename = seqFilename.substring(0, seqFilename.length() -
			4) + ".h5";
		final File hdf5File = new File(hdf5Filename);
		boolean deflate = true;
		{
			WriteSequenceToHdf5.writeHdf5File(seq, perSetupExportMipmapInfo, deflate,
				hdf5File, loopbackHeuristic, afterEachPlane, numCellCreatorThreads,
				new SubTaskProgressWriter(progressWriter, 0, 0.95));
		}

		// write xml sequence description
		final SequenceDescriptionMinimal seqh5 = new SequenceDescriptionMinimal(seq,
			null);
		final Hdf5ImageLoader hdf5Loader = new Hdf5ImageLoader(hdf5File, partitions,
			seqh5, false);
		seqh5.setImgLoader(hdf5Loader);

		final ArrayList<ViewRegistration> registrations = new ArrayList<>();
		for (int t = 0; t < numTimepoints; ++t)
			for (int s = 0; s < numSetups; ++s)
				registrations.add(new ViewRegistration(t, s, getSrcTransform(srcs.get(
					s), t, 0)));

		final File basePath = seqFile.getParentFile();

		spimData = new SpimDataMinimal(basePath, seqh5, new ViewRegistrations(
			registrations));

		try {
			new XmlIoSpimDataMinimal().save((SpimDataMinimal) spimData, seqFile
				.getAbsolutePath());
			progressWriter.setProgress(1.0);
		}
		catch (final Exception e) {
			throw new RuntimeException(e);
		}

		logger.info("Done!");

	}

	public AbstractSpimData<?> get() {
		return spimData;
	}

	public File getFile() {
		return xmlFile;
	}

	static public AffineTransform3D getSrcTransform(Source<?> src, int timepoint,
		int level)
	{
		AffineTransform3D at = new AffineTransform3D();
		src.getSourceTransform(timepoint, level, at);
		return at;
	}

}
