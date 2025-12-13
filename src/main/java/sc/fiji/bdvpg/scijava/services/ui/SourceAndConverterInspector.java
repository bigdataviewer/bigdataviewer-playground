/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.ISourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.tree.DefaultMutableTreeNode;
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
}
