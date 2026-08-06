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
package sc.fiji.bdvpg.scijava.service.tree;

import bdv.util.BdvHandle;
import bdv.util.EmptySource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import sc.fiji.bdvpg.scijava.service.RenamableSource;
import sc.fiji.bdvpg.scijava.service.SourceBdvDisplayService;
import sc.fiji.bdvpg.scijava.service.SourceService;
import sc.fiji.bdvpg.source.SourceHelper;
import sc.fiji.persist.IObjectScijavaAdapterService;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link TreeSelectionContext}: classification of a
 * multi-selection into sources to act on, BDV window nodes, SpimData nodes and
 * inspection result nodes.
 */
public class TreeSelectionContextTest {

    private Context context;
    private SourceService sourceService;
    private SourceBdvDisplayService displayService;
    private final List<BdvHandle> bdvHandlesToClose = new ArrayList<>();

    /**
     * Tree-node-to-filter-node mapping playing the role of
     * {@link SourceTreeView#getFilterNode}.
     */
    private final Map<DefaultMutableTreeNode, FilterNode> nodeMap = new HashMap<>();

    @Before
    public void setUp() {
        context = new Context(SourceService.class, SourceBdvDisplayService.class,
                IObjectScijavaAdapterService.class);
        sourceService = context.getService(SourceService.class);
        displayService = context.getService(SourceBdvDisplayService.class);
    }

    @After
    public void tearDown() {
        for (BdvHandle bdvh : bdvHandlesToClose) {
            try { bdvh.close(); } catch (Exception ignored) {}
        }
        bdvHandlesToClose.clear();
        if (context != null) {
            context.close();
        }
    }

    // ==================== Helpers ====================

    private SourceAndConverter<?> createTestSource(String name) {
        AffineTransform3D transform = new AffineTransform3D();
        transform.identity();
        EmptySource source = new EmptySource(100, 100, 50, transform, name, null);
        return new SourceAndConverter<>(source,
                SourceHelper.createConverterRealType(new UnsignedShortType()));
    }

    private BdvHandle createBdvHandle() {
        BdvHandle bdvh = displayService.getNewBdv();
        bdvHandlesToClose.add(bdvh);
        return bdvh;
    }

    /**
     * Creates a tree node representing a source leaf.
     */
    private DefaultMutableTreeNode sourceLeaf(SourceAndConverter<?> source) {
        return new DefaultMutableTreeNode(new RenamableSource(source));
    }

