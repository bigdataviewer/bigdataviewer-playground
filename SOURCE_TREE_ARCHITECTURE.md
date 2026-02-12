# Source Tree View Architecture

This document describes the architecture of the Source Tree View UI, which displays and manages `SourceAndConverter` objects in a hierarchical tree structure.

## Overview

The Source Tree View uses a **Model-View separation** pattern to decouple the data logic from Swing UI concerns. This enables:

- Thread-safe batch operations
- Efficient incremental UI updates
- Testable business logic without GUI dependencies

```
SourceAndConverterService (batch events)
         │
         ▼
  SourceTreeModel (thread-safe, pure data)
         │  fires SourcesChangedEvent / StructureChangedEvent
         ▼
   SourceTreeView (Swing, EDT-only)
         │
         ▼
      JTree
```

## Package Structure

All tree-related classes are in `sc.fiji.bdvpg.scijava.services.ui.tree`:

| Class | Responsibility |
|-------|----------------|
| `FilterNode` | Pure data model for filter nodes (no Swing) |
| `SpimDataFilterNode` | Filters sources by SpimData membership |
| `EntityFilterNode` | Filters sources by SpimData entities (Channel, Angle, etc.) |
| `BdvHandleFilterNode` | Filters sources by BdvHandle presence |
| `SourceTreeModel` | Thread-safe model with batch operations |
| `SourceTreeView` | Swing view with incremental updates |
| `SpimDataFilterFactory` | Creates SpimData filter hierarchies |
| `SourcesChangedEvent` | Batch event for source changes |
| `StructureChangedEvent` | Event for tree structure changes |
| `SourceTreeModelListener` | Listener interface |

## Design Principles

### 1. Model-View Separation

**FilterNode** is a pure data class with no Swing dependencies:

```java
public class FilterNode {
    private String name;
    private Predicate<SourceAndConverter<?>> filter;
    private final boolean displaySources;
    private FilterNode parent;
    private final List<FilterNode> children;
    private final Set<SourceAndConverter<?>> inputSources;
    private final Set<SourceAndConverter<?>> outputSources;
}
```

This contrasts with the previous `SourceFilterNode` which extended `DefaultMutableTreeNode`, mixing data logic with Swing.

### 2. Thread Safety via ReadWriteLock

`SourceTreeModel` uses a `ReentrantReadWriteLock` to allow:
- Multiple concurrent reads
- Exclusive writes

```java
public class SourceTreeModel {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void addSources(Collection<SourceAndConverter<?>> sources) {
        lock.writeLock().lock();
        try {
            // ... modify model
            fireSourcesChanged(event);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public FilterNode getRoot() {
        lock.readLock().lock();
        try {
            return root;
        } finally {
            lock.readLock().unlock();
        }
    }
}
```

### 3. Batch Operations

Instead of updating the UI for each source individually, batch operations collect all changes and fire a single event:

```java
// OLD: N UI updates for N sources
for (SourceAndConverter<?> sac : sources) {
    ui.update(sac);  // triggers tree reload each time
}

// NEW: 1 UI update for N sources
ui.addSources(sources);  // single batch event
```

This dramatically improves performance when registering SpimData with many sources.

### 4. Incremental UI Updates

`SourceTreeView` uses `nodesWereInserted()` and `nodesWereRemoved()` instead of `model.reload()`:

```java
// OLD: Full tree reload (expensive, loses expansion state)
model.reload(node);

// NEW: Incremental update (efficient, preserves state)
treeModel.nodesWereInserted(parentNode, indices);
treeModel.nodesWereRemoved(parentNode, indices, removedNodes);
```

### 5. Event-Driven Updates

Events carry all information needed for efficient updates:

```java
public class SourcesChangedEvent {
    public enum Type { ADDED, REMOVED, UPDATED }
    private final Type type;
    private final Collection<SourceAndConverter<?>> sources;
    // Maps each affected FilterNode to its affected sources
    private final Map<FilterNode, List<SourceAndConverter<?>>> affectedNodes;
}
```

The `affectedNodes` map tells the view exactly which nodes need updating.

### 6. EDT Safety

All Swing operations happen on the Event Dispatch Thread:

```java
@Override
public void sourcesChanged(SourcesChangedEvent event) {
    SwingUtilities.invokeLater(() -> applySourcesChanged(event));
}
```

### 7. Thread-Safe FilterNode with Defensive Copies

Since events are processed asynchronously on the EDT (via `invokeLater`), the model's write lock is released before the view processes the event. Another thread could modify the model's collections while the view is iterating.

