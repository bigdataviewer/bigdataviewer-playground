# Source Tree View - Test Plan

This document describes the tests to implement for the source tree view (`sc.fiji.bdvpg.scijava.services.ui.tree`).

All tests should target `SourceTreeModel` and `FilterNode` directly. A minimal `SourceAndConverterService` and `SourceAndConverterBdvDisplayService` can be used where needed. See `sc.fiji.bdvpg.tests.adapters.sources.SourceSerializationTests` for an example of test that needs setting up a minimal context.
Be aware that tests are run with a x11 emulated, so creating a BigDataViewer is not an issue.

A draft test class (non working) is present in sc.fiji.bdvpg.tests.ui.tree.SourceTreeModelTest


## 1. FilterNode - Basic Operations

### 1.1 Static filter: addSource / removeSource
- Source passes filter -> appears in outputSources, addSource returns true
- Source fails filter -> in inputSources but not outputSources, addSource returns false
- Source already in inputSources (static filter) -> addSource returns false (short-circuit)
- removeSource -> removed from both inputSources and outputSources
- removeSource for unknown source -> returns false, no error

### 1.2 Dynamic filter: addSource re-evaluation
- Source fails filter on first add -> in inputSources, not in outputSources
- Same source added again after filter state changes -> re-evaluated, added to outputSources, returns true
- Source already in outputSources (dynamic filter, still passes) -> addSource returns false (no duplicate)

### 1.3 reevaluateSource
- Source not in inputSources -> returns 0
- Source was rejected, now passes -> added to outputSources, returns 1
- Source was accepted, now fails -> removed from outputSources, returns -1
- Source state unchanged -> returns 0

### 1.4 hasOutputSources
- Empty node -> false
- After adding passing source -> true
- After removing all passing sources -> false

### 1.5 Children management
- addChild / removeChild / getChildren / getChildCount
- getChildren returns a defensive copy (modifying it doesn't affect the node)
- Parent reference is set/cleared correctly

## 2. SourceTreeModel - Source Operations

### 2.1 addSources (batch)
- Add N sources -> all appear in root's outputSources
- Sources matching "Other Sources" filter -> appear under otherSourcesNode
- SourcesChangedEvent(ADDED) is fired with correct affectedNodes map
- Single event fired for entire batch (not N events)
- sourceIndex is updated for each source

### 2.2 removeSources (batch)
- Remove N sources -> removed from all nodes
- SourcesChangedEvent(REMOVED) fired with correct affectedNodes
- sourceIndex entries are cleaned up
- Single event for entire batch

### 2.3 updateSources
- Source re-evaluated across all nodes
- Fires REMOVED then ADDED events as needed

## 3. SourceTreeModel - SpimData Operations

### 3.1 addSpimData
- Creates SpimDataFilterNode with entity sub-hierarchy
- SpimData node added as child of root
- StructureChangedEvent(NODES_ADDED) fired
- Sources already registered for that SpimData are populated into the new node
- spimDataIndex is updated
- Duplicate addSpimData call is a no-op

### 3.2 removeSpimData
- Node removed from root
- StructureChangedEvent(NODES_REMOVED) fired
- spimDataIndex entry removed
- sourceIndex entries cleaned up

### 3.3 Auto-removal of empty SpimData nodes
- Add SpimData with sources -> node exists
- Remove all sources belonging to that SpimData -> SpimData node is automatically removed
- StructureChangedEvent(NODES_REMOVED) fired after SourcesChangedEvent(REMOVED)
- spimDataIndex entry is cleaned up
- Removing only some sources -> node persists (not empty yet)
- Multiple SpimData: removing all sources from one only removes that one

### 3.4 renameSpimData
- Node name updated
- StructureChangedEvent(NODE_RENAMED) fired

## 4. SourceTreeModel - BdvHandle Operations

These tests require a mock BdvHandle (or at least a mock ViewerState).

### 4.1 addBdvHandle
- Creates BdvHandleFilterNode with "All Sources" child
- StructureChangedEvent(NODES_ADDED) fired
- bdvHandleIndex is updated
- Initial population: sources already in BDV appear in the node
- Initial population: sources not in BDV don't appear
- Duplicate addBdvHandle call is a no-op

### 4.2 removeBdvHandle / removeBdvHandleNodes
- Node removed from tree
- StructureChangedEvent(NODES_REMOVED) fired
- bdvHandleIndex entry removed
- cleanup() called (state listener removed)

### 4.3 refreshBdvHandleNode (via filterUpdateCallback)
- Source shown in BDV -> appears in BdvHandleFilterNode and its "All Sources" child
- Source removed from BDV -> disappears from node and children
- Multiple sources added at once -> all appear
- Source shown then hidden then shown again -> correctly tracks state

### 4.4 Dynamic filter interaction
- Source registered in service, then shown in BDV -> appears in node (dynamicFilter re-evaluates)
- Source registered, shown in BDV, then removed from BDV -> disappears from node
- BDV closed -> removeBdvHandleNodes cleans up everything

## 5. Event Ordering and Correctness

### 5.1 Event ordering for SpimData auto-removal
- SourcesChangedEvent(REMOVED) fires BEFORE StructureChangedEvent(NODES_REMOVED)
- This ensures the view can process source removal while parent nodes still exist

### 5.2 Event content
- SourcesChangedEvent.affectedNodes only contains nodes with displaySources=true
- SourcesChangedEvent.sources contains the correct collection
- StructureChangedEvent indices are correct

## 6. Thread Safety (if feasible)

### 6.1 Concurrent reads
- Multiple threads calling getRoot(), getNodesForSource(), hasConsumed() concurrently -> no errors

### 6.2 Read during write
- One thread adding sources while another reads -> no ConcurrentModificationException
- FilterNode.getOutputSources() returns a safe snapshot

## 7. Edge Cases

- Add source to empty model
- Remove source not in model -> no error
- Remove sources when model is empty -> no error
- Add SpimData with zero sources
- addBdvHandle when no sources registered yet
- refreshBdvHandleNode when BdvHandleFilterNode has been removed (race condition)

## Implementation Notes

- Test class location: `src/test/src/sc/fiji/bdvpg/` (non-standard path from pom-scijava)
- Tests for FilterNode and SourceTreeModel can be pure unit tests
- BdvHandle tests will need mocking since BdvHandle requires Swing; consider using Mockito or a test double for ViewerState
- Use `@Before` to set up a fresh SourceTreeModel for each test
- Consider a helper that creates a minimal SourceAndConverter for testing (see existing test utilities)