    /**
     * Creates a tree node representing a filter node and registers it in the
     * resolver map.
     */
    private DefaultMutableTreeNode filterTreeNode(FilterNode filterNode) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(
                filterNode.name());
        nodeMap.put(treeNode, filterNode);
        return treeNode;
    }

    private TreeSelectionContext contextOf(DefaultMutableTreeNode... nodes) {
        TreePath[] paths = new TreePath[nodes.length];
        for (int i = 0; i < nodes.length; i++) {
            paths[i] = new TreePath(nodes[i]);
        }
        return new TreeSelectionContext(paths, nodeMap::get);
    }

    // ==================== Sources ====================

    @Test
    public void sourceLeaves_collectedAsSources() {
        SourceAndConverter<?> s1 = createTestSource("S1");
        SourceAndConverter<?> s2 = createTestSource("S2");

        TreeSelectionContext ctx = contextOf(sourceLeaf(s1), sourceLeaf(s2));

        assertEquals("Both sources should be collected", 2, ctx.sources().size());
        assertTrue(ctx.sources().contains(s1));
        assertTrue(ctx.sources().contains(s2));
        assertTrue("No BDV nodes expected", ctx.bdvNodes().isEmpty());
        assertTrue("No inspect nodes expected", ctx.inspectNodes().isEmpty());
        assertFalse(ctx.isEmpty());
    }

    @Test
    public void duplicateSourceSelection_collapsesToOne() {
        SourceAndConverter<?> s1 = createTestSource("S1");

        // Same source selected twice (it can appear at several places in the tree)
        TreeSelectionContext ctx = contextOf(sourceLeaf(s1), sourceLeaf(s1));

        assertEquals("Duplicated selection should yield the source once",
                1, ctx.sources().size());
    }

    @Test
    public void filterNode_contributesItsOutputSources() {
        FilterNode filterNode = new FilterNode("My Filter", source -> true, true);
        SourceAndConverter<?> s1 = createTestSource("S1");
        SourceAndConverter<?> s2 = createTestSource("S2");
        filterNode.addSource(s1);
        filterNode.addSource(s2);

        TreeSelectionContext ctx = contextOf(filterTreeNode(filterNode));

        assertEquals("Filter node sources should be collected",
                2, ctx.sources().size());
        assertTrue(ctx.sources().contains(s1));
        assertTrue(ctx.sources().contains(s2));
    }

    @Test
    public void sourceLeafAndItsFilterNode_collapsesToOne() {
        FilterNode filterNode = new FilterNode("My Filter", source -> true, true);
        SourceAndConverter<?> s1 = createTestSource("S1");
        filterNode.addSource(s1);

        TreeSelectionContext ctx = contextOf(filterTreeNode(filterNode),
                sourceLeaf(s1));

        assertEquals("Source selected directly and via its filter node should" +
                " be collected once", 1, ctx.sources().size());
    }

    // ==================== SpimData nodes ====================

    @Test
    public void spimDataNode_listedAndSourcesCollected() {
        SpimDataFilterNode spimNode = new SpimDataFilterNode("Dataset", null,
                sourceService);
        // Bypass the metadata-based filter: accept everything for this test
        spimNode.setFilter(source -> true);
        SourceAndConverter<?> s1 = createTestSource("S1");
        spimNode.addSource(s1);

        TreeSelectionContext ctx = contextOf(filterTreeNode(spimNode));

        assertEquals("SpimData node should be listed", 1,
                ctx.spimDataNodes().size());
        assertTrue("SpimData node sources should be collected",
                ctx.sources().contains(s1));
    }

    // ==================== BDV window nodes ====================

    @Test
    public void bdvNode_listedButItsSourcesNotCollected() {
        BdvHandle bdvh = createBdvHandle();
        SourceAndConverter<?> s1 = createTestSource("S1");
        sourceService.register(s1);
        bdvh.getViewerPanel().state().addSources(Collections.singletonList(s1));

        BdvHandleFilterNode bdvNode = new BdvHandleFilterNode("TestBDV", bdvh);
        bdvNode.addSource(s1);
        assertTrue("Precondition: BDV node contains the source",
                bdvNode.outputSources().contains(s1));

        TreeSelectionContext ctx = contextOf(filterTreeNode(bdvNode));

        assertEquals("BDV node should be listed", 1, ctx.bdvNodes().size());
        assertEquals("BdvHandle should be resolved", bdvh,
                ctx.bdvHandles().get(0));
        assertTrue("Sources of a selected BDV node must NOT be collected for" +
                " deletion", ctx.sources().isEmpty());

        bdvNode.cleanup();
    }

    @Test
    public void mixedSelection_bdvNodeAndSourceLeaf() {
        BdvHandle bdvh = createBdvHandle();
        SourceAndConverter<?> shown = createTestSource("Shown");
        SourceAndConverter<?> other = createTestSource("Other");
        sourceService.register(shown);
        bdvh.getViewerPanel().state().addSources(Collections.singletonList(
                shown));

        BdvHandleFilterNode bdvNode = new BdvHandleFilterNode("TestBDV", bdvh);
        bdvNode.addSource(shown);
        assertTrue("Precondition: BDV node contains the shown source",
                bdvNode.outputSources().contains(shown));

        TreeSelectionContext ctx = contextOf(filterTreeNode(bdvNode),
                sourceLeaf(other));

        assertEquals("One BDV window to act on", 1, ctx.bdvHandles().size());
        assertEquals("One source to act on", 1, ctx.sources().size());
        assertTrue(ctx.sources().contains(other));
        assertFalse(ctx.sources().contains(shown));

        bdvNode.cleanup();
    }

    // ==================== Inspect nodes ====================

    @Test
    public void inspectNode_classifiedAsInspectNode() {
        DefaultMutableTreeNode inspect = new DefaultMutableTreeNode(
                SourceTree.INSPECT_NODE_PREFIX + "S1]");

        TreeSelectionContext ctx = contextOf(inspect);

        assertEquals("Inspect node should be listed", 1,
                ctx.inspectNodes().size());
        assertTrue("No sources expected", ctx.sources().isEmpty());
        assertFalse(ctx.isEmpty());
    }

    @Test
    public void unknownPlainNode_ignored() {
        DefaultMutableTreeNode unknown = new DefaultMutableTreeNode(
                "Some random node");

        TreeSelectionContext ctx = contextOf(unknown);

        assertTrue("Unknown plain nodes should be ignored", ctx.isEmpty());
    }

    // ==================== Empty / null selection ====================

    @Test
    public void nullSelection_isEmpty() {
        TreeSelectionContext ctx = new TreeSelectionContext(null, nodeMap::get);

        assertTrue(ctx.isEmpty());
        assertEquals(0, ctx.paths().length);
        assertTrue(ctx.sources().isEmpty());
        assertTrue(ctx.bdvNodes().isEmpty());
        assertTrue(ctx.bdvHandles().isEmpty());
        assertTrue(ctx.spimDataNodes().isEmpty());
        assertTrue(ctx.inspectNodes().isEmpty());
    }

    @Test
    public void emptySelection_isEmpty() {
        TreeSelectionContext ctx = new TreeSelectionContext(new TreePath[0],
                nodeMap::get);

        assertTrue(ctx.isEmpty());
    }
}
