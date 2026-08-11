# How the ecosystem handles metadata for OME-Zarr sources

Small survey, written 2026-07-25, to inform adapting BDV-Playground to Zarr. It
answers one question per metadata kind: **is it carried by the OME-NGFF spec, or
is each application inventing a local convention?** Java facts were checked
against the `main` branch of each repo with `gh`; Python against `ome-zarr-py`
`master`.

**Does the published 0.5 spec change any of this? Almost nothing.** I read
<https://ngff.openmicroscopy.org/specifications/0.5/index.html> directly. NGFF 0.5
is 0.4 **re-packaged onto Zarr v3** — same axes, same scale+translation-only
transforms, same transitional `omero`, same plate/well HCS, still no entities. The
*only* substantive deltas are mechanical (how/where the JSON lives) plus one
formalisation (`bioformats2raw.layout`). Details in the **"NGFF 0.5"** section
below. In particular, **beware the version-number trap**: n5-universe's `v05/`
package does **not** implement spec 0.5 — it implements **RFC-5**, an unrelated
future proposal that happens to share the digit. See §6 and the 0.5 section.

Repos looked at:

- **n5-universe** (Saalfeld lab, the Java NGFF metadata model BDV/n5-viewer use) —
  <https://github.com/saalfeldlab/n5-universe>
- **mobie-io** (MoBiE's IO layer, sits on n5-universe) —
  <https://github.com/mobie/mobie-io>
- **mobie-viewer-fiji** (the MoBiE application + its own project JSON model) —
  <https://github.com/mobie/mobie-viewer-fiji>
- **ome-zarr-py** (the OME reference Python reader/writer) —
  <https://github.com/ome/ome-zarr-py>
- **ome/ngff** (the spec itself + the RFC process) —
  <https://github.com/ome/ngff>, published at <https://ngff.openmicroscopy.org>
- **multiview-reconstruction** (the library under BigStitcher; its OME-Zarr
  importer + loader) — <https://github.com/PreibischLab/multiview-reconstruction>
- **multiview-stitcher** (BigStitcher-like stitcher in Python) —
  <https://github.com/multiview-stitcher/multiview-stitcher>

---

## TL;DR table

| Metadata | In the NGFF spec? | Java (n5-universe) reads it? | Python (ome-zarr-py) reads it? | MoBiE's answer |
|---|---|---|---|---|
| Spatial calibration (voxel size, offset) | **Yes** — `axes` + `coordinateTransformations` (scale/translation) | **Yes** | Yes | uses NGFF |
| Axis units (incl. **time unit**) | **Yes** — `axis.unit` (UDUNITS-2 subset) | **Yes** (`Axis.getUnit()`) | Yes | uses NGFF |
| **Timepoints** (t as a dimension) | **Yes** — `t` axis inside one array | **Yes** (t axis → `numTimepoints`) | Yes | uses NGFF |
| Channels **as a dimension** | **Yes** — `c` axis | **Yes** (splits 1 source/channel) | Yes | uses NGFF |
| **Channel name / color / contrast** | **Transitional** — `omero` block (0.4); *not normative* | **No** — not parsed at all | **Yes** — full `OMERO` parser | **own JSON** (`ImageDisplay`) |
| **HCS** plate / well | **Yes** — `plate` / `well` groups | **No** class for it | **Yes** — `Plate`/`Well` | **own** HCS grid views |
| Labels / segmentation | **Yes** — `labels/` + `image-label` | partial (path only) | **Yes** — `Labels`/`Label` | reads NGFF `labels/` |
| Tile / Angle / Illumination (acquisition entities) | **No** — not modelled anywhere | n/a | n/a | not entities; grid *transforms* + views |
| Rich transforms (affine, TPS, sequences) for registration | **Not in 0.4 or 0.5** — proposed in **RFC-5** | **Yes** — `v05/transformations/*` (this is **RFC-5**, *not* spec 0.5) | no | own transform JSON in views |

The short story: **NGFF is strong on "how the pixels are packaged" (axes, units,
scale/translation, timepoints, HCS layout) and weak-to-absent on "how the sample
was acquired and should be displayed" (channel identity, tiles/angles, display
settings, registration).** Everything in the last group is either a *transitional*
part of the spec (omero), a *future RFC*, or a per-application convention.

