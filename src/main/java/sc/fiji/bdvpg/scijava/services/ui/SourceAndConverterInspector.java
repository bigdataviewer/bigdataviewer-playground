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
import java.util.Map;
import java.util.function.Supplier;

public class SourceAndConverterInspector {


    // TODO : understand what the heck is this ?
    public static void appendMetadata(DefaultMutableTreeNode parent, SourceAndConverter sac) {
        Map<String, Object> metadata = SourceAndConverterServices.getSourceAndConverterService().getSacToMetadata().get(sac);
        metadata.keySet().forEach(k -> {
            DefaultMutableTreeNode nodeMetaKey = new DefaultMutableTreeNode(k);
            parent.add(nodeMetaKey);
            DefaultMutableTreeNode nodeMetaValue = new DefaultMutableTreeNode(metadata.get(k));
            nodeMetaKey.add(nodeMetaValue);
        });
    }

    public static void appendInspectorResult(DefaultMutableTreeNode parent, SourceAndConverter sac, SourceAndConverterService sourceAndConverterService) {

        if (sac.getSpimSource() instanceof TransformedSource) {
            DefaultMutableTreeNode nodeTransformedSource = new DefaultMutableTreeNode("Transformed Source");
            parent.add(nodeTransformedSource);
            TransformedSource source = (TransformedSource) sac.getSpimSource();
            DefaultMutableTreeNode nodeAffineTransformGetter = new DefaultMutableTreeNode(new Supplier<AffineTransform3D>(){
                public AffineTransform3D get() {
                    AffineTransform3D at3D = new AffineTransform3D();
                    source.getFixedTransform(at3D);
                    return at3D;
                }
                public String toString() {
                    return "AffineTransform["+source.getName()+"]";
                }
            });
            nodeTransformedSource.add(nodeAffineTransformGetter);
            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeTransformedSource.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getWrappedSource());
                sourceAndConverterService.register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeTransformedSource.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService);
            }
            appendMetadata(nodeTransformedSource,sac);
        }

        if (sac.getSpimSource() instanceof WarpedSource) {
            DefaultMutableTreeNode nodeWarpedSource = new DefaultMutableTreeNode("Warped Source");
            parent.add(nodeWarpedSource);
            WarpedSource source = (WarpedSource) sac.getSpimSource();
            DefaultMutableTreeNode nodeRealTransformGetter = new DefaultMutableTreeNode(new Supplier<RealTransform>(){
                public RealTransform get() {
                    return source.getTransform();
                }
                public String toString() {
                    return "RealTransform["+source.getName()+"]";
                }
            });
            nodeWarpedSource.add(nodeRealTransformGetter);
            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getWrappedSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeWarpedSource.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getWrappedSource());
                sourceAndConverterService.register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeWarpedSource.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService);
            }
            appendMetadata(nodeWarpedSource,sac);
        }

        if (sac.getSpimSource() instanceof ResampledSource) {
            DefaultMutableTreeNode nodeResampledSource = new DefaultMutableTreeNode("Resampled Source");
            parent.add(nodeResampledSource);
            ResampledSource source = (ResampledSource) sac.getSpimSource();

            DefaultMutableTreeNode nodeOrigin = new DefaultMutableTreeNode("Origin");
            nodeResampledSource.add(nodeOrigin);

            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getOriginalSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getOriginalSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeOrigin.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getOriginalSource());
                sourceAndConverterService.register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeOrigin.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService);
            }

            DefaultMutableTreeNode nodeResampler = new DefaultMutableTreeNode("Sampler Model");
            nodeResampledSource.add(nodeResampler);

            if (sourceAndConverterService.getSourceAndConvertersFromSource(source.getModelResamplerSource()).size()>0) {
                // at least A sourceandconverteralready exists for this source
                sourceAndConverterService.getSourceAndConvertersFromSource(source.getModelResamplerSource()).forEach((src) -> {
                            DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                            nodeResampler.add(wrappedSourceNode);
                            appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService);
                        }
                );
            } else {
                // no source and converter exist for this source : creates it
                SourceAndConverter src = SourceAndConverterUtils.createSourceAndConverter(source.getModelResamplerSource());
                sourceAndConverterService.register(src);
                DefaultMutableTreeNode wrappedSourceNode = new DefaultMutableTreeNode(new RenamableSourceAndConverter(src));
                nodeResampler.add(wrappedSourceNode);
                appendInspectorResult(wrappedSourceNode, src, sourceAndConverterService);
            }
            appendMetadata(nodeResampledSource,sac);
        }

        if (sac.getSpimSource() instanceof AbstractSpimSource) {
            DefaultMutableTreeNode nodeSpimSource = new DefaultMutableTreeNode("Spim Source");
            parent.add(nodeSpimSource);
            appendMetadata(nodeSpimSource,sac);
        }
    }
}