**Solution**: `FilterNode` is internally synchronized and returns thread-safe copies:

```java
public class FilterNode {
    // All public methods are synchronized
    public synchronized List<FilterNode> getChildren() {
        return new ArrayList<>(children);  // Returns a copy
    }

    public synchronized Set<SourceAndConverter<?>> getOutputSources() {
        return new LinkedHashSet<>(outputSources);  // Returns a copy
    }

    // Package-private modifiers are also synchronized
    synchronized void addChild(FilterNode child) { ... }
    synchronized boolean addSource(SourceAndConverter<?> sac) { ... }
}
```

This means the view can safely iterate without additional copying:

```java
// Safe: getChildren() returns a thread-safe snapshot
for (FilterNode child : filterNode.getChildren()) { ... }

// Safe: getOutputSources() returns a thread-safe snapshot
List<SourceAndConverter<?>> sorted = SourceAndConverterHelper.sortDefaultGeneric(
    filterNode.getOutputSources());
```

**Key principle**: Thread safety is enforced at the `FilterNode` level, making the API simpler for callers.

### 8. O(1) Lookup via Indexes

`SourceTreeModel` maintains indexes for fast lookups:

```java
// Source -> FilterNodes containing it
private final Map<SourceAndConverter<?>, Set<FilterNode>> sourceIndex;

// SpimData -> its filter node
private final Map<AbstractSpimData<?>, SpimDataFilterNode> spimDataIndex;

// BdvHandle -> its filter node
private final Map<BdvHandle, BdvHandleFilterNode> bdvHandleIndex;
```

`SourceTreeView` maintains bidirectional mappings:

```java
// FilterNode -> Swing TreeNode
private final Map<FilterNode, DefaultMutableTreeNode> filterToTreeNode;

// Source -> Swing TreeNodes displaying it
private final Map<SourceAndConverter<?>, Set<DefaultMutableTreeNode>> sourceToTreeNodes;
```

### 9. Sorting in View Only

The Model stores sources in insertion order. Sorting is a View concern:

```java
// In SourceTreeView
List<SourceAndConverter<?>> sortedSources =
    SourceAndConverterHelper.sortDefaultGeneric(filterNode.getOutputSources());
```

This keeps the Model simple and allows different Views to use different sort orders.

## Usage Examples

### Adding Sources (Batch)

```java
// In SourceAndConverterService
public synchronized void registerBatch(Collection<SourceAndConverter<?>> sources) {
    List<SourceAndConverter<?>> newSources = new ArrayList<>();
    for (SourceAndConverter<?> sac : sources) {
        if (!isRegistered(sac)) {
            // ... register source
            newSources.add(sac);
        }
    }
    // Single UI update for all sources
    if (!newSources.isEmpty()) {
        ui.addSources(newSources);
    }
}
```

### Registering SpimData

```java
// Collect all sources first
List<SourceAndConverter<?>> allSources = new ArrayList<>();
setupIdToSourceAndConverter.keySet().forEach(id -> {
    SourceAndConverter<?> sac = setupIdToSourceAndConverter.get(id);
    register(sac, "no tree");  // register without UI update
    linkToSpimData(sac, asd, id);
    allSources.add(sac);
});

// Single batch UI update
ui.addSources(allSources);
```

### Adding Custom Filter Nodes

```java
SourceTreeModel model = ui.getSourceTreeModel();
FilterNode parent = model.getRoot();
FilterNode customFilter = new FilterNode("My Filter",
    sac -> sac.getSpimSource().getName().contains("GFP"),
    true);  // displaySources = true
model.addNode(parent, customFilter);
```

## Migration Notes

The public API of `SourceAndConverterServiceUI` is preserved for backward compatibility:

| Method | Behavior |
|--------|----------|
| `update(sac)` | Adds single source (still works, less efficient) |
| `addSources(collection)` | **New**: Batch add with single UI update |
| `remove(sac)` | Removes single source |
| `removeSources(collection)` | **New**: Batch remove |
| `getTreeModel()` | Returns Swing `DefaultTreeModel` |
| `getSourceTreeModel()` | **New**: Returns thread-safe `SourceTreeModel` |

The old `SourceFilterNode`, `SpimDataFilterNode` (old), `SpimDataElementFilter`, and `BdvHandleFilterNode` (old) classes in `sc.fiji.bdvpg.scijava.services.ui` are no longer used by the UI but remain for any external code that may reference them.
