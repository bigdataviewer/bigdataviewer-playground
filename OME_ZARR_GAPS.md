# OME-Zarr / OME-NGFF support in BDV-Playground — gap analysis

Status as of 2026-07-22, against the stack resolved by pom-scijava 45.0.0:
n5-viewer_fiji 6.2.0, n5-universe 3.0.2, n5-zarr 2.0.1, n5 4.0.1, bigdataviewer-core 10.6.11.

## What already works

Opening. Validated end-to-end on real remote data (`OpenOmeZarrDemo`), both
OME-NGFF v0.4 (Zarr v2, `ZarrKeyValueReader`) and v0.5 (Zarr v3,
`ZarrV3KeyValueReader`). The path is:

```
URI -> N5Factory.openReader() -> N5Reader
    -> N5DatasetDiscoverer(n5vParsers, n5vGroupParsers) -> N5TreeNode / N5Metadata
    -> N5Viewer.buildN5Sources(...) -> List<SourceAndConverter>
    -> SourceService.register(sac)
```

No XML, no SpimData, no `XmlIoBasicImgLoader`. What bigdataviewer-core
contributed is the `bdv.img.n5.N5Properties` generalisation, which lets
`N5ImageLoader` sit on an arbitrary `N5Reader` backend. `XmlIoN5ImageLoader`
(`bdv.n5` / `bdv.n5.cloud`) still exists but only serves BDV's own N5 layout —
it is not an OME-Zarr entry point.

The resulting sources are correct multiresolution pyramids
(`MetadataMipmapSource` wrapped in `bdv.tools.transformation.TransformedSource`),
and `buildN5Sources` splits a channel axis into one source per channel.
No POM change is needed — the whole stack arrives transitively via
bigwarp_fiji -> bigdataviewer_fiji.

## The core problem

SpimData is a *sample-description model*: `ViewSetup`s carrying typed `Entity`
attributes, `ViewRegistration`s, timepoints, missing views. OME-NGFF is an
*array-packaging spec*: a pyramid of arrays plus axes plus a coordinate
transform. It deliberately does not model tiles, angles, illuminations, or
user-defined entities.

So the metadata is not "somewhere else in the Zarr" — for most of it, **it is not
in the spec at all**. Each gap below therefore needs a decision: map onto an
existing NGFF concept, read it from a sidecar (OME-XML / `bioformats2raw`
layout), or store it Playground-side in custom Zarr attributes.

---

## Gap list

### 1. Entity metadata (Tile / Channel / Angle / Illumination / custom)

**Verified:** `org.janelia.saalfeldlab.n5.universe.metadata.ome.ngff` contains
no class modelling any of these. `OmeNgffMultiScaleMetadata` has exactly
`version, name, type, axes, datasets, coordinateTransformations, metadata`.

Consequences in this codebase:

- `SourceService` never populates `SPIM_DATA_INFO`, so every consumer of it is
  inert for Zarr sources.
- `EntityHandler` plugins (`sc.fiji.bdvpg.dataset.EntityHandler`) are never
  invoked — both `loadEntity` overloads take an `AbstractSpimData` +
  `BasicViewSetup`, neither of which exists on this path. **The entire custom
  entity extension mechanism is bypassed.**
- The source tree (`SourceTree`, `EntityFilterNode`, `SpimDataFilterFactory`)
  cannot build "group by Channel / Tile / Angle" filter nodes. Zarr sources land
  as a flat list.
- Channel identity survives only as a *name suffix* (`..._ch0`, `..._ch1`)
  invented by `buildN5Sources`. It is not queryable, not typed, and channel
  *names* from the file are lost entirely (see gap 2).

What to decide:
- Whether to widen `EntityHandler` (or add a sibling SPI) so entities can be
  derived from an `N5Reader` + metadata path, not only from SpimData.
- Whether to synthesise a lightweight `SpimData` in front of the Zarr sources so
  the whole existing entity/tree/export machinery keeps working unchanged. This
  is the higher-leverage option and should be evaluated first.
- Where the Playground writes its *own* entities on save — a custom attribute
  namespace in `.zattrs` / `zarr.json` is the natural home.

### 2. Channel names, colors and contrast limits (`omero` block)

**Verified:** n5-universe 3.0.2 has no `omero` metadata class. The `omero` block
of NGFF 0.4 — `channels[].label`, `.color`, `.window.{min,max,start,end}` — is
therefore **not parsed at all**, even though most IDR datasets carry it.

This is the most visible user-facing loss and probably the cheapest win: read
`omero` directly off the group attributes and apply it via the existing
`ColorChanger` / `BrightnessAdjuster`. Note NGFF 0.5 deprecates `omero`, so a
0.5-era replacement has to be handled separately (or accepted as absent).

Until then the demo has to guess contrast with `BrightnessAutoAdjuster`, which
is a per-open heuristic, not the acquisition's real display settings.

### 3. Serialization / session round-trip

