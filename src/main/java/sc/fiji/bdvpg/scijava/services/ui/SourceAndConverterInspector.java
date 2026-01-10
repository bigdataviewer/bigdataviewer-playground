/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.scijava.services.ui;

import bdv.AbstractSpimSource;
import bdv.img.WarpedSource;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.base.NamedEntity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.cache.AbstractGlobalCache;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class SourceAndConverterInspector {

	protected static final Logger logger = LoggerFactory.getLogger(
		SourceAndConverterInspector.class);

	/**
	 * Appends all the metadata of a SourceAndConverter into a tree structure
	 * 
	 * @param parent node below which the metadata nodes will be added
	 * @param sac source for which the metadata are fetched
	 */
	public static void appendMetadata(DefaultMutableTreeNode parent,
		SourceAndConverter<?> sac)
	{
		SourceAndConverterServices.getSourceAndConverterService().getMetadataKeys(
			sac).forEach(k -> {
				DefaultMutableTreeNode nodeMetaKey = new DefaultMutableTreeNode(k);
				parent.add(nodeMetaKey);
				DefaultMutableTreeNode nodeMetaValue = new DefaultMutableTreeNode(
					SourceAndConverterServices.getSourceAndConverterService().getMetadata(
						sac, k));
				nodeMetaKey.add(nodeMetaValue);
			});
	}

	/**
	 * Inspects recursively a SourceAndConverter object Properly inspected
	 * bdv.viewer.Source class: - SpimSource {@link AbstractSpimSource} -
	 * WarpedSource {@link WarpedSource} - TransformedSource
	 * {@link TransformedSource} - ResampledSource {@link ResampledSource}
	 *
	 * @param parent parent node
	 * @param sac source
	 * @param registerIntermediateSources if you want the intermediate sources
	 *          registered in the source and converter service
	 * @param sourceAndConverterService source service
	 * @return the set of sources that were necessary to build the sac (including
	 *         itself)
	 */
	public static Set<SourceAndConverter<?>> appendInspectorResult(
		DefaultMutableTreeNode parent, SourceAndConverter<?> sac,
		ISourceAndConverterService sourceAndConverterService,
		boolean registerIntermediateSources)
	{
		Set<SourceAndConverter<?>> subSources = new HashSet<>();
		subSources.add(sac);

		if (sac.getSpimSource() instanceof TransformedSource) {
			DefaultMutableTreeNode nodeTransformedSource = new DefaultMutableTreeNode(
				"Transformed Source");
			parent.add(nodeTransformedSource);
			TransformedSource<?> source = (TransformedSource<?>) sac.getSpimSource();

			if (!sourceAndConverterService.getSourceAndConvertersFromSource(source
                    .getWrappedSource()).isEmpty())
			{
				// at least a SourceAndConverter already exists for this source
				sourceAndConverterService.getSourceAndConvertersFromSource(source
					.getWrappedSource()).forEach((src) -> {
						DefaultMutableTreeNode wrappedSourceNode =
							new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
						nodeTransformedSource.add(wrappedSourceNode);
						subSources.addAll(appendInspectorResult(wrappedSourceNode, src,
							sourceAndConverterService, registerIntermediateSources));
					});
			}
			else {
				// no source and converter exist for this source : creates it
				SourceAndConverter<?> src = SourceAndConverterHelper
					.createSourceAndConverter(source.getWrappedSource());
				if (registerIntermediateSources) {
					sourceAndConverterService.register(src);
				}
				DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(
					new RenamableSourceAndConverter(src));
				nodeTransformedSource.add(wrappedSourceNode);
				subSources.addAll(appendInspectorResult(wrappedSourceNode, src,
					sourceAndConverterService, registerIntermediateSources));
			}

			DefaultMutableTreeNode nodeAffineTransformGetter =
				new DefaultMutableTreeNode(new Supplier<AffineTransform3D>()
				{

					public AffineTransform3D get() {
						AffineTransform3D at3D = new AffineTransform3D();
						source.getFixedTransform(at3D);
						return at3D;
					}

					public String toString() {
						return "AffineTransform[" + source.getName() + "]";
					}
				});

			nodeTransformedSource.add(nodeAffineTransformGetter);
			appendMetadata(nodeTransformedSource, sac);
		}

		if (sac.getSpimSource() instanceof WarpedSource) {
			DefaultMutableTreeNode nodeWarpedSource = new DefaultMutableTreeNode(
				"Warped Source");
			parent.add(nodeWarpedSource);
			WarpedSource<?> source = (WarpedSource<?>) sac.getSpimSource();

			if (!sourceAndConverterService.getSourceAndConvertersFromSource(source
                    .getWrappedSource()).isEmpty())
			{
				// at least a SourceAndConverter already exists for this source
				sourceAndConverterService.getSourceAndConvertersFromSource(source
					.getWrappedSource()).forEach((src) -> {
						DefaultMutableTreeNode wrappedSourceNode =
							new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
						nodeWarpedSource.add(wrappedSourceNode);
						subSources.addAll(appendInspectorResult(wrappedSourceNode, src,
							sourceAndConverterService, registerIntermediateSources));
					});
			}
			else {
				// no source and converter exist for this source : creates it
				SourceAndConverter<?> src = SourceAndConverterHelper
					.createSourceAndConverter(source.getWrappedSource());
				if (registerIntermediateSources) {
					sourceAndConverterService.register(src);
				}
				DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(
					new RenamableSourceAndConverter(src));
				nodeWarpedSource.add(wrappedSourceNode);
				subSources.addAll(appendInspectorResult(wrappedSourceNode, src,
					sourceAndConverterService, registerIntermediateSources));
			}
			DefaultMutableTreeNode nodeRealTransformGetter =
				new DefaultMutableTreeNode(new Supplier<RealTransform>()
				{

					public RealTransform get() {
						return source.getTransform();
					}

					public String toString() {
						return "RealTransform[" + source.getName() + "]";
					}
				});
			nodeWarpedSource.add(nodeRealTransformGetter);
			appendMetadata(nodeWarpedSource, sac);
		}

		if (sac.getSpimSource() instanceof ResampledSource) {
			DefaultMutableTreeNode nodeResampledSource = new DefaultMutableTreeNode(
				"Resampled Source");
			parent.add(nodeResampledSource);
			ResampledSource<?> source = (ResampledSource<?>) sac.getSpimSource();

			DefaultMutableTreeNode nodeOrigin = new DefaultMutableTreeNode("Origin");
			nodeResampledSource.add(nodeOrigin);

			if (!sourceAndConverterService.getSourceAndConvertersFromSource(source
                    .getOriginalSource()).isEmpty())
			{
				// at least a SourceAndConverter already exists for this source
				sourceAndConverterService.getSourceAndConvertersFromSource(source
					.getOriginalSource()).forEach((src) -> {
						DefaultMutableTreeNode wrappedSourceNode =
							new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
						nodeOrigin.add(wrappedSourceNode);
						subSources.addAll(appendInspectorResult(wrappedSourceNode, src,
							sourceAndConverterService, registerIntermediateSources));
					});
			}
			else {
				// no source and converter exist for this source : creates it
				SourceAndConverter<?> src = SourceAndConverterHelper
					.createSourceAndConverter(source.getOriginalSource());
				if (registerIntermediateSources) {
					sourceAndConverterService.register(src);
				}
				DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(
					new RenamableSourceAndConverter(src));
				nodeOrigin.add(wrappedSourceNode);
				subSources.addAll(appendInspectorResult(wrappedSourceNode, src,
					sourceAndConverterService, registerIntermediateSources));
			}

			DefaultMutableTreeNode nodeResampler = new DefaultMutableTreeNode(
				"Sampler Model");
			nodeResampledSource.add(nodeResampler);

			if (!sourceAndConverterService.getSourceAndConvertersFromSource(source
                    .getModelResamplerSource()).isEmpty())
			{
				// at least a SourceAndConverter already exists for this source
				sourceAndConverterService.getSourceAndConvertersFromSource(source
					.getModelResamplerSource()).forEach((src) -> {
						DefaultMutableTreeNode wrappedSourceNode =
							new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
						nodeResampler.add(wrappedSourceNode);
						subSources.addAll(appendInspectorResult(wrappedSourceNode, src,
							sourceAndConverterService, registerIntermediateSources));
					});
			}
			else {
				// no source and converter exist for this source : creates it
				SourceAndConverter<?> src = SourceAndConverterHelper
					.createSourceAndConverter(source.getModelResamplerSource());
				if (registerIntermediateSources) {
					sourceAndConverterService.register(src);
				}
				DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(
					new RenamableSourceAndConverter(src));
				nodeResampler.add(wrappedSourceNode);
				subSources.addAll(appendInspectorResult(wrappedSourceNode, src,
					sourceAndConverterService, registerIntermediateSources));
			}
			appendMetadata(nodeResampledSource, sac);
		}

		if (sac.getSpimSource() instanceof AbstractSpimSource) {
			DefaultMutableTreeNode nodeSpimSource = new DefaultMutableTreeNode(
				"Spim Source");
			parent.add(nodeSpimSource);

			// Add detailed SpimData information if available
			appendSpimDataInfo(nodeSpimSource, sac, sourceAndConverterService);

			// Add general metadata
			appendMetadata(nodeSpimSource, sac);
		}

		return subSources;
	}

	/**
	 * Returns the root {@link SourceAndConverter} in the sense that it finds the
	 * original source at the root of this source
	 * <p>
	 * for {@link ResampledSource} the model is ignored
	 * <p>
	 * it should fall either on an {@link AbstractSpimSource} or on something
	 * unhandled or unknown
	 * <p>
	 * it shouldn't fall on a {@link ResampledSource} nor on a
	 * {@link TransformedSource} nor on a {@link WarpedSource} because these three
	 * class are wrappers
	 *
	 * @param sac source to check
	 * @param sacService source and converter service, useful to find the root
	 * @return root source and converter object
	 */
	public static SourceAndConverter<?> getRootSourceAndConverter(
		SourceAndConverter<?> sac, SourceAndConverterService sacService)
	{
		return getListToRootSourceAndConverter(sac, sacService).getLast();
	}

	/**
	 * @param sac the source to investigate
	 * @return the root source
	 */
	public static SourceAndConverter<?> getRootSourceAndConverter(
		SourceAndConverter<?> sac)
	{
		return getListToRootSourceAndConverter(sac,
			(SourceAndConverterService) SourceAndConverterServices
				.getSourceAndConverterService()).getLast();
	}

	/**
	 * @param source the source to investigate
	 * @return the root source
	 */
	public static SourceAndConverter<?> getRootSourceAndConverter(
		Source<?> source)
	{
		SourceAndConverter<?> sac;
		List<SourceAndConverter<?>> sacs = SourceAndConverterServices
			.getSourceAndConverterService().getSourceAndConvertersFromSource(source);
		if (sacs.isEmpty()) {
			sac = SourceAndConverterHelper.createSourceAndConverter(source);
		}
		else {
			sac = sacs.get(0);
		}
		return getListToRootSourceAndConverter(sac,
			(SourceAndConverterService) SourceAndConverterServices
				.getSourceAndConverterService()).getLast();
	}

	/**
	 * TODO
	 * 
	 * @param sac TODO
	 * @param sacService TODO
	 * @return TODO
	 */
	public static LinkedList<SourceAndConverter<?>>
		getListToRootSourceAndConverter(SourceAndConverter<?> sac,
			SourceAndConverterService sacService)
	{

		LinkedList<SourceAndConverter<?>> chain = new LinkedList<>();

		DefaultMutableTreeNode nodeSac = new DefaultMutableTreeNode(
			new RenamableSourceAndConverter(sac));

		chain.add(sac);

		appendInspectorResult(nodeSac, sac, sacService, false);

		DefaultMutableTreeNode current = nodeSac;

		while (current.getChildCount() > 0) {
			current = (DefaultMutableTreeNode) current.getFirstChild();
			if (current.getUserObject() instanceof RenamableSourceAndConverter) {
				chain.add(((RenamableSourceAndConverter) (current
					.getUserObject())).sac);
			}
			else {
				logger.debug("No renamable source found");
				logger.debug(
					"RenamableSourceAndConverter not contained in first node of inspector result");
				logger.debug("Class found = " + current.getUserObject().getClass()
					.getSimpleName());
				logger.debug("Object found = " + current.getUserObject());
			}
		}

		return chain;

	}

	/**
	 * Appends detailed SpimData information to a tree node.
	 * This includes ImageLoader info, cache info, attributes, and view registrations.
	 *
	 * @param parent parent node to add information to
	 * @param sac source and converter
	 * @param sourceAndConverterService service to get metadata
	 */
	private static void appendSpimDataInfo(DefaultMutableTreeNode parent,
		SourceAndConverter<?> sac,
		ISourceAndConverterService sourceAndConverterService)
	{
		if (!sourceAndConverterService.containsMetadata(sac,
			ISourceAndConverterService.SPIM_DATA_INFO))
		{
			return;
		}

		Object metadata = sourceAndConverterService.getMetadata(sac,
			ISourceAndConverterService.SPIM_DATA_INFO);
		if (!(metadata instanceof SourceAndConverterService.SpimDataInfo)) {
			return;
		}

		SourceAndConverterService.SpimDataInfo spimDataInfo =
			(SourceAndConverterService.SpimDataInfo) metadata;
		AbstractSpimData<?> asd = spimDataInfo.asd;
		int setupId = spimDataInfo.setupId;

		// Add ImageLoader information
		appendImageLoaderInfo(parent, asd);

		// Add Cache information
		appendCacheInfo(parent, asd);

		// Add Base Path information
		appendBasePathInfo(parent, asd);

		// Add Attributes information
		appendAttributesInfo(parent, asd, setupId);

		// Add View Registrations information (transforms)
		appendViewRegistrationsInfo(parent, asd, setupId, sac);
	}

	/**
	 * Appends ImageLoader class information.
	 */
	private static void appendImageLoaderInfo(DefaultMutableTreeNode parent,
		AbstractSpimData<?> asd)
	{
		try {
			BasicImgLoader imgLoader = asd.getSequenceDescription().getImgLoader();
			if (imgLoader != null) {
				DefaultMutableTreeNode loaderNode = new DefaultMutableTreeNode(
					"ImageLoader: " + imgLoader.getClass().getSimpleName());
				parent.add(loaderNode);

				// Add loader type details if available
				String loaderType = imgLoader.getClass().getName();
				if (loaderType.contains("Hdf5")) {
					loaderNode.add(new DefaultMutableTreeNode("Type: HDF5"));
				}
				else if (loaderType.contains("N5")) {
					loaderNode.add(new DefaultMutableTreeNode("Type: N5"));
				}
				else if (loaderType.contains("Zarr")) {
					loaderNode.add(new DefaultMutableTreeNode("Type: Zarr"));
				}
			}
		}
		catch (Exception e) {
			logger.debug("Could not get ImageLoader info: " + e.getMessage());
		}
	}

	/**
	 * Appends cache information extracted via reflection.
	 */
	private static void appendCacheInfo(DefaultMutableTreeNode parent,
		AbstractSpimData<?> asd)
	{
		try {
			BasicImgLoader imgLoader = asd.getSequenceDescription().getImgLoader();
			if (imgLoader != null) {
				// Try to get cache field via reflection
				Field cacheField = null;
				try {
					cacheField = imgLoader.getClass().getDeclaredField("cache");
				}
				catch (NoSuchFieldException e) {
					// Try parent class
					try {
						cacheField = imgLoader.getClass().getSuperclass()
							.getDeclaredField("cache");
					}
					catch (NoSuchFieldException ex) {
						// No cache field found
					}
				}

				if (cacheField != null) {
					cacheField.setAccessible(true);
					Object cache = cacheField.get(imgLoader);
					if (cache != null) {
						DefaultMutableTreeNode cacheNode = new DefaultMutableTreeNode(
							"Cache: " + cache.getClass().getSimpleName());
						parent.add(cacheNode);

						// Try to get cache stats if it's a VolatileGlobalCellCache
						if (cache instanceof VolatileGlobalCellCache) {
							try {
								VolatileGlobalCellCache vgcc = (VolatileGlobalCellCache) cache;
								cacheNode.add(new DefaultMutableTreeNode(
									"Cache Type: VolatileGlobalCellCache"));
							}
							catch (Exception ex) {
								// Silently ignore
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			logger.debug("Could not get cache info: " + e.getMessage());
		}
	}

	/**
	 * Appends base path information.
	 */
	private static void appendBasePathInfo(DefaultMutableTreeNode parent,
		AbstractSpimData<?> asd)
	{
        File basePath = asd.getBasePath();
        if (basePath != null) {
            DefaultMutableTreeNode pathNode = new DefaultMutableTreeNode(
                "Base Path: " + basePath.toString());
            parent.add(pathNode);
        }
	}

	/**
	 * Appends attributes information for the view setup.
	 */
	private static void appendAttributesInfo(DefaultMutableTreeNode parent,
		AbstractSpimData<?> asd, int setupId)
	{
		try {
			AbstractSequenceDescription<?, ?, ?> seq = asd.getSequenceDescription();
			BasicViewSetup setup = seq.getViewSetupsOrdered().stream()
				.filter(s -> s.getId() == setupId).findFirst().orElse(null);

			if (setup != null) {
				DefaultMutableTreeNode attributesNode = new DefaultMutableTreeNode(
					"Attributes");
				parent.add(attributesNode);

				// Add each attribute
				for (Entity entity : setup.getAttributes().values()) {
					if (entity != null) {
						String entityName = entity.getClass().getSimpleName() + ": ";
						if (entity instanceof NamedEntity) {
							entityName += ((NamedEntity) entity).getName();
						}
						else {
							entityName += "ID " + entity.getId();
						}
						attributesNode.add(new DefaultMutableTreeNode(entityName));
					}
				}


				// Add voxel size if available
				if (setup.getVoxelSize() != null) {
					attributesNode.add(new DefaultMutableTreeNode("Voxel Size: " + setup
						.getVoxelSize().dimension(0) + " x " + setup.getVoxelSize()
							.dimension(1) + " x " + setup.getVoxelSize().dimension(2)));
				}
			}
		}
		catch (Exception e) {
			logger.debug("Could not get attributes info: " + e.getMessage());
		}
	}

	/**
	 * Appends view registration information (transforms) and dimensions for the view setup.
	 */
	private static void appendViewRegistrationsInfo(DefaultMutableTreeNode parent, AbstractSpimData<?> asd, int setupId, SourceAndConverter<?> sac) {

        ViewRegistrations vrs = asd.getViewRegistrations();
        DefaultMutableTreeNode registrationsNode = new DefaultMutableTreeNode("Data");
        parent.add(registrationsNode);

        // Get the source to query dimensions
        Source<?> source = sac.getSpimSource();

        // Get timepoints
        List<TimePoint> timePoints = asd.getSequenceDescription()
            .getTimePoints().getTimePointsOrdered();

        for (int i = 0; i < timePoints.size(); i++) {
            TimePoint tp = timePoints.get(i);
            ViewRegistration vr = vrs.getViewRegistration(tp.getId(), setupId);
            if (vr != null) {
                DefaultMutableTreeNode tpNode = new DefaultMutableTreeNode(
                    "Timepoint " + tp.getId());
                registrationsNode.add(tpNode);

                // Add transform information as a supplier
                tpNode.add(new DefaultMutableTreeNode(new Supplier<AffineTransform3D>()
                {

                    public AffineTransform3D get() {
                        return vr.getModel();
                    }

                    public String toString() {
                        AffineTransform3D transform = vr.getModel();
                        // Display all 12 values of the 3D transform in scientific notation with 3 significant digits
                        return String.format(
                                "Transform: [%.3e, %.3e, %.3e, %.3e; %.3e, %.3e, %.3e, %.3e; %.3e, %.3e, %.3e, %.3e]",
                                transform.get(0, 0), transform.get(0, 1), transform.get(0, 2), transform.get(0, 3),
                                transform.get(1, 0), transform.get(1, 1), transform.get(1, 2), transform.get(1, 3),
                                transform.get(2, 0), transform.get(2, 1), transform.get(2, 2), transform.get(2, 3)
                        );
                    }
                }));

                // Add dimensions for all resolution levels
                try {
                    int numMipmapLevels = source.getNumMipmapLevels();
                    if (numMipmapLevels > 1) {
                        DefaultMutableTreeNode dimensionsNode = new DefaultMutableTreeNode(
                            "Dimensions (" + numMipmapLevels + " resolution levels)");
                        tpNode.add(dimensionsNode);

                        for (int level = 0; level < numMipmapLevels; level++) {
                            long[] dims = new long[source.getSource(tp.getId(), level).numDimensions()];
                            source.getSource(tp.getId(), level).dimensions(dims);

                            String dimStr = "Level " + level + ": ";
                            if (dims.length == 3) {
                                dimStr += dims[0] + " x " + dims[1] + " x " + dims[2];
                            } else if (dims.length == 2) {
                                dimStr += dims[0] + " x " + dims[1];
                            } else {
                                for (int d = 0; d < dims.length; d++) {
                                    dimStr += dims[d];
                                    if (d < dims.length - 1) dimStr += " x ";
                                }
                            }
                            dimensionsNode.add(new DefaultMutableTreeNode(dimStr));
                        }
                    } else {
                        // Single resolution
                        long[] dims = new long[source.getSource(tp.getId(), 0).numDimensions()];
                        source.getSource(tp.getId(), 0).dimensions(dims);

                        String dimStr = "Dimensions: ";
                        if (dims.length == 3) {
                            dimStr += dims[0] + " x " + dims[1] + " x " + dims[2];
                        } else if (dims.length == 2) {
                            dimStr += dims[0] + " x " + dims[1];
                        } else {
                            for (int d = 0; d < dims.length; d++) {
                                dimStr += dims[d];
                                if (d < dims.length - 1) dimStr += " x ";
                            }
                        }
                        tpNode.add(new DefaultMutableTreeNode(dimStr));
                    }
                } catch (Exception e) {
                    logger.debug("Could not get dimension info for timepoint " + tp.getId() + ": " + e.getMessage());
                }

                // Add cache statistics for this timepoint
                try {
                    AbstractGlobalCache globalCache = SourceAndConverterServices
                        .getSourceAndConverterService().getCache();
                    if (globalCache != null) {
                        // Get the root source object for cache lookup
                        //Source<?> rootSource = SourceAndConverterHelper.getRootSource(sac.getSpimSource());
                        AbstractGlobalCache.CacheStats stats = globalCache.getCacheStats(asd, setupId, tp.getId());
                        //if (stats.numberOfCells > 0) {
                            String cacheInfo = "Cache: " + stats.numberOfCells + " cells, " +
                                stats.getSizeInMB() + " MB";
                            tpNode.add(new DefaultMutableTreeNode(cacheInfo));
                        //}
                    }
                } catch (Exception e) {
                    logger.debug("Could not get cache stats for timepoint " + tp.getId() + ": " + e.getMessage());
                }
            }
        }
	}
}
