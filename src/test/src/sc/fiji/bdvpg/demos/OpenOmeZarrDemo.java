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
package sc.fiji.bdvpg.demos;

import bdv.cache.SharedQueue;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.SourceAndConverter;
import net.imagej.ImageJ;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.bdv.N5Viewer;
import org.janelia.saalfeldlab.n5.bdv.N5ViewerCreator;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.universe.N5DatasetDiscoverer;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.N5TreeNode;
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata;
import sc.fiji.bdvpg.DemoHelper;
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.source.display.BrightnessAutoAdjuster;
import sc.fiji.bdvpg.viewer.bdv.navigate.ViewerTransformAdjuster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * PROOF-OF-CONCEPT: open OME-Zarr / OME-NGFF datasets (v0.4 and v0.5) into
 * BigDataViewer-Playground.
 * <p>
 * This demonstrates the "new" opening path (post pom-scijava 45.x), which does
 * NOT go through a BDV XML / SpimData / XmlIo serializer. Instead it:
 * <ol>
 *     <li>opens the container with {@link N5Factory} (n5-universe) - autodetects
 *         Zarr v2 / v3</li>
 *     <li>discovers &amp; parses OME-NGFF metadata with
 *         {@link N5DatasetDiscoverer} using the n5-viewer parser sets</li>
 *     <li>builds {@code SourceAndConverter}s directly with
 *         {@link N5Viewer#buildN5Sources}</li>
 *     <li>registers those raw sources in the Playground SourceService and shows
 *         them in a BDV window</li>
 * </ol>
 * The whole OME-Zarr reading stack (n5-viewer_fiji, n5-universe, n5-zarr) is
 * already on the classpath transitively via bigwarp_fiji -&gt; bigdataviewer_fiji.
 * <p>
 * NOTE: requires internet access - datasets are streamed from the IDR S3/HTTP
 * endpoints. If a URL is unreachable it is skipped and the demo continues.
 */
public class OpenOmeZarrDemo {

    static ImageJ ij;

    // --- Public sample datasets from the IDR NGFF sample catalog ---
    // https://idr.github.io/ome-ngff-samples/
    static final String OME_ZARR_V04 =
            "https://uk1s3.embassy.ebi.ac.uk/idr/zarr/v0.4/idr0062A/6001240.zarr";
    static final String OME_ZARR_V05 =
            "https://livingobjects.ebi.ac.uk/idr/zarr/v0.5/idr0066/ExpD_chicken_embryo_MIP.ome.zarr";

    public static void main(String... args) {
        ij = new ImageJ();
        DemoHelper.startFiji(ij);

        open("OME-NGFF v0.4", OME_ZARR_V04);
        open("OME-NGFF v0.5", OME_ZARR_V05);

        System.out.println("\n[OpenOmeZarrDemo] Done. Sources registered in the " +
                "Playground: " + SourceServices.getSourceService().getSources().size());
    }

    static void open(String label, String url) {
        System.out.println("\n========================================================");
        System.out.println("[" + label + "] opening " + url);
        System.out.println("========================================================");
        try {
            // 1. Open the container (Zarr v2/v3 autodetected)
            final N5Reader n5 = new N5Factory().openReader(url);
            System.out.println("  reader: " + n5.getClass().getName());

            // 2. Discover + parse OME-NGFF metadata using the n5-viewer parsers
            final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5,
                    Executors.newCachedThreadPool(),
                    java.util.Arrays.asList(N5ViewerCreator.n5vParsers),
                    java.util.Arrays.asList(N5ViewerCreator.n5vGroupParsers));
            final N5TreeNode root = discoverer.discoverAndParseRecursive("");

            System.out.println("  --- discovered tree ---");
            N5TreeNode.flattenN5Tree(root).forEach(node -> {
                final N5Metadata m = node.getMetadata();
                System.out.println("    path='" + node.getPath() + "' dataset=" +
                        node.isDataset() + " meta=" +
                        (m == null ? "<none>" : m.getClass().getSimpleName()));
            });

            // 3. Select the "top" openable nodes: any node that carries metadata,
            //    minus nodes that are nested inside another selected node (this
            //    keeps a multiscale group and drops its individual scale levels).
            final List<N5Metadata> selected = selectViewableMetadata(root);
            System.out.println("  selected " + selected.size() + " openable node(s):");
            selected.forEach(m -> System.out.println("      -> " + m.getPath() +
                    "  (" + m.getClass().getSimpleName() + ")"));

            if (selected.isEmpty()) {
                System.out.println("  !! nothing openable found - skipping");
                return;
            }

            // 4. Build SourceAndConverters directly (no SpimData, no XML)
            final SharedQueue queue = new SharedQueue(8);
            final List<ConverterSetup> converterSetups = new ArrayList<>();
            final List<SourceAndConverter<?>> sources = new ArrayList<>();
            final DataSelection selection = new DataSelection(n5, selected);

            // raw List cast: buildN5Sources is generic over the pixel type T
            @SuppressWarnings({ "rawtypes", "unchecked" })
            final List rawSources = sources;
            N5Viewer.buildN5Sources(n5, selection, queue, converterSetups,
                    rawSources, BdvOptions.options());

            System.out.println("  built " + sources.size() + " SourceAndConverter(s)");

            // 5. Register in the Playground and show
            for (SourceAndConverter<?> sac : sources) {
                if (sac == null) continue;
                describe(sac);
                SourceServices.getSourceService().register(sac);
                SourceServices.getBdvDisplayService().show(sac);
            }

            final BdvHandle bdv = SourceServices.getBdvDisplayService().getActiveBdv();
            for (SourceAndConverter<?> sac : sources) {
                if (sac == null) continue;
                new BrightnessAutoAdjuster<>(sac, 0).run(); // auto contrast
            }
            if (!sources.isEmpty() && sources.get(0) != null) {
                new ViewerTransformAdjuster(bdv, sources.get(0)).run();
            }

        } catch (Throwable t) {
            System.out.println("  !! FAILED to open " + url);
            t.printStackTrace(System.out);
        }
    }

    /**
     * Walks the discovered tree and collects one metadata entry per openable
     * image:
     * <ul>
     *     <li>A node carrying metadata that is <b>not itself a dataset</b> is a
     *     multiscale group (e.g. OME-NGFF {@code OmeNgffMetadata}). We take it as
     *     a single multiresolution image and do <b>not</b> descend into its
     *     scale-level children ({@code /0}, {@code /1}, ...).</li>
     *     <li>A node that <b>is</b> a dataset and still carries metadata (and is
     *     therefore not part of a multiscale group we already took) is a
     *     standalone image.</li>
     * </ul>
     * This yields one multiresolution {@code SourceAndConverter} per image
     * (further split per channel by {@link N5Viewer#buildN5Sources}), instead of
     * one flat source per resolution level.
     */
    static List<N5Metadata> selectViewableMetadata(N5TreeNode root) {
        final List<N5Metadata> out = new ArrayList<>();
        collect(root, out);
        return out;
    }

    static void collect(N5TreeNode node, List<N5Metadata> out) {
        final N5Metadata m = node.getMetadata();
        if (m != null && !node.isDataset()) {
            // group-level (multiscale) metadata -> one multiresolution image
            out.add(m);
            return; // children are its scale levels: do not add them separately
        }
        if (m != null) {
            // standalone dataset, not nested in a multiscale group
            out.add(m);
        }
        for (N5TreeNode child : node.childrenList()) {
            collect(child, out);
        }
    }

    static void describe(SourceAndConverter<?> sac) {
        try {
            final bdv.viewer.Source<?> s = sac.getSpimSource();
            final long[] dims = new long[s.getSource(0, 0).numDimensions()];
            s.getSource(0, 0).dimensions(dims);
            System.out.println("    source '" + s.getName() + "' type=" +
                    s.getType().getClass().getSimpleName() + " levels=" +
                    s.getNumMipmapLevels() + " dims(lvl0)=" +
                    java.util.Arrays.toString(dims) + " class=" +
                    s.getClass().getName());
        } catch (Throwable t) {
            System.out.println("    (could not introspect source: " + t + ")");
        }
    }
}