---

## 1. Spatial calibration, units, time, channel-as-axis — **spec-based, and Java reads it**

This is the part that "just works" and is genuinely standardised.

NGFF `axes` give each dimension a `type` (`space`/`time`/`channel`) and an optional
`unit`, and `coordinateTransformations` give a per-scale-level `scale` +
`translation`. That is the entire spatial+temporal calibration model of 0.4/0.5.

- n5-universe models an axis as
  [`metadata/axes/Axis.java`](https://github.com/saalfeldlab/n5-universe/blob/main/src/main/java/org/janelia/saalfeldlab/n5/universe/metadata/axes/Axis.java)
  — fields `type` (`space`/`channel`/`time`/`displacement`/`array`), `name`,
  `unit`, `discrete`. The javadoc literally points at
  `https://ngff.openmicroscopy.org/0.4/#axes-md`. So **time unit is spec-based** and
  read as `axis.getUnit()`.
- Per-level scale/translation live in
  [`ome/ngff/NgffSingleScaleAxesMetadata.java`](https://github.com/saalfeldlab/n5-universe/blob/main/src/main/java/org/janelia/saalfeldlab/n5/universe/metadata/ome/ngff/NgffSingleScaleAxesMetadata.java)
  and the multiscale container is
  [`OmeNgffMultiScaleMetadata.java`](https://github.com/saalfeldlab/n5-universe/blob/main/src/main/java/org/janelia/saalfeldlab/n5/universe/metadata/ome/ngff/OmeNgffMultiScaleMetadata.java).
- **Timepoints**: NGFF puts time *inside* one array as the `t` axis. On the BDV
  side, `N5Viewer.buildN5Sources(...)` returns the number of timepoints — you can
  see MoBiE consume exactly that return value in
  [`N5ImageData.open()`](https://github.com/mobie/mobie-io/blob/main/src/main/java/org/embl/mobie/io/imagedata/N5ImageData.java)
  (`numTimePoints = Math.max(numTimePoints, N5Viewer.buildN5Sources(...))`). There
  is **no per-timepoint registration** concept in NGFF — unlike SpimData, where
  each (setup, timepoint) has its own `ViewRegistration`.
- **Channel as a dimension** is spec-based (the `c` axis); `buildN5Sources` splits
  it into one `SourceAndConverter` per channel. But the channel's *identity* (its
  name) is **not** carried here — see §2.

Python reads the same axes/transformations in
[`ome_zarr/axes.py`](https://github.com/ome/ome-zarr-py/blob/master/ome_zarr/axes.py)
and the `Multiscales` spec class in
[`ome_zarr/reader.py`](https://github.com/ome/ome-zarr-py/blob/master/ome_zarr/reader.py).

## 2. Channel name / color / contrast — **spec is "transitional", Java skips it, Python reads it, MoBiE reinvents it**

This is the sharpest divergence and the one most relevant to you.

**In the spec:** channel display metadata lives in the `omero` block
(`channels[].label`, `.color`, `.window.{start,end,min,max}`, `.active`,
`rdefs.model`). In NGFF 0.4 this is explicitly a **transitional** section — carried
over from OMERO, *not* normative, and de-emphasised going forward
(<https://ngff.openmicroscopy.org/0.4/#omero-md>). So channel *names* are only
"semi-spec".

**Python — reads it fully.** `ome-zarr-py` has a dedicated `OMERO` spec class that
parses names, colors, contrast windows, visibility and greyscale model:
[reader.py, `class OMERO`](https://github.com/ome/ome-zarr-py/blob/master/ome_zarr/reader.py)
(populates `node.metadata["channel_names"]`, `["colormap"]`,
`["contrast_limits"]`, `["visible"]`). This is why napari shows real channel names
from IDR data and BDV does not.

**Java (n5-universe) — does not parse `omero` at all.** There is no `omero`
metadata class anywhere in the tree; channels survive only as a `_ch0/_ch1` name
suffix invented by `buildN5Sources`. Confirmed by listing
`metadata/ome/**` — the only classes are multiscale/axes/transformations.

**MoBiE — does not read `omero` either; it stores display metadata in its own
project JSON.** `mobie-io`'s
[`N5ImageData.getMetadata()`](https://github.com/mobie/mobie-io/blob/main/src/main/java/org/embl/mobie/io/imagedata/N5ImageData.java)
returns a `CanonicalDatasetMetadata` whose color/contrast come from the default
`ConverterSetup`, **not** from the file. The *real* channel display settings live
one layer up, in MoBiE's own `dataset.json` "views", as
[`ImageDisplay`](https://github.com/mobie/mobie-viewer-fiji/blob/main/src/main/java/org/embl/mobie/lib/serialize/display/ImageDisplay.java)
— fields `color`, `double[] contrastLimits`, `opacity`, blending, even
`boolean invert; // TODO add to spec`. That "TODO add to spec" comment tells you
everything: **MoBiE maintains its own display spec and layers it over NGFF pixels.**
This whole serialization package
([`lib/serialize/*`](https://github.com/mobie/mobie-viewer-fiji/tree/main/src/main/java/org/embl/mobie/lib/serialize))
— `Dataset`, `View`, `Display`, `*DataSource` — is MoBiE convention, documented at
<https://mobie.github.io/specs/mobie.html>, not NGFF.

**Future spec:** channel identity/provenance is being reworked in
**RFC-7 "Channel provenance"** (<https://github.com/ome/ngff/tree/main/rfc/7>). If
you want a forward-looking channel-name story rather than parsing transitional
`omero`, that's the thread to watch.

**For you:** the cheap win noted in your gap doc (parse `omero` off the group
attributes yourself and drive `ColorChanger`/`BrightnessAdjuster`) is exactly what
Python already does and what neither Java library does — so you would not be
duplicating existing Java code, you'd be filling a real hole.

## 3. HCS (plate / well) — **spec-based, Python reads it, Java n5-universe doesn't, MoBiE has its own grid path**

- **Spec:** `plate` and `well` group metadata are normative NGFF
  (<https://ngff.openmicroscopy.org/0.4/#plate-md>).
- **Python:** `ome-zarr-py` has `Plate` and `Well` spec classes in
  [reader.py](https://github.com/ome/ome-zarr-py/blob/master/ome_zarr/reader.py)
  (lazy well loading, plate label images).
- **Java n5-universe:** no plate/well/HCS metadata class exists (checked the tree).
- **MoBiE:** handles HCS in the *application*, not the IO layer, via
  [`HCSDataSetter`](https://github.com/mobie/mobie-viewer-fiji/blob/main/src/main/java/org/embl/mobie/lib/hcs/HCSDataSetter.java)
  and grid transformations — i.e. it turns a plate into MoBiE grid views. Again a
  convention on top, not a read of NGFF plate metadata into typed objects.
- **Future:** **RFC-8 "Collections"**
  (<https://github.com/ome/ngff/tree/main/rfc/8>) generalises multi-image
  containers.

## 4. Labels / segmentation — **spec-based, both Java-ish and Python read it**

NGFF `labels/` + `image-label` is normative. `ome-zarr-py` reads it (`Labels` /
`Label` classes, including `image-label.colors`/`properties`). `mobie-io` reads the
`labels/` list with a tiny local POJO
([`ngff/Labels.java`](https://github.com/mobie/mobie-io/blob/main/src/main/java/org/embl/mobie/io/ngff/Labels.java))
by parsing `labels/.zattrs`, then opens each as a source — see the labels branch in
`N5ImageData.open()`.

## 5. Acquisition entities (Tile / Angle / Illumination) — **nobody models these in NGFF**

This is the SpimData/BigStitcher concept your `EntityHandler` SPI is built on, and
it has **no home in NGFF at all** — not in the spec, not as a convention in
n5-universe, ome-zarr-py, or MoBiE. NGFF describes array packaging; it does not say
"this source is tile 3, angle 1, illumination 0."

How the others cope:

- **MoBiE** doesn't reconstruct typed entities. It expresses tiling/positioning as
  *grid transformations* and groups sources into *views*
  ([`serialize/transformation/*`](https://github.com/mobie/mobie-viewer-fiji/tree/main/src/main/java/org/embl/mobie/lib/serialize/transformation),
  `GridTransformation`, `AffineTransformation`, …) recorded in its own JSON.
- **BigStitcher** keeps a **SpimData XML alongside** the container; the OME-Zarr is
  just the pixel backend. The `ViewSetup`/entity model stays in the XML.

So the honest options for BDV-Playground are the two you already identified:
synthesise a lightweight `SpimData` in front of Zarr sources (keeps your entity/
tree/export machinery), or read entities from a `bioformats2raw` `OME/METADATA.ome.xml`
sidecar where present. NGFF core will not give you these.

## 6. Rich transforms (registration results) — **not in 0.4 OR 0.5, but RFC-5 is being implemented in the very library you already depend on**

**Version-number trap, spelled out:** the published **spec 0.5** allows only
`scale` + `translation` (verified — *"The transformation MUST only be of type
`translation` or `scale`"*). The n5-universe package literally named **`v05`** is
**not** an implementation of spec 0.5's transforms — it is an implementation of
**RFC-5**, a separate, still-in-review proposal. The digit collision is
coincidental. So "0.5 transforms" can mean two different things, and only the
`scale`+`translation` one is actually a released standard.

- 0.4 **and** 0.5 **core** allow only `scale` + `translation`
  ([`coordinateTransformations/*`](https://github.com/saalfeldlab/n5-universe/tree/main/src/main/java/org/janelia/saalfeldlab/n5/universe/metadata/ome/ngff/coordinateTransformations)).
- The richer set (affine, scale-offset, sequence/stacked, thin-plate-spline,
  displacement field, coordinate-system graph) is **RFC-5 "Coordinate Systems and
  Transformations"** (<https://github.com/ome/ngff/tree/main/rfc/5>), currently in
  state **S4 (update implementations)**, authored by John Bogovic.
- n5-universe already ships that implementation under
  [`ome/ngff/v05/transformations/`](https://github.com/saalfeldlab/n5-universe/tree/main/src/main/java/org/janelia/saalfeldlab/n5/universe/metadata/ome/ngff/v05/transformations)
  (`AffineCoordinateTransform`, `ThinPlateSplineCoordinateTransform`,
  `SequenceCoordinateTransform`, `RealTransformCoordinateTransform`, …) plus a
  [`v05/graph/`](https://github.com/saalfeldlab/n5-universe/tree/main/src/main/java/org/janelia/saalfeldlab/n5/universe/metadata/ome/ngff/v05/graph)
  coordinate-system package — so RFC-5 and the jar you already resolve are the same
  code line.

Because your project already has TPS and transform-sequence adapters
(`net.imglib2.realtransform.*`), RFC-5 is the natural standards-compliant target
for persisting registration results — mapping Playground `RealTransform`s ↔ these
`CoordinateTransform` classes — instead of your own JSON. It is ahead of the
frozen 0.5 spec but actively implemented, not speculative.

---

## 7. BigStitcher — opens 0.4 **and** 0.5, but keeps SpimData/XML as the metadata backbone

This is the most directly relevant precedent for you, because BigStitcher leans on
the entity model harder than anyone and has already had to answer "what do I do
with OME-Zarr?". The answer, from the code: **it synthesises a full `SpimData2` at
import time and persists the good old XML.** The Zarr is only the pixel backend
plus calibration. It does *not* move metadata into the Zarr.

The import lives in multiview-reconstruction (the library under BigStitcher):
[`fiji/datasetmanager/OMEZARR.java`](https://github.com/PreibischLab/multiview-reconstruction/blob/master/src/main/java/net/preibisch/mvrecon/fiji/datasetmanager/OMEZARR.java).

**Version support — both.** The importer offers a literal choice
`{ "OME-ZARR v2", "OME-ZARR v3" }` and branches on
`StorageFormat.ZARR2` vs `StorageFormat.ZARR`, reading `ome/multiscales` for the
v3 case (*"Zarrv3 detected, trying 'ome/multiscales'"*). So **0.4 (Zarr v2) and 0.5
(Zarr v3) both open**, via the same n5-universe `OmeNgffMultiScaleMetadata` you use.
There's even an in-code note about a Zarr-v3/NGFF-0.5 deserialization quirk
(`multiscales[0].getPaths()` returns null under n5-universe 3.0.0, so it reads
`datasets[].path` instead) — i.e. this path is actively maintained against 0.5.

**How it maps NGFF → the entity model — the interesting part:**

- **Spatial calibration**: read straight from NGFF — `scale`, `translation`, and
  `unit` (taken from the `x` axis). Becomes `VoxelDimensions` + a calibration
  `ViewRegistration`.
- **Channel and Timepoint**: read from the **array axes**. It inspects the
  dimensionality: 3D ⇒ 1 timepoint / 1 channel, 4D ⇒ dimension 3 is `C`
  (`sizeC = dims[3]`), 5D ⇒ dimension 4 is also `T` (`sizeT = dims[4]`). These
  become `Channel` and `TimePoint` entities. So channels/timepoints come from NGFF,
  as in §1.
- **Tile / Angle / Illumination** — the entities NGFF has **no field for** — are
  recovered from the **file/URI layout**, not the Zarr metadata. `OMEZARR.java`
  runs the classic BigStitcher **filename-pattern detector**
  (`NumericalFilenamePatternDetector`) over a wildcard list of Zarr containers, and
  the user assigns each numeric pattern to `Tile` / `Angle` / `Illumination` /
  `Channel` / `TimePoint` in the dialog. Absent an assignment, each defaults to a
  single `Tile(0)` / `Angle(0)` / `Illumination(0)`. This is the concrete proof of
  §5: **because NGFF carries no tile/angle/illumination, BigStitcher reconstructs
  them from container naming + user input at import**, then bakes them into entities.
- The result is assembled into a `SpimData2` — `ViewSetup`s (Channel, Angle,
  Illumination, Tile), `TimePoints`, `ViewRegistrations`, `MissingViews`,
  `BoundingBoxes`, interest points, PSFs, stitching results — the whole apparatus.

**Persistence — still XML.** The pixel loader is
[`AllenOMEZarrLoader`](https://github.com/PreibischLab/multiview-reconstruction/blob/master/src/main/java/net/preibisch/mvrecon/fiji/spimdata/imgloaders/AllenOMEZarrLoader.java)
(an `ImgLoader` reading the NGFF arrays through n5-universe / a `N5Properties`-style
backend, cf.
[`AllenOMEZarrProperties`](https://github.com/PreibischLab/multiview-reconstruction/blob/master/src/main/java/net/preibisch/mvrecon/fiji/spimdata/imgloaders/AllenOMEZarrProperties.java)),
serialised by
[`XmlIoAllenOMEZarrLoader`](https://github.com/PreibischLab/multiview-reconstruction/blob/master/src/main/java/net/preibisch/mvrecon/fiji/spimdata/imgloaders/XmlIoAllenOMEZarrLoader.java).
So a BigStitcher OME-Zarr project **is a `dataset.xml`** whose `<ImageLoader>`
points at OME-Zarr, exactly the way a `bdv.n5`/HDF5 project points at its
container. The registration results BigStitcher computes are written back into the
**XML `ViewRegistration`s**, not into NGFF transforms. (The "Allen" name reflects
that the loader was built around the Allen Institute's OME-Zarr exports, where each
tile is a separate array with its own transform.)

**Migration planned?** Nothing in the code or issues suggests BigStitcher intends
to make the Zarr the metadata backbone. What's evolving is *breadth of reading*
(0.5 / Zarr-v3 support was added; RFC-5 transforms exist in the stack) — not
relocating the SpimData model into the container. The XML stays. This matches
BigStitcher-Spark, whose OME-Zarr support is about **fusion output** (writing pixels
to NGFF), still driven by a SpimData XML on the input side.

### Can you reuse `AllenOMEZarrLoader`? — the mechanics

I traced the loader and its `N5Properties`. Verdict: **the *pattern* is exactly
what you want and is the cleanest known template; reusing the *class* is possible
but drags in a heavy GPL-2.0 dependency.**

How it's built:

- `AllenOMEZarrLoader` **extends `bdv.img.n5.N5ImageLoader`** — so it *is* a real
  BDV `ViewerSetupImgLoader`, and you get multiresolution + volatile access +
  `VolatileGlobalCellCache` caching from the base class for free. (Note for your
  cache question: this path *does* have a `VolatileGlobalCellCache`, unlike the raw
  `N5Viewer.buildN5Sources` path — because it goes through `N5ImageLoader`.)
- The OME-Zarr specifics are isolated in one small class,
  [`AllenOMEZarrProperties implements bdv.img.n5.N5Properties`](https://github.com/PreibischLab/multiview-reconstruction/blob/master/src/main/java/net/preibisch/mvrecon/fiji/spimdata/imgloaders/AllenOMEZarrProperties.java)
  (`createN5PropertiesInstance()` returns it). This is precisely the
  `N5Properties` generalisation you identified in your forum draft. It reads the
  NGFF multiscale metadata (`OmeNgffMultiScaleMetadata`, `scale`/`translation`
  `coordinateTransformations`) to produce `getMipmapResolutions` / `getDataType` /
  `getDimensions`, and caches parsed metadata per zarr path.
- **The key glue is the `Map<ViewId, OMEZARREntry>`.** `OMEZARREntry` is
  `{ String path, int[] higherDimensionIndicies }` — for each SpimData `ViewId`
  (timepoint, setup) it records which zarr array and which **hyperslice** to take.
  `extract3DVolume()` then `Views.hyperSlice`s the channel/time axes out of the
  5D array to yield the 3D volume for that view. **This is the concrete answer to
  the caching-granularity question in your forum post**: NGFF keeps c/t as axes
  inside one `CachedCellImg`, and BigStitcher bridges to SpimData's per-(setup,
  timepoint) model by slicing at `prepareCachedImage` time, *after* the cache — one
  cached array per zarr path, hypersliced per view. And `OMEZARREntry` is almost
  exactly your proposed serialization key `{container URI, multiscale path, channel
  index}` (gap doc §3).

Coupling / reuse assessment:

- It is **tightly bound to SpimData**: the constructor needs an
  `AbstractSequenceDescription` and the pre-built `ViewId→OMEZARREntry` map. It is
  **not** a standalone "URI → `Source`" — you must already have the entity model.
  That's fine for the synthetic-SpimData route (you're building one anyway), and it
  does **not** require an XML on disk — `main()` loads XML only as a demo; the
  loader works from an in-memory `SequenceDescription`.
- It lives in **multiview-reconstruction (GPL-2.0)** under `net.preibisch.mvrecon`,
  which pulls in the whole BigStitcher stack. So *depending* on it is a big, viral
  dependency; *replicating* the ~2 small classes (`N5Properties` impl + entry map)
  is trivial and keeps you dependency-light. Reuse the class only if you also want
  to **read BigStitcher's OME-Zarr XML projects** directly.
- It does **not** read `omero` — same channel-display gap as everywhere else (§2).

**Bottom line for BDV-Playground:** your "synthesise a lightweight `SpimData` in
front of the Zarr sources" option (gap doc §1, higher-leverage path) is **not
hypothetical — BigStitcher already implements exactly it**, including the
NGFF-axes → Channel/Timepoint mapping, the container-naming → Tile/Angle/Illum
mapping, and the `N5ImageLoader` + `N5Properties` + `ViewId→(path,slice)` template
that even solves your cache and time-axis questions. Mirror that pattern (small,
license-clean) rather than depending on the jar, unless BigStitcher interop is a
goal.

## 8. multiview-stitcher (Python) — a BigStitcher-in-Python, and it *does* handle timelapse

<https://github.com/multiview-stitcher/multiview-stitcher> is the closest Python
analogue to BigStitcher, and it independently reaches the **same** conclusion about
NGFF and registration metadata — which is a useful third data point.

**Capabilities (checked against `docs/features.md`):** 2D + 3D; pairwise
registration (phase correlation, ANTsPy, ITKElastix); global parameter resolution
over a tile-overlap graph; fusion (weighted average, MIP, multi-view deconvolution,
GPU via cupy); OME-Zarr / `multiscale-spatial-image` / CZI (multi-positioning) /
TIFF IO; napari plugin. Not a light-sheet multi-*angle* tool the way BigStitcher is
(bead alignment and rotation are unchecked roadmap items) — it's primarily a
**stitcher** (translation/affine of tiles), with multi-view deconvolution.

**Timelapse — yes, and better than NGFF.** You were right to check, but it does
handle it. The data model is a `MultiscaleSpatialImage` (xarray `DataTree`) with
dims **`(t, c, z, y, x)`** — `t` is first-class. Registration has an explicit
[`register_pair_of_msims_over_time`](https://github.com/multiview-stitcher/multiview-stitcher/blob/main/src/multiview_stitcher/registration.py)
that *"Apply[s] register_pair_of_msims to each time point"* — it iterates
`sim.coords["t"].values`, registers per timepoint, and concatenates the results
along `dim="t"`. So transforms are **time-resolved (per-timepoint)** — the affine
parameter array itself carries a `t` axis (`affine_metadata (t, x_in, x_out)`).
That is exactly SpimData's per-timepoint `ViewRegistration` capability, and *more*
than NGFF, which has no per-timepoint transform concept at all. Channels: you pick
one `reg_channel` (by name or index) to drive registration; channel names are
carried (`c_coords=['DAPI','GFP']`).

**How it deals with metadata — its own transform layer on top of NGFF, because
NGFF has no affine.** This is the punchline and it matches BigStitcher/MoBiE:

- Each image carries **named coordinate systems** via a `transform_key` (e.g.
  `"stage_metadata"` → `"translation_registered"`), stored as extra xarray *data
  variables* (`affine_*` of shape `(t, x_in, x_out)`) attached to every scale. This
  is a multiview-stitcher **convention on top of `multiscale-spatial-image`**, not
  NGFF. (It's conceptually the same idea as NGFF RFC-5 named coordinate
  systems, and as SpimData's registration, just expressed in xarray.)
- Their docs state the gap explicitly (`docs/objects.md`, `docs/data_formats.md`):
  *"modified instances of MultiscaleImage as used by multiview-stitcher cannot
  (yet) be serialized to and from NGFF … as the support for affine transforms is
  missing"*, citing [ome/ngff#94](https://github.com/ome/ngff/issues/94). So the
  registration result **cannot round-trip through OME-Zarr** — on NGFF write
  (`ngff_utils.py`, via `ngff-zarr`) it emits only `scale`+`translation` axes/
  transforms; the affine is either applied by resampling during **fusion** or kept
  in the sidecar MSI, never in the NGFF transform.

**Two things it does that the Java stack doesn't**, worth noting for your own writer
plans:

- It **reads and writes the `omero` block** (`ngff_utils.VirtualOMEZarr` takes an
  `omero=` and writes `zattrs["omero"]`) — so channel display *does* survive on the
  Python side, reinforcing §2's point that this is a Java-side omission, not a spec
  problem.
- It has **HCS plate/well** read/write (`_is_hcs_plate_tree`, a plate-tree virtual
  zarr), consistent with §3/§6 that Python handles HCS and Java n5-universe doesn't.

**Net:** multiview-stitcher confirms the whole picture from a third, independent,
Python codebase — NGFF carries pixels + axes + scale/translation + timepoints-as-
axis + (transitional) omero + HCS, and **every registration/affine result lives in
the application's own layer** (multiview-stitcher's `transform_key` xarray vars,
BigStitcher's XML `ViewRegistration`s, MoBiE's JSON transforms) because NGFF core
still has no affine. For you, if you ever want Python interop, its transform model
is coordinate-system-keyed affines per timepoint — close to both SpimData and
RFC-5, and a reasonable shape to keep in mind for your own transform serialization.

## NGFF 0.5 — what actually changed, and what it does *not* change

Read from the published spec: <https://ngff.openmicroscopy.org/specifications/0.5/index.html>.
0.5 is **0.4 ported to Zarr v3**, standardised via **RFC-2 "Zarr v3"**
(<https://github.com/ome/ngff/tree/main/rfc/2>). The changes are almost entirely
about *packaging*, not *what metadata exists*.

**Genuinely new in 0.5 (affects a reader, not the metadata model):**

1. **Zarr v3.** *"OME-Zarr is implemented using … version 3 of the Zarr
   specification."* Group attributes now live in **`zarr.json`**, not `.zattrs`.
2. **`ome` namespace.** All OME metadata is nested under a single top-level
   **`ome`** key in `attributes` (in 0.4 the blocks — `multiscales`, `omero`,
   `plate`, … — sat at the root of `.zattrs`). Any hand-rolled attribute reader
   (e.g. your planned `omero` parser) must look under `attributes.ome.omero` for
   0.5 vs. root `omero` for 0.4.
3. **`bioformats2raw.layout` is now written into the spec** (as *transitional*):
   a group with `bioformats2raw.layout: 3`, an optional `OME/METADATA.ome.xml`
   sidecar **stored inside the Zarr** (must use `<MetadataOnly/>`), and an optional
   `OME/` group with a `series` attribute listing image paths. If the container is a
   plate, `plate` **takes precedence**. This is the closest thing to a *standardised*
   home for tile/channel/acquisition detail — but it's OME-XML, not NGFF, and only
   transitional.

**Explicitly *unchanged* in 0.5 (so every conclusion above still holds):**

- **Transforms:** *"MUST only be of type `translation` or `scale`."* No affine/TPS/
  sequences. (RFC-5 ≠ 0.5, see §6.)
- **Channels:** still only the **transitional `omero`** block — *"[Transitional]
  information specific to the channels of an image and how to render it can be found
  under the 'omero' key."* No normative channel model anywhere else. Java still
  doesn't parse it; Python still does.
- **Axes:** same `name`/`type`(space/time/channel)/`unit`(UDUNITS-2) structure,
  2–5 dims, `time` first then `channel`.
- **HCS:** `plate`/`well` still normative and unchanged in substance.
- **Entities (tile/angle/illumination):** still **absent** — 0.5 adds nothing here
  (the only "acquisition" is the plate-level `acquisitions` array in HCS).

**So for you, 0.5 changes exactly two practical things:** (a) read `zarr.json` and
descend into the `ome` key; (b) `bioformats2raw.layout` + `OME/METADATA.ome.xml` is
now a spec-blessed (if transitional) place to look for tiles/channels via OME-XML —
which strengthens the "read the OME-XML sidecar" option for your entity problem in
§5. It does **not** move channel display, registration transforms, or acquisition
entities out of "convention / transitional / RFC" territory.

## Is mobie-io 0.5-compatible?

Yes, to the same degree n5-universe is. n5-universe has `v03`/`v05` processing
packages and n5-zarr reads both Zarr v2 and v3, so the path you validated on 0.4
**and** 0.5 is the same one MoBiE uses (`N5ImageData` → `N5Viewer.buildN5Sources`).
That covers pixels, axes, calibration, timepoints, labels — **but not** channel
display, HCS-as-typed-metadata, or entities, which MoBiE pushes into its own JSON
regardless of NGFF version.

## Bottom line for adapting BDV-Playground

1. **Spatial calibration, units, time, timepoints, channel-as-axis, labels** — take
   them straight from NGFF via n5-universe. Spec-based, already read.
2. **Channel names/colors/contrast** — spec-transitional (`omero`); no Java library
   reads it. Parsing it yourself (as Python does) is a genuine, non-duplicative win.
   Watch RFC-7 for the durable version.
3. **HCS** — spec-based but not surfaced by n5-universe; you'd read `plate`/`well`
   yourself (Python's `Plate`/`Well` is the reference) or adopt RFC-8 Collections.
4. **Tiles/angles/illuminations/custom entities** — **not an NGFF concept.** Either
   synthesise a SpimData shim (keeps your `EntityHandler`/tree/export intact) or read
   a `bioformats2raw` OME-XML sidecar. This is the architectural fork in your gap doc,
   and the survey confirms there is no standard to lean on — MoBiE and BigStitcher
   both keep their own side model.
5. **Registration transforms** — persist as RFC-5 `v05/transformations` (same code
   line you already depend on) rather than private JSON.

Every "convention, not spec" cell above is a place where you get to choose your own
representation — just note that MoBiE (its `dataset.json`) and BigStitcher (its
SpimData XML) have already each chosen a different one, so there is no single
Java convention to conform to.
