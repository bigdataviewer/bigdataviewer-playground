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
package sc.fiji.bdvpg.scijava.services.ui.tree;

import bdv.util.BdvHandle;
import bdv.util.EmptySource;
import bdv.viewer.SourceAndConverter;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.persist.IObjectScijavaAdapterService;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SourceTreeModel} and {@link FilterNode}.
 *
 * <p>This test class is in the same package as the source code to access
 * package-private methods on FilterNode (addSource, removeSource, etc.).</p>
 */
public class SourceTreeModelTest {

    private Context context;
    private SourceAndConverterService sourceService;
    private SourceAndConverterBdvDisplayService displayService;
    private SourceTreeModel model;
    private final List<BdvHandle> bdvHandlesToClose = new ArrayList<>();

    @Before
    public void setUp() {
        context = new Context(SourceAndConverterService.class, SourceAndConverterBdvDisplayService.class, IObjectScijavaAdapterService.class);
        sourceService = context.getService(SourceAndConverterService.class);
        displayService = context.getService(SourceAndConverterBdvDisplayService.class);
        model = new SourceTreeModel(sourceService);
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
                SourceAndConverterHelper.createConverterRealType(new UnsignedShortType()));
    }

    private List<SourceAndConverter<?>> createTestSources(int count) {
        List<SourceAndConverter<?>> sources = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sources.add(createTestSource("Source" + i));
        }
        return sources;
    }

    /**
     * Creates test sources, registers them with the service, and links them to
     * a freshly loaded SpimData object. Returns the SpimData.
     */
    private AbstractSpimData<?> createLinkedSpimData(List<? extends SourceAndConverter<?>> sources) {
        try {
            AbstractSpimData<?> asd = new XmlIoSpimData().load("src/test/resources/mri-stack.xml");
            for (int i = 0; i < sources.size(); i++) {
                sourceService.register(sources.get(i));
                sourceService.linkToSpimData(sources.get(i), asd, i);
            }
            return asd;
        } catch (SpimDataException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new BdvHandle via the display service and tracks it for cleanup.
     */
    private BdvHandle createBdvHandle() {
        BdvHandle bdvh = displayService.getNewBdv();
        bdvHandlesToClose.add(bdvh);
        return bdvh;
    }

    /**
     * Listener that records all events for assertions.
     */
    private static class TestListener implements SourceTreeModelListener {
        final List<SourcesChangedEvent> sourcesEvents = new ArrayList<>();
        final List<StructureChangedEvent> structureEvents = new ArrayList<>();
        final List<String> eventOrder = new ArrayList<>();

        @Override
        public void sourcesChanged(SourcesChangedEvent event) {
            sourcesEvents.add(event);
            eventOrder.add("SOURCES");
        }

        @Override
        public void structureChanged(StructureChangedEvent event) {
            structureEvents.add(event);
            eventOrder.add("STRUCTURE");
        }

        void reset() {
            sourcesEvents.clear();
            structureEvents.clear();
            eventOrder.clear();
        }

        SourcesChangedEvent lastSourcesEvent() {
            return sourcesEvents.isEmpty() ? null : sourcesEvents.get(sourcesEvents.size() - 1);
        }

        StructureChangedEvent lastStructureEvent() {
            return structureEvents.isEmpty() ? null : structureEvents.get(structureEvents.size() - 1);
        }
    }

    // ==================== 1.1 FilterNode - Static filter ====================

    @Test
    public void filterNode_staticFilter_sourcePassesFilter() {
        FilterNode node = new FilterNode("Test", sac -> true, true);
        SourceAndConverter<?> source = createTestSource("S1");

        assertTrue("Source should pass filter", node.addSource(source));
        assertTrue("Source should be in outputSources", node.getOutputSources().contains(source));
        assertTrue("Source should be in inputSources", node.getInputSources().contains(source));
    }

    @Test
    public void filterNode_staticFilter_sourceFailsFilter() {
        FilterNode node = new FilterNode("Test", sac -> false, true);
        SourceAndConverter<?> source = createTestSource("S1");

        assertFalse("Source should fail filter", node.addSource(source));
        assertFalse("Source should not be in outputSources", node.getOutputSources().contains(source));
        assertTrue("Source should be in inputSources", node.getInputSources().contains(source));
    }

    @Test
    public void filterNode_staticFilter_duplicateAddReturnsFalse() {
        FilterNode node = new FilterNode("Test", sac -> true, true);
        SourceAndConverter<?> source = createTestSource("S1");

        assertTrue("First add should return true", node.addSource(source));
        assertFalse("Second add should return false (short-circuit)", node.addSource(source));
    }

    @Test
    public void filterNode_removeSource_removesFromBothSets() {
        FilterNode node = new FilterNode("Test", sac -> true, true);
        SourceAndConverter<?> source = createTestSource("S1");
        node.addSource(source);

        assertTrue("Remove should return true", node.removeSource(source));
        assertFalse("Source should not be in outputSources", node.getOutputSources().contains(source));
        assertFalse("Source should not be in inputSources", node.getInputSources().contains(source));
    }

    @Test
    public void filterNode_removeSource_unknownSourceReturnsFalse() {
        FilterNode node = new FilterNode("Test", sac -> true, true);
        assertFalse("Remove unknown source should return false",
                node.removeSource(createTestSource("S1")));
    }

    // ==================== 1.2 FilterNode - Dynamic filter ====================

    @Test
    public void filterNode_dynamicFilter_reevaluatesOnSecondAdd() {
        AtomicInteger count = new AtomicInteger(0);
        FilterNode node = new FilterNode("Test", sac -> count.incrementAndGet() > 1, true);
        node.setDynamicFilter(true);
        SourceAndConverter<?> source = createTestSource("S1");

        assertFalse("First add should fail (count=1)", node.addSource(source));
        assertTrue("Second add should pass (count=2, re-evaluated)", node.addSource(source));
        assertTrue("Source should be in outputSources", node.getOutputSources().contains(source));
    }

    @Test
    public void filterNode_dynamicFilter_alreadyInOutputReturnsFalse() {
        FilterNode node = new FilterNode("Test", sac -> true, true);
        node.setDynamicFilter(true);
        SourceAndConverter<?> source = createTestSource("S1");

        assertTrue("First add should succeed", node.addSource(source));
        assertFalse("Second add should return false (already in output)", node.addSource(source));
    }

    // ==================== 1.3 reevaluateSource ====================

    @Test
    public void filterNode_reevaluateSource_notInInput() {
        FilterNode node = new FilterNode("Test", sac -> true, true);
        assertEquals("Should return 0 for unknown source", 0,
                node.reevaluateSource(createTestSource("S1")));
    }

    @Test
    public void filterNode_reevaluateSource_wasRejectedNowPasses() {
        AtomicInteger count = new AtomicInteger(0);
        FilterNode node = new FilterNode("Test", sac -> count.incrementAndGet() > 1, true);
        SourceAndConverter<?> source = createTestSource("S1");
        node.addSource(source); // count=1, fails

        assertEquals("Should return 1 (added to output)", 1, node.reevaluateSource(source));
        assertTrue("Source should now be in outputSources", node.getOutputSources().contains(source));
    }

    @Test
    public void filterNode_reevaluateSource_wasAcceptedNowFails() {
        AtomicInteger count = new AtomicInteger(0);
        FilterNode node = new FilterNode("Test", sac -> count.incrementAndGet() <= 1, true);
        SourceAndConverter<?> source = createTestSource("S1");
        node.addSource(source); // count=1, passes

        assertEquals("Should return -1 (removed from output)", -1, node.reevaluateSource(source));
        assertFalse("Source should not be in outputSources", node.getOutputSources().contains(source));
        assertTrue("Source should still be in inputSources", node.getInputSources().contains(source));
    }

    @Test
    public void filterNode_reevaluateSource_stateUnchanged() {
        FilterNode node = new FilterNode("Test", sac -> true, true);
        SourceAndConverter<?> source = createTestSource("S1");
        node.addSource(source);

        assertEquals("Should return 0 (unchanged)", 0, node.reevaluateSource(source));
    }

    // ==================== 1.4 hasOutputSources ====================

    @Test
    public void filterNode_hasOutputSources_empty() {
        assertFalse("Empty node should return false",
                new FilterNode("Test", sac -> true, true).hasOutputSources());
    }

    @Test
    public void filterNode_hasOutputSources_afterAdding() {
        FilterNode node = new FilterNode("Test", sac -> true, true);
        node.addSource(createTestSource("S1"));
        assertTrue("Should return true after adding passing source", node.hasOutputSources());
    }

    @Test
    public void filterNode_hasOutputSources_afterRemovingAll() {
        FilterNode node = new FilterNode("Test", sac -> true, true);
        SourceAndConverter<?> source = createTestSource("S1");
        node.addSource(source);
        node.removeSource(source);
        assertFalse("Should return false after removing all sources", node.hasOutputSources());
    }

    // ==================== 1.5 Children management ====================

    @Test
    public void filterNode_addChild_setsParent() {
        FilterNode parent = new FilterNode("Parent", sac -> true, false);
        FilterNode child = new FilterNode("Child", sac -> true, true);

        parent.addChild(child);

        assertEquals("Child should reference parent", parent, child.getParent());
        assertEquals("Parent should have 1 child", 1, parent.getChildCount());
        assertTrue("Children list should contain child", parent.getChildren().contains(child));
    }

    @Test
    public void filterNode_removeChild_clearsParent() {
        FilterNode parent = new FilterNode("Parent", sac -> true, false);
        FilterNode child = new FilterNode("Child", sac -> true, true);
        parent.addChild(child);

        assertTrue("Remove should return true", parent.removeChild(child));
        assertNull("Child parent should be null", child.getParent());
        assertEquals("Parent should have 0 children", 0, parent.getChildCount());
    }

    @Test
    public void filterNode_getChildren_returnsDefensiveCopy() {
        FilterNode parent = new FilterNode("Parent", sac -> true, false);
        parent.addChild(new FilterNode("Child", sac -> true, true));

        List<FilterNode> children = parent.getChildren();
        children.clear(); // modify returned list

        assertEquals("Parent should still have 1 child", 1, parent.getChildCount());
    }

    // ==================== 2.1 SourceTreeModel - addSources ====================

    @Test
    public void model_addSources_allAppearInRoot() {
        List<SourceAndConverter<?>> sources = createTestSources(3);

        model.addSources(sources);

        assertTrue("All sources should be in root",
                model.getRoot().getOutputSources().containsAll(sources));
    }

    @Test
    public void model_addSources_appearsInOtherSourcesNode() {
        // Sources without SPIM_DATA_INFO metadata go to "Other Sources"
        List<SourceAndConverter<?>> sources = createTestSources(2);
        // Register so the service can answer metadata queries
        for (SourceAndConverter<?> sac : sources) {
            sourceService.register(sac);
        }

        model.addSources(sources);

        assertTrue("Sources should appear in Other Sources",
                model.getOtherSourcesNode().getOutputSources().containsAll(sources));
    }

    @Test
    public void model_addSources_firesSourcesChangedEvent() {
        TestListener listener = new TestListener();
        model.addListener(listener);

        model.addSources(createTestSources(3));

        assertEquals("Should fire exactly 1 sourcesChanged event", 1, listener.sourcesEvents.size());
        SourcesChangedEvent event = listener.lastSourcesEvent();
        assertEquals("Event type should be ADDED", SourcesChangedEvent.Type.ADDED, event.getType());
        assertEquals("Event should contain 3 sources", 3, event.getSources().size());
    }

    @Test
    public void model_addSources_singleEventForBatch() {
        TestListener listener = new TestListener();
        model.addListener(listener);

        model.addSources(createTestSources(5));

        assertEquals("Should fire single event for entire batch",
                1, listener.sourcesEvents.size());
    }

    @Test
    public void model_addSources_updatesSourceIndex() {
        SourceAndConverter<?> source = createTestSource("S1");

        model.addSource(source);

        assertFalse("Source should be tracked in sourceIndex",
                model.getNodesForSource(source).isEmpty());
        assertTrue("hasConsumed should return true", model.hasConsumed(source));
    }

    // ==================== 2.2 SourceTreeModel - removeSources ====================

    @Test
    public void model_removeSources_removedFromRoot() {
        List<SourceAndConverter<?>> sources = createTestSources(3);
        model.addSources(sources);

        TestListener listener = new TestListener();
        model.addListener(listener);
        model.removeSources(sources);

        assertFalse("Sources should not be in root",
                model.getRoot().getOutputSources().stream().anyMatch(sources::contains));
        assertEquals("Should fire 1 sourcesChanged event", 1, listener.sourcesEvents.size());
        assertEquals("Event type should be REMOVED",
                SourcesChangedEvent.Type.REMOVED, listener.lastSourcesEvent().getType());
    }

    @Test
    public void model_removeSources_cleansUpSourceIndex() {
        SourceAndConverter<?> source = createTestSource("S1");
        model.addSource(source);

        model.removeSource(source);

        assertTrue("sourceIndex should be empty for removed source",
                model.getNodesForSource(source).isEmpty());
        assertFalse("hasConsumed should return false", model.hasConsumed(source));
    }

    @Test
    public void model_removeSources_singleEventForBatch() {
        List<SourceAndConverter<?>> sources = createTestSources(5);
        model.addSources(sources);

        TestListener listener = new TestListener();
        model.addListener(listener);
        model.removeSources(sources);

        assertEquals("Should fire single event for batch",
                1, listener.sourcesEvents.size());
    }

    // ==================== 2.3 SourceTreeModel - updateSources ====================

    @Test
    public void model_updateSources_reevaluatesFilters() {
        AtomicInteger filterCallCount = new AtomicInteger(0);

        // Create a custom filter node that fails first, passes later
        FilterNode customNode = new FilterNode("Custom",
                sac -> filterCallCount.incrementAndGet() > 1, true);
        model.addNode(model.getRoot(), customNode);

        SourceAndConverter<?> source = createTestSource("S1");
        model.addSource(source);

        // After addSource, the filter was called once for customNode (count=1) → failed
        assertFalse("Source should not be in custom node initially",
                customNode.getOutputSources().contains(source));

        TestListener listener = new TestListener();
        model.addListener(listener);
        model.updateSources(Collections.singletonList(source));

        // After updateSources, reevaluateSource calls filter again (count=2) → passes
        assertTrue("Source should be in custom node after update",
                customNode.getOutputSources().contains(source));
    }

    // ==================== 3.1 SourceTreeModel - addSpimData ====================

    @Test
    public void model_addSpimData_createsNode() {
        List<SourceAndConverter<?>> sources = createTestSources(2);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);

        TestListener listener = new TestListener();
        model.addListener(listener);

        model.addSpimData(asd, "TestDataset");

        SpimDataFilterNode spimNode = model.getSpimDataNode(asd);
        assertNotNull("SpimData node should be created", spimNode);
        assertEquals("Node name should match", "TestDataset", spimNode.getName());

        assertFalse("Should fire structureChanged event", listener.structureEvents.isEmpty());
        assertEquals("Event type should be NODES_ADDED",
                StructureChangedEvent.Type.NODES_ADDED,
                listener.lastStructureEvent().getType());
    }

    @Test
    public void model_addSpimData_populatesExistingSources() {
        List<SourceAndConverter<?>> sources = createTestSources(2);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);

        // Add sources to model first, then add SpimData
        model.addSources(sources);
        model.addSpimData(asd, "TestDataset");

        SpimDataFilterNode spimNode = model.getSpimDataNode(asd);
        assertTrue("SpimData node should contain existing sources",
                spimNode.getOutputSources().containsAll(sources));
    }

    @Test
    public void model_addSpimData_duplicateIsNoOp() {
        List<SourceAndConverter<?>> sources = createTestSources(1);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);

        model.addSpimData(asd, "TestDataset");
        int childCountBefore = model.getRoot().getChildCount();

        model.addSpimData(asd, "TestDataset");

        assertEquals("Child count should not change on duplicate add",
                childCountBefore, model.getRoot().getChildCount());
    }

    // ==================== 3.2 SourceTreeModel - removeSpimData ====================

    @Test
    public void model_removeSpimData_removesNode() {
        List<SourceAndConverter<?>> sources = createTestSources(1);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);
        model.addSpimData(asd, "TestDataset");

        TestListener listener = new TestListener();
        model.addListener(listener);
        model.removeSpimData(asd);

        assertNull("SpimData node should be removed", model.getSpimDataNode(asd));
        assertFalse("Should fire structureChanged event", listener.structureEvents.isEmpty());
        assertEquals("Event type should be NODES_REMOVED",
                StructureChangedEvent.Type.NODES_REMOVED,
                listener.lastStructureEvent().getType());
    }

    @Test
    public void model_removeSpimData_cleansUpSourceIndex() {
        List<SourceAndConverter<?>> sources = createTestSources(2);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);
        model.addSources(sources);
        model.addSpimData(asd, "TestDataset");

        model.removeSpimData(asd);

        // Sources should still be tracked (they're in root/OtherSources), but not in
        // any SpimDataFilterNode
        for (SourceAndConverter<?> source : sources) {
            Set<FilterNode> nodes = model.getNodesForSource(source);
            assertTrue("Source should not reference removed SpimData node",
                    nodes.stream().noneMatch(n -> n instanceof SpimDataFilterNode));
        }
    }

    // ==================== 3.3 Auto-removal of empty SpimData nodes ====================

    @Test
    public void model_autoRemoveEmptySpimDataNode() {
        List<SourceAndConverter<?>> sources = createTestSources(2);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);
        model.addSources(sources);
        model.addSpimData(asd, "TestDataset");
        assertNotNull("SpimData node should exist initially", model.getSpimDataNode(asd));

        TestListener listener = new TestListener();
        model.addListener(listener);

        // Remove all sources belonging to this SpimData
        model.removeSources(sources);

        assertNull("SpimData node should be auto-removed", model.getSpimDataNode(asd));
    }

    @Test
    public void model_autoRemove_eventOrderingCorrect() {
        List<SourceAndConverter<?>> sources = createTestSources(2);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);
        model.addSources(sources);
        model.addSpimData(asd, "TestDataset");

        TestListener listener = new TestListener();
        model.addListener(listener);

        model.removeSources(sources);

        // SourcesChanged(REMOVED) should fire BEFORE StructureChanged(NODES_REMOVED)
        assertEquals("Should have 2 events total", 2, listener.eventOrder.size());
        assertEquals("First event should be SOURCES", "SOURCES", listener.eventOrder.get(0));
        assertEquals("Second event should be STRUCTURE", "STRUCTURE", listener.eventOrder.get(1));
    }

    @Test
    public void model_autoRemove_partialRemovalKeepsNode() {
        List<SourceAndConverter<?>> sources = createTestSources(3);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);
        model.addSources(sources);
        model.addSpimData(asd, "TestDataset");

        // Remove only 2 out of 3
        model.removeSources(sources.subList(0, 2));

        assertNotNull("SpimData node should still exist (not empty)",
                model.getSpimDataNode(asd));
        assertTrue("SpimData node should still have output sources",
                model.getSpimDataNode(asd).hasOutputSources());
    }

    @Test
    public void model_autoRemove_multipleSpimData_onlyRemovesEmptyOne() {
        List<SourceAndConverter<?>> sources1 = createTestSources(2);
        List<SourceAndConverter<?>> sources2 = createTestSources(2);

        AbstractSpimData<?> asd1 = createLinkedSpimData(sources1);
        AbstractSpimData<?> asd2 = createLinkedSpimData(sources2);

        model.addSources(sources1);
        model.addSources(sources2);
        model.addSpimData(asd1, "Dataset1");
        model.addSpimData(asd2, "Dataset2");

        // Remove all sources from asd1 only
        model.removeSources(sources1);

        assertNull("SpimData1 node should be removed", model.getSpimDataNode(asd1));
        assertNotNull("SpimData2 node should still exist", model.getSpimDataNode(asd2));
    }

    // ==================== 3.4 renameSpimData ====================

    @Test
    public void model_renameSpimData() {
        List<SourceAndConverter<?>> sources = createTestSources(1);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);
        model.addSpimData(asd, "OldName");

        TestListener listener = new TestListener();
        model.addListener(listener);
        model.renameSpimData(asd, "NewName");

        assertEquals("Node name should be updated", "NewName",
                model.getSpimDataNode(asd).getName());
        assertFalse("Should fire structureChanged event", listener.structureEvents.isEmpty());
        assertTrue("Event should be NODE_RENAMED",
                listener.lastStructureEvent().isNodeRenamed());
    }

    // ==================== 5.1 Event ordering ====================

    @Test
    public void model_eventOrdering_sourcesRemovedBeforeStructureRemoved() {
        // Same as model_autoRemove_eventOrderingCorrect, kept for plan section coverage
        List<SourceAndConverter<?>> sources = createTestSources(2);
        AbstractSpimData<?> asd = createLinkedSpimData(sources);
        model.addSources(sources);
        model.addSpimData(asd, "TestDataset");

        TestListener listener = new TestListener();
        model.addListener(listener);
        model.removeSources(sources);

        assertTrue("Should have at least 2 events", listener.eventOrder.size() >= 2);
        assertEquals("First: SOURCES", "SOURCES", listener.eventOrder.get(0));
        assertEquals("Second: STRUCTURE", "STRUCTURE", listener.eventOrder.get(1));

        // Verify event types
        assertEquals("SourcesChanged type should be REMOVED",
                SourcesChangedEvent.Type.REMOVED,
                listener.sourcesEvents.get(0).getType());
        assertEquals("StructureChanged type should be NODES_REMOVED",
                StructureChangedEvent.Type.NODES_REMOVED,
                listener.structureEvents.get(0).getType());
    }

    // ==================== 5.2 Event content ====================

    @Test
    public void model_eventContent_affectedNodesOnlyDisplayNodes() {
        // Create hierarchy: root -> intermediate(displaySources=false) -> leaf(displaySources=true)
        FilterNode intermediate = new FilterNode("Intermediate", sac -> true, false);
        FilterNode leaf = new FilterNode("Leaf", sac -> true, true);
        intermediate.addChild(leaf);
        model.addNode(model.getRoot(), intermediate);

        TestListener listener = new TestListener();
        model.addListener(listener);

        model.addSource(createTestSource("S1"));

        // Find the SourcesChanged event from addSource (skip the one from addNode if any)
        SourcesChangedEvent event = listener.sourcesEvents.stream()
                .filter(e -> e.getType() == SourcesChangedEvent.Type.ADDED)
                .reduce((first, second) -> second) // get last
                .orElse(null);
        assertNotNull("Should have an ADDED event", event);

        Map<FilterNode, List<SourceAndConverter<?>>> affected = event.getAffectedNodes();

        assertFalse("Intermediate node (displaySources=false) should not be in affectedNodes",
                affected.containsKey(intermediate));
        assertTrue("Leaf node (displaySources=true) should be in affectedNodes",
                affected.containsKey(leaf));
    }

    @Test
    public void model_eventContent_sourcesCollectionCorrect() {
        List<SourceAndConverter<?>> sources = createTestSources(3);
        TestListener listener = new TestListener();
        model.addListener(listener);

        model.addSources(sources);

        SourcesChangedEvent event = listener.lastSourcesEvent();
        assertNotNull("Event should be fired", event);
        assertEquals("Event should contain 3 sources", 3, event.getSources().size());
        assertTrue("Event sources should match added sources",
                event.getSources().containsAll(sources));
    }

    @Test
    public void model_structureEvent_indicesAreCorrect() {
        AbstractSpimData<?> asd = createLinkedSpimData(Collections.emptyList());

        TestListener listener = new TestListener();
        model.addListener(listener);

        model.addSpimData(asd, "TestDataset");

        StructureChangedEvent event = listener.lastStructureEvent();
        assertNotNull("Should fire structure event", event);
        List<Integer> indices = event.getChildIndices();
        assertFalse("Indices should not be empty", indices.isEmpty());
        assertTrue("Index should be >= 0", indices.get(0) >= 0);
    }

    // ==================== 7. Edge Cases ====================

    @Test
    public void model_addSourceToEmptyModel() {
        SourceAndConverter<?> source = createTestSource("S1");
        model.addSource(source);
        assertTrue("Source should be consumed by root", model.hasConsumed(source));
    }

    @Test
    public void model_removeSourceNotInModel_noError() {
        // Should not throw
        model.removeSource(createTestSource("S1"));
    }

    @Test
    public void model_removeSourcesWhenEmpty_noError() {
        // Should not throw
        model.removeSources(createTestSources(2));
    }

    @Test
    public void model_addSpimDataWithZeroSources() {
        AbstractSpimData<?> asd = createLinkedSpimData(Collections.emptyList());
        model.addSpimData(asd, "EmptyDataset");

        SpimDataFilterNode node = model.getSpimDataNode(asd);
        assertNotNull("Node should be created even with zero sources", node);
        assertFalse("Node should have no output sources", node.hasOutputSources());
    }

    // ==================== 4.1 BdvHandle - addBdvHandle ====================

    @Test
    public void model_addBdvHandle_createsNodeWithAllSourcesChild() {
        BdvHandle bdvh = createBdvHandle();

        TestListener listener = new TestListener();
        model.addListener(listener);

        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        // Find the BdvHandleFilterNode in root's children
        FilterNode bdvNode = null;
        for (FilterNode child : model.getRoot().getChildren()) {
            if (child instanceof BdvHandleFilterNode
                    && ((BdvHandleFilterNode) child).getBdvHandle().equals(bdvh)) {
                bdvNode = child;
                break;
            }
        }

        assertNotNull("BdvHandle node should be created", bdvNode);
        assertEquals("Node name should match", "TestBDV", bdvNode.getName());

        // Should have an "All Sources" child
        assertEquals("BdvHandle node should have 1 child", 1, bdvNode.getChildCount());
        assertEquals("Child should be 'All Sources'", "All Sources",
                bdvNode.getChild(0).getName());

        // Check event
        assertFalse("Should fire structureChanged event", listener.structureEvents.isEmpty());
        assertEquals("Event type should be NODES_ADDED",
                StructureChangedEvent.Type.NODES_ADDED,
                listener.lastStructureEvent().getType());
    }

    @Test
    public void model_addBdvHandle_duplicateIsNoOp() {
        BdvHandle bdvh = createBdvHandle();

        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());
        int childCountBefore = model.getRoot().getChildCount();

        model.addBdvHandle(bdvh, "TestBDV2", model.getRoot());

        assertEquals("Child count should not change on duplicate add",
                childCountBefore, model.getRoot().getChildCount());
    }

    @Test
    public void model_addBdvHandle_initialPopulation_sourcesInBdvAppear() {
        BdvHandle bdvh = createBdvHandle();

        // Create and register a source
        SourceAndConverter<?> source = createTestSource("S1");
        sourceService.register(source);

        // Add source to our model (goes to root)
        model.addSource(source);

        // Add source to BDV viewer state BEFORE adding bdvHandle to model
        bdvh.getViewerPanel().state().addSources(Collections.singletonList(source));

        // Now add BDV handle - it should populate with the source
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        // Find the BdvHandleFilterNode
        BdvHandleFilterNode bdvNode = findBdvNode(bdvh);
        assertNotNull("BdvHandle node should exist", bdvNode);
        assertTrue("Source already in BDV should be in bdvNode",
                bdvNode.hasConsumed(source));
    }

    @Test
    public void model_addBdvHandle_initialPopulation_sourcesNotInBdvDontAppear() {
        BdvHandle bdvh = createBdvHandle();

        // Create source and add to model, but DON'T add to BDV viewer
        SourceAndConverter<?> source = createTestSource("S1");
        sourceService.register(source);
        model.addSource(source);

        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        BdvHandleFilterNode bdvNode = findBdvNode(bdvh);
        assertNotNull("BdvHandle node should exist", bdvNode);
        assertFalse("Source NOT in BDV should not be in bdvNode",
                bdvNode.hasConsumed(source));
    }

    // ==================== 4.2 BdvHandle - removeBdvHandle ====================

    @Test
    public void model_removeBdvHandle_removesNode() {
        BdvHandle bdvh = createBdvHandle();
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());
        assertNotNull("BdvHandle node should exist before removal", findBdvNode(bdvh));

        TestListener listener = new TestListener();
        model.addListener(listener);

        model.removeBdvHandle(bdvh);

        assertNull("BdvHandle node should be removed", findBdvNode(bdvh));
        assertFalse("Should fire structureChanged event", listener.structureEvents.isEmpty());
        assertEquals("Event type should be NODES_REMOVED",
                StructureChangedEvent.Type.NODES_REMOVED,
                listener.lastStructureEvent().getType());
    }

    @Test
    public void model_removeBdvHandleNodes_removesAllInstances() {
        BdvHandle bdvh = createBdvHandle();
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        model.removeBdvHandleNodes(bdvh);

        assertNull("BdvHandle node should be removed", findBdvNode(bdvh));
    }

    // ==================== 4.3 BdvHandle - refreshBdvHandleNode ====================

    @Test
    public void model_refreshBdvHandle_sourceShownInBdvAppears() {
        BdvHandle bdvh = createBdvHandle();
        SourceAndConverter<?> source = createTestSource("S1");
        sourceService.register(source);

        // Add source to model and add BDV handle
        model.addSource(source);
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        BdvHandleFilterNode bdvNode = findBdvNode(bdvh);
        assertFalse("Source should NOT be in BDV node yet", bdvNode.hasConsumed(source));

        // Show source in BDV - triggers state listener → refreshBdvHandleNode
        bdvh.getViewerPanel().state().addSources(Collections.singletonList(source));

        assertTrue("Source should now be in BDV node",
                bdvNode.hasConsumed(source));

        // "All Sources" child should also have the source
        FilterNode allSourcesChild = bdvNode.getChild(0);
        assertTrue("Source should be in 'All Sources' child",
                allSourcesChild.hasConsumed(source));
    }

    @Test
    public void model_refreshBdvHandle_sourceRemovedFromBdvDisappears() {
        BdvHandle bdvh = createBdvHandle();
        SourceAndConverter<?> source = createTestSource("S1");
        sourceService.register(source);

        model.addSource(source);
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        // Show source in BDV
        bdvh.getViewerPanel().state().addSources(Collections.singletonList(source));
        assertTrue("Source should be in BDV node", findBdvNode(bdvh).hasConsumed(source));

        // Remove source from BDV
        bdvh.getViewerPanel().state().removeSources(Collections.singletonList(source));

        assertFalse("Source should no longer be in BDV node",
                findBdvNode(bdvh).hasConsumed(source));
    }

    @Test
    public void model_refreshBdvHandle_multipleSourcesAddedAtOnce() {
        BdvHandle bdvh = createBdvHandle();
        List<SourceAndConverter<?>> sources = createTestSources(3);
        for (SourceAndConverter<?> sac : sources) {
            sourceService.register(sac);
        }

        model.addSources(sources);
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        // Show all sources in BDV at once
        bdvh.getViewerPanel().state().addSources(sources);

        BdvHandleFilterNode bdvNode = findBdvNode(bdvh);
        for (SourceAndConverter<?> sac : sources) {
            assertTrue("Each source should be in BDV node", bdvNode.hasConsumed(sac));
        }
    }

    @Test
    public void model_refreshBdvHandle_showHideShowTracksState() {
        BdvHandle bdvh = createBdvHandle();
        SourceAndConverter<?> source = createTestSource("S1");
        sourceService.register(source);

        model.addSource(source);
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());
        BdvHandleFilterNode bdvNode = findBdvNode(bdvh);

        // Show
        bdvh.getViewerPanel().state().addSources(Collections.singletonList(source));
        assertTrue("After show: source should be in node", bdvNode.hasConsumed(source));

        // Hide
        bdvh.getViewerPanel().state().removeSources(Collections.singletonList(source));
        assertFalse("After hide: source should not be in node", bdvNode.hasConsumed(source));

        // Show again
        bdvh.getViewerPanel().state().addSources(Collections.singletonList(source));
        assertTrue("After re-show: source should be in node again", bdvNode.hasConsumed(source));
    }

    // ==================== 4.4 BdvHandle - Dynamic filter interaction ====================

    @Test
    public void model_bdvHandle_dynamicFilter_sourceRegisteredThenShown() {
        BdvHandle bdvh = createBdvHandle();

        // Add BDV handle first (no sources yet)
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        // Now create and register source
        SourceAndConverter<?> source = createTestSource("S1");
        sourceService.register(source);
        model.addSource(source);

        BdvHandleFilterNode bdvNode = findBdvNode(bdvh);
        assertFalse("Source not in BDV should not be in node", bdvNode.hasConsumed(source));

        // Show source in BDV
        bdvh.getViewerPanel().state().addSources(Collections.singletonList(source));
        assertTrue("Source shown in BDV should now appear in node",
                bdvNode.hasConsumed(source));
    }

    @Test
    public void model_bdvHandle_dynamicFilter_sourceShownThenRemovedFromBdv() {
        BdvHandle bdvh = createBdvHandle();

        SourceAndConverter<?> source = createTestSource("S1");
        sourceService.register(source);
        model.addSource(source);
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        // Show in BDV
        bdvh.getViewerPanel().state().addSources(Collections.singletonList(source));
        assertTrue("Source should be in node", findBdvNode(bdvh).hasConsumed(source));

        // Remove from BDV
        bdvh.getViewerPanel().state().removeSources(Collections.singletonList(source));
        assertFalse("Source removed from BDV should disappear from node",
                findBdvNode(bdvh).hasConsumed(source));
    }

    @Test
    public void model_bdvHandle_closedBdv_removeBdvHandleNodesCleansUp() {
        BdvHandle bdvh = createBdvHandle();
        SourceAndConverter<?> source = createTestSource("S1");
        sourceService.register(source);
        model.addSource(source);
        model.addBdvHandle(bdvh, "TestBDV", model.getRoot());

        bdvh.getViewerPanel().state().addSources(Collections.singletonList(source));
        assertNotNull("BDV node should exist", findBdvNode(bdvh));

        // Simulate BDV close by calling removeBdvHandleNodes
        model.removeBdvHandleNodes(bdvh);

        assertNull("BDV node should be removed after close", findBdvNode(bdvh));
    }

    // ==================== BdvHandle helpers ====================

    /**
     * Finds the BdvHandleFilterNode for a given BdvHandle in our model's root children.
     */
    private BdvHandleFilterNode findBdvNode(BdvHandle bdvh) {
        for (FilterNode child : model.getRoot().getChildren()) {
            BdvHandleFilterNode found = findBdvNodeRecursive(child, bdvh);
            if (found != null) return found;
        }
        return null;
    }

    private BdvHandleFilterNode findBdvNodeRecursive(FilterNode node, BdvHandle bdvh) {
        if (node instanceof BdvHandleFilterNode
                && ((BdvHandleFilterNode) node).getBdvHandle().equals(bdvh)) {
            return (BdvHandleFilterNode) node;
        }
        for (FilterNode child : node.getChildren()) {
            BdvHandleFilterNode found = findBdvNodeRecursive(child, bdvh);
            if (found != null) return found;
        }
        return null;
    }
}
