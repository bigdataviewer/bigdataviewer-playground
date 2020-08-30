package sc.fiji.bdvpg.scijava.services.ui;

import bdv.AbstractSpimSource;
import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.ResampledSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.LinkedList;
import java.util.function.Supplier;

public class SourceAndConverterInspector {

    /**
     * Appends all the metadata fo a SourceAndConverter into a tree structure
     */
    public static void appendMetadata(DefaultMutableTreeNode parent, SourceAndConverter sac) {
        SourceAndConverterServices.getSourceAndConverterService().getMetadataKeys(sac)
                .forEach(k -> {
                    DefaultMutableTreeNode nodeMetaKey = new DefaultMutableTreeNode(k);
                    parent.add(nodeMetaKey);
                    DefaultMutableTreeNode nodeMetaValue = new DefaultMutableTreeNode(
                            SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac, k));
                    nodeMetaKey.add(nodeMetaValue);
                });
    }

    /**
     * Inspects recursively a SourceAndConverter object
     * Properly inspected bdv.viewer.Source class:
     * - SpimSource (AbstractSpimSource)
     * - WarpedSource
     * - TransformedSource
     * - ResampledSource
     *
     * @param parent
     * @param sac
     * @param sourceAndConverterService
     */
    public static void appendInspectorResult(DefaultMutableTreeNode parent,
                                             SourceAndConverter sac,
                                             SourceAndConverterService sourceAndConverterService,
                                             boolean registerIntermediateSources) {
        if (sac.getSpimSource() instanceof TransformedSource) {
            DefaultMutableTreeNode nodeTransformedSource = new DefaultMutableTreeNode("Transformed Source");
            parent.add(nodeTransformedSource);
            TransformedSource source = (TransformedSource) sac.getSpimSource();

            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).size() > 0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeTransformedSource.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService, registerIntermediateSources);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getWrappedSource());
                if (registerIntermediateSources) {
                    sourceAndConverterService.register(src);
                }
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeTransformedSource.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService, registerIntermediateSources);
            }

            DefaultMutableTreeNode nodeAffineTransformGetter = new DefaultMutableTreeNode(new Supplier<AffineTransform3D>() {
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
            DefaultMutableTreeNode nodeWarpedSource = new DefaultMutableTreeNode("Warped Source");
            parent.add(nodeWarpedSource);
            WarpedSource source = (WarpedSource) sac.getSpimSource();

            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).size() > 0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeWarpedSource.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService, registerIntermediateSources);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getWrappedSource());
                if (registerIntermediateSources) {
                    sourceAndConverterService.register(src);
                }
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeWarpedSource.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService, registerIntermediateSources);
            }
            DefaultMutableTreeNode nodeRealTransformGetter = new DefaultMutableTreeNode(new Supplier<RealTransform>() {
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
            DefaultMutableTreeNode nodeResampledSource = new DefaultMutableTreeNode("Resampled Source");
            parent.add(nodeResampledSource);
            ResampledSource source = (ResampledSource) sac.getSpimSource();

            DefaultMutableTreeNode nodeOrigin = new DefaultMutableTreeNode("Origin");
            nodeResampledSource.add(nodeOrigin);

            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getOriginalSource()).size() > 0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getOriginalSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeOrigin.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService, registerIntermediateSources);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getOriginalSource());
                if (registerIntermediateSources) {
                    sourceAndConverterService.register(src);
                }
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeOrigin.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService, registerIntermediateSources);
            }

            DefaultMutableTreeNode nodeResampler = new DefaultMutableTreeNode("Sampler Model");
            nodeResampledSource.add(nodeResampler);

            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getModelResamplerSource()).size() > 0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getModelResamplerSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeResampler.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService, registerIntermediateSources);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getModelResamplerSource());
                if (registerIntermediateSources) {
                    sourceAndConverterService.register(src);
                }
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeResampler.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService, registerIntermediateSources);
            }
            appendMetadata(nodeResampledSource, sac);
        }

        if (sac.getSpimSource() instanceof AbstractSpimSource) {
            DefaultMutableTreeNode nodeSpimSource = new DefaultMutableTreeNode("Spim Source");
            parent.add(nodeSpimSource);
            appendMetadata(nodeSpimSource, sac);
        }
    }

    /**
     * Returns the root 'sourceandconverter' in the sense that it finds
     * the original source at the root of this source
     * <p>
     * for {@link ResampledSource} the model is ignored
     * <p>
     * it should fall either on an {@link AbstractSpimSource}
     * or on something unhandled or unknown
     * <p>
     * it shouldn't fall on a {@link ResampledSource} nor on a
     * {@link TransformedSource} nor on a {@link WarpedSource}
     * because these three class are wrappers
     */
    public static SourceAndConverter getRootSourceAndConverter(SourceAndConverter sac, SourceAndConverterService sacService) {
        return getListToRootSourceAndConverter(sac, sacService).getLast();
    }

    public static SourceAndConverter getRootSourceAndConverter(SourceAndConverter sac) {
        return getListToRootSourceAndConverter(sac, (SourceAndConverterService) SourceAndConverterServices.getSourceAndConverterService()).getLast();
    }

    public static LinkedList<SourceAndConverter> getListToRootSourceAndConverter(SourceAndConverter sac, SourceAndConverterService sacService) {

        LinkedList<SourceAndConverter> chain = new LinkedList<>();

        DefaultMutableTreeNode nodeSac = new DefaultMutableTreeNode(new
                RenamableSourceAndConverter(sac));

        chain.add(sac);

        appendInspectorResult(nodeSac,
                sac, sacService, // Hum, why the casting ?
                false);

        DefaultMutableTreeNode current = nodeSac;

        while (current.getChildCount() > 0) {
            System.out.println(">>>>");
            current = (DefaultMutableTreeNode) current.getFirstChild();
            if (current.getUserObject() instanceof RenamableSourceAndConverter) {

                System.out.println("RSAC");
                chain.add(((RenamableSourceAndConverter) (current.getUserObject())).sac);
                System.out.println("Renamable SourceAndConverter found");
                System.out.println(">"+chain.getLast());
            } else {
                System.out.println("NO RSAC");
                System.out.println("RenamableSourceAndConverter not contained " +
                        "in first node of inspector result");
                System.out.println("Class found = "+current.getUserObject().getClass().getSimpleName());
                System.out.println("Object found = "+current.getUserObject());

                //return null;
            }
        }

        return chain;

    }
}