`SpimSourceAdapter` serialises a source purely by reference:
`{spimdata, viewsetup}` resolved through `SPIM_DATA_INFO`. Zarr sources are
`MetadataMipmapSource` inside a `TransformedSource`, have no backing SpimData,
and have **no registered `ISourceAdapter` at all** — so a Playground session
containing an OME-Zarr source will not round-trip.

Needs a new `@Plugin(type = ISourceAdapter.class)` recording at minimum:
container URI, dataset/multiscale path, channel index, and enough to rebuild the
same `SharedQueue`-backed source. Design questions:
- Dedupe: N containers must not be reopened N x channels times on deserialize —
  mirror how `SpimDataAdapter` caches the `AbstractSpimData` instance.
- Relative vs absolute URIs, and remote credentials (see gap 7).
- Version pinning: does a session record which NGFF version it was opened as?

See `ADAPTER_TESTING_STATUS.md` for the adapter inventory this must join.

### 4. Coordinate transforms beyond scale+translation

NGFF 0.4 core allows only `scale` and `translation`. NGFF 0.5 in n5-universe is
richer than that — the jar ships `v05/transformations/` with
`AffineCoordinateTransform`, `ScaleOffsetCoordinateTransform`,
`StackedCoordinateTransform`, `RealTransformCoordinateTransform`,
`ThinPlateSplineCoordinateTransform`, plus a `v05/graph/` coordinate-system
package.

That is a genuine opportunity and a genuine gap: the Playground already has
thin-plate-spline and transform-sequence adapters
(`net.imglib2.realtransform.*`), so there is a plausible bridge between NGFF 0.5
transforms and Playground `RealTransform`s in both directions. Nothing currently
uses it — `buildN5Sources` collapses everything to the `TransformedSource`
affine. Worth scoping how much of a registration result could be persisted as
standards-compliant NGFF 0.5 rather than Playground-private JSON.

### 5. Writing / export

Export today is `SourcesToXMLHDF5Exporter` / `DatasetToXMLExporter` — XML+HDF5
only. There is no OME-Zarr writer. `OmeNgffMetadata.buildForWriting(...)` exists
in n5-universe and is the obvious foundation, but the surrounding questions are
open: pyramid generation, chunking, compression, sharding (Zarr v3), and which
NGFF version to emit by default.

Round-tripping *entities* through a writer is blocked on gap 1.

### 6. Multi-image containers and HCS

**Verified:** no plate/well/HCS metadata class in n5-universe 3.0.2.

Two container layouts matter and neither is handled:
- **`bioformats2raw.layout=1`** — multi-series containers with a sidecar
  `OME/METADATA.ome.xml`. This is the most likely real-world source of true tile
  and channel metadata for BIOP users, and reading that OME-XML is arguably the
  most direct answer to "how do I get tiles and channels from Zarr". Worth
  treating as a first-class path rather than a footnote.
- **HCS plate/well** — `plate` / `well` groups. Currently the tree walk in
  `OpenOmeZarrDemo.collect()` would descend into every well and open everything,
  which does not scale to a real plate.

Also unhandled: NGFF `labels/` groups (segmentations), which should probably
become sources with a label converter rather than plain intensity sources.

### 7. Plumbing

- **No SciJava command.** Opening is demo-only; there is no menu entry parallel
  to `DatasetXMLLoadCommand` / `XMLToDatasetImporter`. Needs a URI widget, and
  ideally the n5-viewer dataset-selection dialog for multi-image containers.
- **Cache.** The Playground has a global cache (`sc.fiji.bdvpg.cache`) shared
  across datasets; `buildN5Sources` is handed its own `new SharedQueue(8)`.
  These should be reconciled or the Playground's cache accounting is wrong for
  Zarr sources.
- **Credentials.** `N5Factory` supports S3 auth; nothing in the Playground
  surfaces or persists it. Interacts with gap 3 — sessions must not serialise
  secrets.
- **Timepoints.** NGFF models time as an axis inside one array; SpimData models
  it as a first-class dimension with per-timepoint registrations. Confirm how
  `buildN5Sources` maps a `t` axis onto `Source.getSource(t, level)` and whether
  the Playground's timepoint-aware code paths behave.
- **Error handling.** Remote opens fail slowly and in many ways (404, auth,
  unsupported codec). Currently a stack trace to stdout.

---

## Suggested order

1. Gap 2 (`omero`) — cheap, immediately visible, unblocks realistic screenshots.
2. Gap 7 command — makes the feature reachable by users.
3. Gap 1 decision (synthetic SpimData vs. widened `EntityHandler`) — this is the
   architectural fork; gaps 3, 5 and 6 all depend on the answer, so it should be
   decided before writing the adapter.
4. Gap 3 adapter.
5. Gaps 4, 5, 6 as scoped projects.

## Open questions to resolve first

- Is a synthetic `AbstractSpimData` wrapper around Zarr sources acceptable, or
  is the intent to move the Playground *away* from SpimData as the metadata
  backbone? Everything downstream hinges on this.
- Should Playground-specific metadata be written into the Zarr container
  (portable, pollutes the container) or kept in the session JSON (clean, but
  metadata is lost outside the Playground)?
- Which NGFF version is the write target?