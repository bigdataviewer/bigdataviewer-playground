# Draft forum post (image.sc)

**Title:** OME-Zarr and the SpimData metadata model — where did the ViewSetup/Entity layer go?

**Tags:** ome-ngff, bigdataviewer, n5, bigstitcher, bdv-playground

---

Hi all,

I maintain [BigDataViewer-Playground](https://github.com/bigdataviewer/bigdataviewer-playground),
which manages `SourceAndConverter`s and their display across multiple BDV windows
inside Fiji. I've just been through the exercise of adding OME-Zarr opening, and
I'd like to check my understanding before I build anything on top of it — because
I suspect some of this glue already exists and I'd rather use it than reinvent it.

## Opening works, and it was easier than expected

For the record, since I couldn't find this written down in one place, this is what
I ended up with, and it works on remote OME-NGFF v0.4 (Zarr v2) and v0.5 (Zarr v3)
without adding a single dependency — the whole stack comes in transitively via
`bigwarp_fiji` -> `bigdataviewer_fiji` under pom-scijava 45:

```java
final N5Reader n5 = new N5Factory().openReader(url);

final N5DatasetDiscoverer discoverer = new N5DatasetDiscoverer(n5,
        Executors.newCachedThreadPool(),
        Arrays.asList(N5ViewerCreator.n5vParsers),
        Arrays.asList(N5ViewerCreator.n5vGroupParsers));
final N5TreeNode root = discoverer.discoverAndParseRecursive("");

// take the multiscale *group* node, not its scale-level children
final List<N5Metadata> selected = selectViewableMetadata(root);

N5Viewer.buildN5Sources(n5, new DataSelection(n5, selected),
        new SharedQueue(8), converterSetups, sources, BdvOptions.options());
```

That yields proper multiresolution sources (`MetadataMipmapSource` inside a
`TransformedSource`), correctly split per channel. No XML, no SpimData, no
`XmlIoBasicImgLoader` anywhere. I understand the enabling piece on the
bigdataviewer-core side is the `N5Properties` generalisation of `N5ImageLoader` —
is that the right reading?

(One thing that tripped me up, in case it helps someone else: if you feed
`buildN5Sources` both the multiscale group metadata *and* the metadata of each
scale-level child, you get one flat single-resolution source per pyramid level in
addition to the correct pyramid. Selecting only nodes where
`node.getMetadata() != null && !node.isDataset()`, without descending further,
fixes it.)

## What I lost, and this is the actual question

The XML/SpimData layer was never just a pointer to pixels — it carried the sample
description, and a lot of BDV-Playground is built on that. Concretely, what I used
to get from a `SpimData` and no longer get:

- **`ViewSetup` attributes as typed `Entity`s** — `Channel`, `Tile` (with its
  location), `Angle`, `Illumination`. My source tree groups and filters sources by
  these; users select "all sources of tile 3" and act on them.
- **User-defined `Entity` subclasses.** BDV-Playground has an `EntityHandler` SPI
  precisely so downstream projects can define their own entity types and have them
  load, display and get written back on save. It's an extension point other people
  build on.
- **`ViewRegistration`s** — per-view transform *chains*, where each step keeps its
  own identity (calibration, then translation, then stitching, then registration).
- **Timepoints as a first-class dimension**, with per-timepoint registrations, plus
  `MissingViews`.

On the OME-NGFF side, as far as I can tell from n5-universe 3.0.2,
`OmeNgffMultiScaleMetadata` exposes exactly `version`, `name`, `type`, `axes`,
`datasets`, `coordinateTransformations` and the downsampling `metadata` — and there
is no class anywhere under `metadata/ome/ngff/` modelling channels-as-entities,
tiles, angles or illuminations. Which is entirely reasonable: NGFF describes how
arrays are packaged, not how the sample was acquired. But it does mean my metadata
doesn't have an obvious home.

Notably, `omero` (channel `label` / `color` / `window`) also doesn't appear to be
parsed, even though most IDR datasets carry it — so I currently can't even recover
channel *names*, and I'm auto-guessing contrast. Is that deliberate, deprecated-in-
0.5 and therefore not worth wiring up, or just not done yet?

## The questions

You all designed the XML/SpimData model in the first place, and BigStitcher leans
on it harder than anyone, so I assume this has come up:

1. **Where is acquisition metadata supposed to live now?** Is the intended answer
   the `bioformats2raw.layout=1` container with its `OME/METADATA.ome.xml` sidecar
   (in which case tile/channel metadata is really an OME-XML question, not an NGFF
   one)? Custom attributes in `.zattrs` / `zarr.json`? Or is it simply understood
   that NGFF doesn't cover this and each application keeps its own sidecar?

2. **Is there already a SpimData <-> OME-Zarr bridge I should be using?** My
   instinct is to synthesise a lightweight `SpimData` in front of the Zarr sources
   so that my existing entity/tree/export machinery keeps working unchanged — but
   that's exactly the kind of thing that has probably already been written properly
   for BigStitcher, and I'd rather depend on it than maintain a parallel version.
   What does BigStitcher do today when it works on OME-Zarr — does it still carry a
   SpimData XML alongside the container?

3. **Custom entities.** For a downstream tool that defines its own entity types,
   is there a recommended place to persist them in a Zarr container, or a
   convention emerging? I'd like to not invent a private namespace if one already
   exists.

4. **NGFF 0.5 transforms.** I see `v05/transformations/` in n5-universe ships
   affine, scale-offset, stacked, thin-plate-spline and a coordinate-system graph
   package — considerably more than the scale+translation of 0.4. Is that intended
   to become the persistence format for registration results (which would be
   great — I have TPS and transform-sequence support already and would happily
   write standards-compliant NGFF instead of my own JSON), or is it still
   experimental and ahead of the spec?

5. **Serialization by reference.** BDV-Playground saves sessions by referencing a
   source's SpimData plus its setup id. For Zarr I'd record
   `{container URI, multiscale path, channel index}`. Does something equivalent
   already exist, or is that mapping mine to define?

6. **Can I inject my own cache?** (Different in kind from the above, but it's the
   other thing blocking me.) BDV-Playground runs a process-wide, memory-bounded
   global cache so that many open datasets can't collectively blow the heap. On
   the SpimData path I install it by swapping the `backingCache` field of the
   `VolatileGlobalCellCache` held by the `ImgLoader` — ugly reflection, but there
   is a single, reachable cache per loader.

   On the Zarr path there's no `ImgLoader` and no `VolatileGlobalCellCache`; the
   cache is fixed at construction time inside each `CachedCellImg`, so there's
   nothing to swap afterwards. That's fine — arguably better — because
   `N5Utils` already has exactly the injection point I need:

   ```java
   open(N5Reader, String, Consumer<IterableInterval<T>>,
        LoaderCache<Long, Cell<A>>, Set<AccessFlags>, T)
   open(N5Reader, String, Consumer,
        Function<DataType, LoaderCache>, Set<AccessFlags>)
   ```

   and my global cache already implements `net.imglib2.cache.LoaderCache`.

   The gap is only that it isn't threaded through: `MetadataMipmapSource.getImgs`
   calls the two-argument `N5Utils.open(n5, path)`, which hardcodes the default
   unbounded `SoftRefLoaderCache`, and `buildN5Sources` offers no way to pass one
   in. So today my options are to fork `MetadataMipmapSource` or to give up on
   bounded memory for Zarr sources, neither of which is appealing.

   **Would you accept a PR adding an optional `Function<DataType, LoaderCache>`
   (defaulting to current behaviour) to `MetadataMipmapSource` / `MetadataSource`
   and through `buildN5Sources`?** It looks like a small change given n5-imglib2
   already accepts one, and I'd guess anyone embedding these sources in a larger
   application hits the same wall.

   Related, and much smaller: `buildN5Sources` *does* take the `SharedQueue` as a
   parameter, which is great — I'll pass my shared instance rather than letting
   each dataset spawn its own fetcher pool. Just flagging that the demo code I've
   seen around tends to do `new SharedQueue(n)` per call, which quietly
   multiplies thread pools in a multi-dataset application.

   (Side question on caching semantics: in NGFF, time is an axis *inside* one
   array, so a single `CachedCellImg` covers all timepoints — whereas SpimData
   gave me a cache per (setup, timepoint, level). Is there a recommended
   granularity for keying a shared cache across NGFF sources, or is per-level
   with the cell index disambiguating time the expected pattern?)

Happy to contribute back whatever comes out of this — a documented opening recipe
at minimum, and the entity bridge if it turns out nobody has written it yet.

Thanks!
Nicolas