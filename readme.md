# BigDataViewer-Playground

[![](https://github.com/bigdataviewer/bigdataviewer-playground/actions/workflows/build-main.yml/badge.svg)](https://github.com/bigdataviewer/bigdataviewer-playground/actions/workflows/build-main.yml)
[![Maven Scijava Version](https://img.shields.io/github/v/tag/bigdataviewer/bigdataviewer-playground?label=Version-[Maven%20Scijava])](https://maven.scijava.org/#browse/browse:releases:sc%2Ffiji%2Fbigdataviewer-playground)

**BigDataViewer-Playground** (bdv-playground) is a Java library that extends
[BigDataViewer](https://imagej.net/plugins/bdv/) with a service layer, a GUI, and a
large set of actions for managing and displaying multi-dimensional image sources in
[Fiji](https://fiji.sc)/ImageJ2.

It centralizes the handling of `SourceAndConverter` objects — registering them, organizing
them in a tree, transforming and resampling them, and displaying them across multiple
BigDataViewer (BDV) and BigVolumeViewer (BVV) windows — and exposes most of this both as a
programmatic API and as SciJava commands in the Fiji menus.

> **Project status.** This is a mature, actively maintained library (first released in 2019).
> It started as an open collaborative "playground" of BDV actions; today it is primarily used
> as a building block — a dependency that a handful of downstream Fiji/ImageJ2 projects rely on
> for their source management, display and registration needs. Development is driven by those
> concrete use cases rather than by a broad feature roadmap. Contributions and bug reports are
> still welcome (see [Issues](https://github.com/bigdataviewer/bigdataviewer-playground/issues)),
> but expect the scope to track what the dependent projects need.

---

## What it provides

- **A central source registry** — `SourceAndConverterService` keeps track of every
  `SourceAndConverter`, the metadata attached to it, and the `SpimData` it came from. A global,
  bounded image-data cache is shared across all datasets.
- **Multi-window display management** — `SourceAndConverterBdvDisplayService` tracks several
  BDV/BVV windows at once and the visibility of each source in each window.
- **A source-management GUI** — a hierarchical, filterable tree view of all sources, grouped by
  dataset, channel/angle/other entities, and by the window they are shown in (see
  [`SOURCE_TREE_ARCHITECTURE.md`](SOURCE_TREE_ARCHITECTURE.md)).
- **~57 SciJava commands** for importing, displaying, transforming, resampling, coloring,
  registering (BigWarp), and exporting sources — all reachable from
  `Plugins > BigDataViewer-Playground` and listed in
  [`DOCUMENTATION_STATUS.md`](DOCUMENTATION_STATUS.md).
- **Workspace serialization** — save and reload the full state (sources, transforms, display
  settings) to/from JSON, via a set of Gson adapters
  (see [`ADAPTER_TESTING_STATUS.md`](ADAPTER_TESTING_STATUS.md)).

---

## Using it as a dependency

bdv-playground is published to the [SciJava Maven repository](https://maven.scijava.org). Most
consumers use it as a Maven artifact.

```xml
<dependency>
    <groupId>sc.fiji</groupId>
    <artifactId>bigdataviewer-playground</artifactId>
    <version>0.21.0</version> <!-- check the badge above / Maven for the latest release -->
</dependency>
```

Add the SciJava repository if your POM does not already inherit it from `pom-scijava`:

```xml
<repository>
    <id>scijava.public</id>
    <url>https://maven.scijava.org/content/groups/public</url>
</repository>
```

### Inside Fiji

The commands are also available in a running Fiji instance under the
`Plugins > BigDataViewer-Playground` menu (with sub-menus `Workspace`, `Import`, `Display`,
`Dataset`, `Process`, `Register`, `Export`, `BDV`, `BVV`). When bdv-playground is pulled in as a
transitive dependency of another Fiji plugin, these menus appear automatically.

---

## Programmatic usage

Everything is driven by a SciJava `Context`. Services can be obtained from the context, or
statically through `SourceAndConverterServices` when no context is injected.

```java
// Create a SciJava context with all services
Context ctx = new Context();
ctx.service(UIService.class).showUI(SwingUI.NAME);

// Create a BDV window managed by the display service
SourceBdvDisplayService display = ctx.getService(SourceBdvDisplayService.class);
display.getNewBdv();
BdvHandle bdvh = display.getActiveBdv();

// Add a custom action to the BDV window's menu
BdvMenuHelper.addActionToBdvHandleMenu(
        bdvh, "Greetings>Say Hello!", 0,
        () -> bdvh.getViewerPanel().showMessage("Hello!"));
```

Because windows are *registered* with the display service, any source or `BdvHandle` becomes
discoverable by the rest of the framework — for example as a SciJava `@Parameter` typed
`SourceAndConverter[]` or `BdvHandle` in your own commands.

Runnable demos covering most features live in
[`src/test/src/sc/fiji/bdvpg/demos`](src/test/src/sc/fiji/bdvpg/demos) — creation, navigation,
synchronization, resampling, transforms, BigWarp registration, import/export, and more. They are
the best starting point for learning the API.

---

## Architecture in brief

- **Two SciJava services** form the backbone:
  `SourceAndConverterService` (registry + metadata + cache) and
  `SourceAndConverterBdvDisplayService` (display across windows).
  `SourceAndConverterServices` provides static access to both.
- **Sources are wrapped** with their converters and can be chained
  (`WarpedSource` → `TransformedSource` → `ResampledSource` → …);
  `SourceAndConverterHelper.getRootSource()` walks back to the root.
- **Metadata** is stored in weak-key Guava caches so sources can be garbage-collected.
- **Actions** follow a `Runnable` pattern (see the coding guidelines below); higher-level
  operations are exposed as SciJava `Command`s implementing `BdvPlaygroundActionCommand`, which
  are auto-registered as contextual menu actions.
- **Serialization** uses Gson with custom adapters (sources, converters, real transforms,
  `SpimData` references, BDV options), registered as SciJava plugins and obtained via
  `ScijavaGsonHelper.getGson(context)`.

A deeper description is in [`CLAUDE.md`](CLAUDE.md) and the architecture documents linked above.

---

## Building and testing

The project targets **Java 8** and inherits from `pom-scijava`.

```bash
mvn clean install              # build + run tests
mvn clean install -DskipTests  # build only

mvn test -Dtest=sc.fiji.bdvpg.source.SourceAndConverterHelperTest   # a single test class
```

Test sources live under the non-standard path `src/test/src/sc/fiji/bdvpg/` (a `pom-scijava`
convention). Tests fall into three groups: pure unit tests (math, serialization), headless
integration tests that need an ImageJ context but no GUI, and interactive `*Demo` classes that
launch a UI and are normally run by hand.

---

## Coding guidelines

Atomic functionality is implemented as a class implementing
[`Runnable`](https://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html):

- Mandatory parameters are passed as constructor arguments.
- Optional parameters are set with setters.
- `run()` performs the action; results are retrieved with getters (which may call `run()`
  internally if needed).

```java
class Action implements Runnable {
    Action(BdvHandle bdvh) { /* ... */ }
    void setParameter(Object p) { /* ... */ }
    public void run() { /* ... */ }
    Object getResult() { /* ... */ }
}
```

Higher-level operations that chain several actions are implemented as SciJava `Command`s, which
also makes them available to end users through the Fiji menus and to other scripting languages.

---

## License, authors and citation

Released under the **Simplified (2-clause) BSD License** — see [`LICENSE.txt`](LICENSE.txt).

Developed and maintained by Nicolas Chiaruttini (EPFL), Robert Haase (MPI-CBG) and
Christian Tischer (EMBL), with contributions from the imaging community.

Questions and discussion: the [image.sc forum](https://forum.image.sc/).