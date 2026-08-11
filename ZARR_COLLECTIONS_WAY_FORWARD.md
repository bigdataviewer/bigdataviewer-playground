# Collection tables & RFC-8 — does this change the way forward?

Follow-up to [`ZARR_METADATA_LANDSCAPE.md`](ZARR_METADATA_LANDSCAPE.md), prompted by
Tischi's (MoBiE author) reply on the forum:

> in MoBiE these days we do less and less of the JSON-based projects, but mainly
> using the MoBiE **collection table**
> (<https://mobie.github.io/tutorials/mobie_collection_table.html>). The OME-Zarr
> **collection spec RFC-8** is also relevant here
> (<https://ngff.openmicroscopy.org/rfc/8/index.html>).

His two pointers are **the same idea at two different maturity levels**, and that
idea is precisely the layer your SpimData XML occupies. This note explains what
each one is, whether it's spec or convention, how it maps onto your model, and
what (if anything) it should change about your plan. Short version up front:
**staying on the XML adapter is the right call for now** — but for a good reason,
and with one cheap addition worth making.

---

## TL;DR

- The **MoBiE collection table** is a *flat table* (Excel/CSV/TSV/Google Sheet),
  one row per source, that carries display + grouping + **transforms** (affine,
  TPS, elastix b-spline, displacement field) **outside** the Zarr. It is **MoBiE
  convention, not NGFF spec** — the columns are hard-coded constants in
  `CollectionTableConstants.java`. It ships today (`OpenCollectionTableCommand`).
- **RFC-8 "Collections"** is the *in-progress NGFF standardisation* of exactly
  that layer: a group node listing member images with per-member `attributes`
  (transforms via **RFC-5**, labels, HCS, display, and **prefixed custom
  metadata**). Status: **draft D1, "not implemented yet", not backwards
  compatible.**
- Both are the answer to the question the gap analysis kept hitting —
  *"where does the multi-image / registration / display layer live?"* — that NGFF
  core had no home for. RFC-8 is the first time NGFF proposes to give it one.
- **What it does NOT give you**, in either form: typed acquisition entities
  (Tile/Angle/Illumination), per-timepoint registration *chains*, `MissingViews`,
  or your `EntityHandler` SPI. That model is still richer than a collection.
- **Way forward:** keep SpimData/XML as your internal backbone; add a
  collection-table **reader** as a cheap import path (it hands you channels +
  display + transforms + grouping essentially for free); design your own session
  format *collection-shaped* so RFC-8 is a cheap migration if it lands; park
  entities under a `bdvplayground:` prefixed namespace (RFC-8 explicitly blesses
  this) rather than inventing a private container layout.

---

## 1. Why these two are one thing

Everything in the landscape survey pointed at the same hole: NGFF describes a
*single* array pyramid (axes + scale/translation + omero), and **every tool that
needs to combine several images and register them reinvents a layer on top** —
BigStitcher in SpimData XML, MoBiE historically in `mobie-views.json`,
multiview-stitcher in xarray `transform_key`s. RFC-8 is the OME project finally
trying to **standardise that layer inside the container**, and the MoBiE
collection table is Tischi's **pragmatic, shipping-today version of the same
concept as a sidecar table**.

So the mental model is simple:

```
single OME-Zarr image  =  pixels + axes + scale/translation + omero   (spec, stable)
        ── the layer above it ──
collection / project    =  list of images + transforms + display + grouping
                           MoBiE JSON project      → legacy, being retired
                           MoBiE collection table  → convention, shipping now
                           RFC-8 collection         → spec, draft/unimplemented
                           SpimData XML             → your backbone, richest of all
```

RFC-8 does **not** change how you *open a single image* — your
`buildN5Sources` recipe is untouched. It changes what the *container/project*
around several images is allowed to say.

---

## 2. The MoBiE collection table — convention, but a good one

Source of truth (verified on `main`):
[`CollectionTableConstants.java`](https://github.com/mobie/mobie-viewer-fiji/blob/main/src/main/java/org/embl/mobie/lib/table/columns/CollectionTableConstants.java),
opened by
[`OpenCollectionTableCommand`](https://github.com/mobie/mobie-viewer-fiji/blob/main/src/main/java/org/embl/mobie/command/open/OpenCollectionTableCommand.java),
consumed by
[`CollectionDataSetter`](https://github.com/mobie/mobie-viewer-fiji/blob/main/src/main/java/org/embl/mobie/lib/data/CollectionDataSetter.java).
Tutorial: <https://mobie.github.io/tutorials/mobie_collection_table.html>.

One row = one source. Only `uri` is required. The columns (verbatim constants):

| Column | Role | Your equivalent |
|---|---|---|
| `uri` | image/label/spots location (path or URL) | container URI |
| `type` | `intensities` / `labels` / `spots` | converter type (intensity vs label) |
| `channel` | 0-based channel index into a multi-channel OME-Zarr | your `{…, channel index}` session key |
| `name` | display name | source name |
| `color` | e.g. `r(0)-g(255)-b(0)-a(255)` or a name | `ColorChanger` |
| `contrast_limits` | `(min,max)` or `auto` | `BrightnessAdjuster` |
| `blend` | `sum` (additive) / `alpha` | converter blending |
| `affine` | row-packed 3×4 matrix | `AffineTransform3D` on a `TransformedSource` |
| `thin_plate_spline` / `_uri` | BigWarp TPS (inline JSON or file) | your TPS `RealTransform` adapter |
| `elastix_bspline_uri` | elastix b-spline file | (no equivalent yet) |
| `displacement_field_uri` | displacement field | (no equivalent yet) |
| `view` | free-text: sources sharing a coordinate system | a synced BdvHandle / group |
| `display` | shared UI settings across rows | converter-setup sharing |
| `group` | drop-down grouping in the UI | source-tree group node |
| `grid` / `grid_position` | 2D grid layout `(x,y)` | (HCS/grid layout) |
| `exclusive` | replace currently shown sources | — |
| `format` | force `OmeZarr` when extension is absent | — |
| `bounding_box`, `spot_radius`, `labels_table` | spots/labels extras | — |

Observations that matter to you:

1. **It is application convention, full stop.** None of these column names come
   from NGFF; they're MoBiE constants. That's not a criticism — it's the honest
   state of the art. The transform layer *has* to live outside the array because
   NGFF still has no affine (ome/ngff#94), so MoBiE put it in a table.
2. **It is transform-rich in exactly the way you are.** `affine`,
   `thin_plate_spline`, `elastix_bspline`, `displacement_field` — this is the
   same "registration results don't fit in scale+translation" problem you already
   solved with your `net.imglib2.realtransform.*` adapters. A collection table is,
   functionally, MoBiE's `ViewRegistration` equivalent.
3. **`channel` per row is exactly your serialization-by-reference key** (forum
   Q5): `{uri, channel}`. MoBiE reached the same design. Worth noting the tutorial
   links the very image.sc thread about "loading only one channel from an OME-Zarr".
4. **What it does NOT have:** no typed Tile/Angle/Illumination, no *chained*
   per-step transforms (it's one affine + optionally one warp per row, not
   calibration→translation→stitch→register as identifiable steps), no timepoint
   dimension in the table (time stays an axis inside each array), no custom-entity
   extension point. It's flatter than SpimData by design.

**Bottom line on the table:** it's the lightest possible thing that carries
channels + display + transforms + grouping, and it's trivial to parse. That makes
it an attractive *import* format — but it is not a richer model than what you
already have.

---

## 3. RFC-8 — the spec version, and its real status

Spec: <https://ngff.openmicroscopy.org/rfc/8/index.html>. Authors span scalable
minds (Webknossos), BioVisionCenter Zurich (Fractal), EMBL/EMBL-EBI, German
BioImaging. **State: D1 (first draft). The RFC explicitly says "this RFC has not
been implemented yet," and it is not backwards compatible.** So it is a
direction-of-travel signal, not a build target for 2026.

What it actually proposes — a `collection` node in `zarr.json` (or a standalone
JSON) listing member `nodes`:

```json
{
  "ome": {
    "version": "0.x",
    "type": "collection",
    "name": "example",
    "nodes": [
      { "type": "multiscale", "name": "tile_0",
        "path": { "type": "zarr", "path": "./tile_0.ome.zarr" },
        "attributes": { "coordinateTransformations": [ /* RFC-5 */ ] } },
      { "type": "multiscale", "name": "seg",
        "attributes": { "labels": { "source": [ { "id": "tile_0" } ] } } }
    ],
    "attributes": {
      "scene": { "coordinateSystems": [...], "coordinateTransformations": [...] }
    }
  }
}
```

The parts that change the picture from the gap analysis:

- **A node can reference a *remote* image by URL** (`path.type: "zarr"` / `"json"`)
  — collections can span storage systems without copying. This is the standard
  form of your `{container URI, path, channel}` reference.
- **Per-member `attributes` carry RFC-5 coordinate transformations** at both the
  single-image level and a collection-level `scene`. **This is the first time
  affine/TPS/sequence transforms get an NGFF-native home** — precisely gap 4 and
  the ome/ngff#94 hole. RFC-8 is the container that makes RFC-5 usable.
- **HCS `plate`/`well` and `bioformats2raw` series are folded into this** — a
  plate becomes a collection-of-collections, a series becomes a collection of
  multiscales. So gaps 6 (HCS/multi-image) get a single unified answer.
- **`labels` become m:n references** (a label node points at its source images)
  instead of a rigid child group — relevant if you ever model segmentations.
- **Explicit prefixed extension points.** Node types, attribute keys, path types,
  and transform types may be namespaced: `mobie:table`, `fractal:roi`,
  `neuroglancer:shader`, `myorg:nonlinear`. *"Unprefixed identifiers are reserved
  for the core spec."*

That last bullet is the direct answer to your **forum question 3 (custom
entities)**: the emerging convention is a **prefixed attribute namespace**. In an
RFC-8 world your Tile/Channel/Angle/Illumination and user `EntityHandler` types
would live under something like `bdvplayground:entities` on a node — sanctioned,
not a private hack.

What RFC-8 still does **not** give you, even fully realised:

- **Typed acquisition entities** as first-class citizens — you'd carry them in a
  `bdvplayground:` block, i.e. still your model, just stored in-container.
- **Per-timepoint registration** — RFC-5/RFC-8 transforms are not indexed by a
  time coordinate; SpimData's per-timepoint `ViewRegistration` remains strictly
  more expressive (same conclusion as multiview-stitcher, which needed its own
  time-resolved `transform_key`s).
- **`MissingViews`** and the setup/timepoint sparsity model.

So even the *spec* endpoint doesn't retire your SpimData model — it standardises
the *storage* of the grouping+transform+display layer, and offers a blessed
cubby-hole (`bdvplayground:`) for the rest.

---

## 4. Does this change the gap analysis?

Mapping onto [`OME_ZARR_GAPS.md`](OME_ZARR_GAPS.md):

| Gap | Before (survey) | With collections in view |
|---|---|---|
| 1. Entity metadata | "not in the spec at all" | Still not modelled by the spec — but RFC-8 gives a **sanctioned prefixed namespace** to store *your* entities; the collection table shows a working flat approximation (channel/group/view). |
| 2. omero display | Java stack ignores it | Collection table carries `color`/`contrast_limits`/`blend` per row; RFC-8 carries display in `attributes`. Confirms display belongs in the *project* layer, not necessarily the array. Your cheap `omero` read is still the right first step. |
| 4. Rich transforms | RFC-5 ≠ 0.5; no home | RFC-8 is the **home** for RFC-5 transforms. Draft, but this is the standards path for persisting registration results. |
| 6. HCS / multi-image | unhandled, no class | RFC-8 unifies HCS + series + grouping under one `collection` concept. Still draft. |
| 3. Session round-trip | needs a new adapter | A collection (table now, RFC-8 later) is essentially *your session format expressed portably*. Design yours to be collection-shaped. |

Nothing here invalidates the survey; it **names the destination** the survey kept
circling. The core thesis holds: registration/grouping/display live above the
array — RFC-8 is the proposal to give "above the array" a spec.

---

## 5. Recommendation — you're right to stay on XML, here's the sharpened why

Your instinct ("I'll probably stay with the XML adapter") is correct. Reasons,
now that we've seen the alternatives:

1. **XML/SpimData is still the richest model on the table.** Nothing above —
   collection table or RFC-8 — carries typed entities, per-timepoint registration
   chains, missing views, or your `EntityHandler` SPI. Switching backbones would
   be a downgrade, not an upgrade.
2. **RFC-8 is D1 and explicitly unimplemented.** Building on it now means tracking
   a moving, backwards-incompatible target. Not a 2026 dependency.
3. **The collection table is convention, single-vendor.** Great to *read*, risky
   to adopt as your one true format.

But "stay on XML" shouldn't mean "ignore this." Three concrete, low-risk moves:

**(a) Add a collection-table *reader* (do this — best effort/reward ratio).**
It's a table parse. Each row → a `SourceAndConverter`: `uri`+`channel` via your
existing `buildN5Sources` path, `color`/`contrast_limits` via
`ColorChanger`/`BrightnessAdjuster`, `affine`→`TransformedSource`,
`thin_plate_spline`→your existing TPS adapter, `group`/`view`→source-tree nodes.
You get a portable, MoBiE-interoperable, transform-carrying import format for very
little code, and it directly exercises the display + transform + grouping plumbing
you'll need anyway. This is the highest-leverage item.

**(b) Keep synthesising SpimData internally** (the gap-analysis "synthetic
SpimData" option). Read a collection *into* a lightweight SpimData so your entity
tree / export / adapters keep working unchanged. The collection is the *wire
format*; SpimData stays the *in-memory model*.

**(c) Shape your session/export format like a collection, and reserve a
`bdvplayground:` prefix now.** When you write your session JSON (gap 3) and
eventually an OME-Zarr export (gap 5), model it as *a list of member nodes with
per-member attributes* rather than a bespoke structure. Put entities under a
prefixed key. If RFC-8 stabilises, migration is a rename, not a redesign; if it
doesn't, you've lost nothing — you have a clean sidecar either way.

What **not** to do: don't wait for RFC-8, don't adopt the MoBiE table as your
authoritative format, and don't try to cram typed entities into unprefixed NGFF
keys.

---

## 6. Links

- MoBiE collection table tutorial — <https://mobie.github.io/tutorials/mobie_collection_table.html>
- Column constants (verified `main`) — <https://github.com/mobie/mobie-viewer-fiji/blob/main/src/main/java/org/embl/mobie/lib/table/columns/CollectionTableConstants.java>
- Open command — <https://github.com/mobie/mobie-viewer-fiji/blob/main/src/main/java/org/embl/mobie/command/open/OpenCollectionTableCommand.java>
- Collection data setter — <https://github.com/mobie/mobie-viewer-fiji/blob/main/src/main/java/org/embl/mobie/lib/data/CollectionDataSetter.java>
- RFC-8 Collections — <https://ngff.openmicroscopy.org/rfc/8/index.html>
- RFC-5 Coordinate systems & transforms (what RFC-8 embeds) — <https://ngff.openmicroscopy.org/rfc/5/index.html>
- The affine-in-NGFF gap that started all this — <https://github.com/ome/ngff/issues/94>
