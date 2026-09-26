# Icons and icon loading

This `brace-icons` source slice maps the pinned Blueprint 6.18.0 [Icon component](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/icon/icon.mdx) and [icon loading](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/icons/src/loading-icons.mdx) capabilities (`@blueprintjs/icons` 6.13.0 at that commit) to Compose. Both inventory rows remain **in progress**. The separate opt-in [Blueprint glyph pack](#optional-blueprint-glyph-pack) is **in progress** with all 706 names from the pinned icons package. `brace-icons` continues to bundle only 11 original Brace drawings. No Maven Central version has shipped.

## Install and use

Publish the local source snapshot with `./gradlew :brace-foundation:publishToMavenLocal :brace-icons:publishToMavenLocal`, then add the aligned coordinate:

```kotlin
implementation("io.github.joelromanpr.brace:brace-icons:0.1.0-SNAPSHOT")
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

`BraceIconSize.Small`, `Medium`, and `Large` use the current Brace sizing tokens (16, 20, and 24dp in the first token source). `customSize` accepts a positive dp size when an app needs a different scale. `BraceIconIntent` uses semantic default, primary, success, warning, or danger colors; `tint` can override that color. Theme, brand, and contrast switches update the rendered tint at runtime. `ChevronForward` mirrors in RTL. These are single 24-unit original drawings scaled to the requested size; Brace does not choose separate Blueprint 16px and 20px assets.

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

Blueprint's React static imports, JavaScript dynamic chunks and loader options, SVG/DOM wrapper props, CSS icon fonts, and `tagName` have no independent Android API. Compose `ImageVector`, `Modifier`, composition scoping, and Android semantics provide the native behavior. The pinned `Icon` props for intent and size map to Brace semantic tint and dp sizing; browser effects and HTML attributes remain application-specific Compose drawing or modifiers.

## Optional Blueprint glyph pack

`brace-blueprint-icons` is an **opt-in**, aligned-version artifact. It does not replace `brace-icons` or change `BraceIconRegistry.Default`. It imports the 706 names in pinned `@blueprintjs/icons` 6.13.0, with distinct 16px and 20px SVG paths. Add it only when the complete pinned artwork is needed:

```kotlin
implementation("io.github.joelromanpr.brace:brace-blueprint-icons:0.1.0-SNAPSHOT")
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

The [pack manifest](../brace-blueprint-icons/src/main/assets/brace-blueprint-icons.json) records every pinned name, metadata, exact path/viewBox, and original SVG SHA-256 for both sizes, plus hashes of the upstream metadata and license. [The generator](../scripts/generate_blueprint_icons.py) checks all 706 SVGs against a pinned Blueprint checkout with `--check --upstream /path/to/checkout`; CI checks committed assets and generated Kotlin with `--check`. The [copied Apache-2.0 license](../brace-blueprint-icons/src/main/assets/blueprint-icons-LICENSE.txt) and [attribution/modification notice](../brace-blueprint-icons/src/main/assets/blueprint-icons-ATTRIBUTION.txt) ship in the AAR. Search tags and artwork remain Blueprint's licensed material. See the [M35 report](milestones/m35-blueprint-icon-pack.md) for test results and open review gates.

## Artwork, license, and verification

All 11 bundled vector paths in [BundledVectors.kt](../brace-icons/src/main/java/io/github/joelromanpr/brace/icons/BundledVectors.kt) were drawn for Brace Android. They are licensed under this repository's [Apache-2.0 LICENSE](../LICENSE). The [artifact asset manifest](../brace-icons/src/main/assets/brace-icons-manifest.json) records each name, source, author, and license and ships in the AAR. No Blueprint path is copied into `brace-icons`. The optional `brace-blueprint-icons` artifact contains licensed path data and the separate attribution described above. Apps registering other third-party artwork own its license and attribution.

Focused tests cover manifest/registry parity, immutable custom registration, runtime fallback, decorative and announced semantics, sizing, an accessible high-contrast icon button, and RTL mirroring. The [M11 report](milestones/m11-icons.md) records results and pending gates. Manual TalkBack, hardware keyboard, mouse, and cross-device review remain required before a stable row or release is claimed.
