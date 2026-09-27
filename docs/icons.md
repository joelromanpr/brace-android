# Icons and icon loading

`brace-icons` provides 11 original Brace drawings, themed icon buttons, and a registry for your own Compose vectors. Add it when an app needs icons without a large artwork pack. Two optional [licensed icon packs](#optional-legacy-blueprint-glyph-pack) offer more drawings. All three artifacts are available in the `1.0.0` release; their [component status](coverage.md) remains in progress.

## Install and use

Add the icon artifact from Maven Central:

```kotlin
implementation("io.github.joelromanpr.brace:brace-icons:1.0.0")
```

```kotlin
BraceTheme {
    Row {
        BraceIcon(BraceIcons.Info, contentDescription = null, intent = BraceIconIntent.Primary)
        Text("Import details") // supplies the meaning; the icon is decorative
    }
    BraceIconButton(
        name = BraceIcons.Search,
        label = "Search records",
        onClick = { openSearch() },
    )
}
```

`BraceIconSize.Small`, `Medium`, and `Large` use Brace sizing tokens (16, 20, and 24dp). `customSize` accepts a positive dp size. `BraceIconIntent` uses semantic default, primary, success, warning, or danger colors; `tint` can override that color. Theme, brand, and contrast switches update the rendered tint at runtime. `ChevronForward` mirrors in RTL. Each bundled icon uses one original drawing scaled to the requested size.

Pass `contentDescription = null` when adjacent text already explains the image. A standalone informative icon needs a localized nonblank description. An actionable glyph should use `BraceIconButton`, which provides a named button role, enabled state, keyboard and pointer activation, visible focus, and a 48dp minimum target even when the drawing is 16dp. Static vectors have no animation to suppress for reduced-motion users. At large text scales, keep a readable adjacent label or use the icon button's spoken label; the glyph itself does not replace text.

## Scoped registration and runtime lookup

`BraceIconRegistry.Default` contains the 11 bundled names: `add`, `check`, `chevron-forward`, `close`, `edit`, `help`, `info`, `more`, `remove`, `search`, and `warning`. `BraceIconName` validates lowercase kebab-case keys. `register` returns a new immutable registry, so an app can add or override vectors without changing other screens. Supply any Compose `ImageVector` created by your app or a separately licensed source:

```kotlin
val customRegistry = remember(customVector) {
    BraceIconRegistry.Default.register(BraceIconName("workspace-mark"), customVector)
}
BraceIconRegistryProvider(customRegistry) {
    BraceIconByName(
        name = iconNameFromData,
        contentDescription = "Record status",
        fallback = BraceIcons.Help,
    )
}
```

`find` returns null for an unknown key; `resolve` and `BraceIconByName` use the requested fallback, then the bundled Help drawing if needed. Runtime lookup is an in-memory map, with no disk or network work during composition. Apps can pass a registry directly to an icon instead of using the provider. Keep `contentDescription` accurate for the resolved icon when names come from external data.

## Optional legacy Blueprint glyph pack

`brace-blueprint-icons` is an **opt-in**, aligned-version artifact. It does not replace `brace-icons` or change `BraceIconRegistry.Default`. It imports the 706 legacy names in pinned `@blueprintjs/icons` 6.13.0, with distinct 16px and 20px SVG paths. Add this artifact when the complete pinned legacy artwork is needed:

```kotlin
implementation("io.github.joelromanpr.brace:brace-blueprint-icons:1.0.0")
```

Load the 844 KB packaged JSON asset once on a background dispatcher, then retain the immutable pack across screens. Loading performs one local read and JSON parse; `find` lazily parses each requested path into a cached Compose `ImageVector`. Rendering after load does no file or network I/O. The 16px and 20px choices are artwork resolution, independent of the Brace theme's display size. Small defaults to the 16px path; medium and large default to the 20px path. The 20px `third-party` drawing retains its original 20×18 viewBox, and `blank` intentionally draws nothing.

```kotlin
// In a ViewModel or application initializer on Dispatchers.IO:
val pack = withContext(Dispatchers.IO) { BraceBlueprintIconPack.load(context) }

BraceTheme {
    BraceBlueprintIcon(pack, BraceBlueprintIconNames.Search, contentDescription = null,
        intent = BraceIconIntent.Primary)
    BraceBlueprintIconByName(pack, serverIconName,
        contentDescription = if (pack.find(serverIconName) != null) "Record location" else "Unknown icon, help shown",
        resolution = BraceBlueprintIconResolution.Px16)
}
val results = pack.search("map", limit = 20) // name, display name, group, and tags
```

For an icon-only action using imported artwork, register just the requested vector; the original default registry stays unchanged:

```kotlin
val searchRegistry = remember(pack) {
    BraceIconRegistry.empty().register(BraceBlueprintIconNames.Search,
        pack.find(BraceBlueprintIconNames.Search)!!)
}
BraceIconButton(BraceBlueprintIconNames.Search, label = "Search records",
    onClick = ::searchRecords, registry = searchRegistry)
```

`find` returns null for an unknown name; `BraceBlueprintIconByName` shows Brace's original Help glyph as a fallback. Give that fallback an accurate spoken description. Decorative glyphs use `contentDescription = null`; informative glyphs require a localized nonblank description. A glyph alone is not an action. To make an icon-only action, register the selected vector in a scoped `BraceIconRegistry` and render `BraceIconButton` with a localized label; its target remains at least 48dp and provides keyboard focus. Do not use icon appearance alone to communicate status. The static artwork has no animation and needs no reduced-motion substitution. At large text scales, keep a readable text label near informative glyphs.

The [pack manifest](../brace-blueprint-icons/src/main/assets/brace-blueprint-icons.json) records every pinned name, metadata, exact path/viewBox, and original SVG SHA-256 for both sizes, plus hashes of the upstream metadata and license. [The generator](../scripts/generate_blueprint_icons.py) checks all 1,412 legacy SVGs across both sizes against a pinned Blueprint checkout with `--check --upstream /path/to/checkout`; CI checks committed assets and generated Kotlin with `--check`. The [copied Apache-2.0 license](../brace-blueprint-icons/src/main/assets/blueprint-icons-LICENSE.txt) and [attribution/modification notice](../brace-blueprint-icons/src/main/assets/blueprint-icons-ATTRIBUTION.txt) ship in the AAR. Search tags and artwork remain Blueprint's licensed material. See the [component list](coverage.md) for current availability and test links.

## Optional next-generation Blueprint glyph pack

For outlined and filled artwork, add `brace-blueprint-icons-next`. It contains 695 outlined names, 386 filled variants, and a mapping from 706 legacy names. This optional pack does not change the 11 original Brace vectors. Its API remains in progress during the `1.0.0` release; [source and license details](attribution.md) are recorded separately.

```kotlin
implementation("io.github.joelromanpr.brace:brace-blueprint-icons-next:1.0.0")
```

Load its 762 KB packaged manifest once off the UI thread, then retain the immutable pack. `find` returns the requested outlined or filled variant, or null when a filled form is unavailable. A missing filled form draws its outline; an unknown name uses the original Brace Help glyph. Give that fallback an accurate spoken description. Type-safe names, runtime lookup, metadata search, and an explicit [legacy name map](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/icons/icons-name-map.json) are available:

```kotlin
val pack = withContext(Dispatchers.IO) { BraceBlueprintNextIconPack.load(context) }
val migrated = pack.nextNameForLegacy("search") // magnifying-glass
val filled = pack.find(BraceBlueprintNextIconNames.MagnifyingGlass,
    BraceBlueprintNextIconVariant.Filled)
BraceTheme {
    BraceBlueprintNextIcon(pack, BraceBlueprintNextIconNames.MagnifyingGlass,
        contentDescription = "Search symbol", variant = BraceBlueprintNextIconVariant.Filled,
        intent = BraceIconIntent.Primary)
}
val results = pack.search("magnifying", limit = 20)
```

Decorative glyphs use a null description; informative glyphs require a localized nonblank one. For icon-only actions, register the chosen vector in a scoped `BraceIconRegistry` and use `BraceIconButton` for a named, focusable 48dp target. Theme color, brand, high contrast, explicit RTL mirroring, and reduced-motion behavior match the legacy pack. The 695 outlined SVGs include an empty `blank` and a 17×16 `cube-pen` viewBox. The source's one `<rect height="16"/>` has default width zero and draws no pixels; the generated Compose path omits that inert element.

The [next manifest](../brace-blueprint-icons-next/src/main/assets/brace-blueprint-icons-next.json) records pinned metadata, source hashes, every SVG path/viewBox, and the migration mapping. [The generator](../scripts/generate_blueprint_next_icons.py) checks all 1,081 SVG bytes against a pinned Blueprint checkout with `--check --upstream /path/to/checkout`; CI checks committed output without a checkout. The [Apache-2.0 license copy](../brace-blueprint-icons-next/src/main/assets/blueprint-icons-next-LICENSE.txt) and [attribution/modification notice](../brace-blueprint-icons-next/src/main/assets/blueprint-icons-next-ATTRIBUTION.txt) ship in the AAR. The [component list](coverage.md) records current availability and test links.

## Artwork, license, and verification

All 11 bundled vector paths in [BundledVectors.kt](../brace-icons/src/main/java/io/github/joelromanpr/brace/icons/BundledVectors.kt) were drawn for Brace Android. They are licensed under this repository's [Apache-2.0 LICENSE](../LICENSE). The [artifact asset manifest](../brace-icons/src/main/assets/brace-icons-manifest.json) records each name, source, author, and license and ships in the AAR. No Blueprint path is copied into `brace-icons`. The optional `brace-blueprint-icons` and `brace-blueprint-icons-next` artifacts contain licensed path data and the separate attributions described above. Apps registering other third-party artwork own its license and attribution.

Focused tests cover manifest/registry parity, immutable custom registration, runtime fallback, decorative and announced semantics, sizing, an accessible high-contrast icon button, and RTL mirroring. The [component list](coverage.md) links to the current tests and status. Manual TalkBack, hardware keyboard, mouse, and cross-device review remain required before a stable row or release is claimed.
