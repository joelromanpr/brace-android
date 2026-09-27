# M36 — semantic content adapters

**Status:** source integrated through M16 protected main; final-head checks and hosted review pending. No public artifact or stable inventory claim.

## Delivered in this slice

- Native Compose equivalents for the pinned H1–H6, Blockquote, Code, Pre, OL, and UL HTML wrappers in `brace-core`.
- Inventory, generated coverage, catalog samples and copyable usage, documentation, device tests, and an independent Maven consumer reference updated together.
- Existing Brace semantic colors and typography drive light/dark/contrast/brand variants; text follows Android font scaling, RTL, and collection/heading semantics.

## Verification

- On protected main `47989fc0f56652bee74223e08760907eeb6d9107`, inventory and documentation generation passed with 148 rows and 0/122 applicable rows stable. M34, M21, M15, M22, and M16 were then integrated; generated coverage and site/static checks still pass on M16 main `dd8ab119805a8676fa72a055d0803f4923cf24a3`. The site build validates 23 existing captures and 66 guides, including direct semantic-content and M36 audit routes. The combined Gradle and device gates below were run before M34/M21/M15/M22/M16 integration; final-head verification remains pending.
- To keep the growing catalog under the JVM method-size limit, the semantic sample is dispatched to a private composable; its public API and behavior did not change.
- Combined `build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck :catalog:assembleDebug` passed offline on the replay (836 tasks). The API dump was regenerated to match canonical class ordering; the exported semantic-content signatures did not change.
- API 36 at 320×640, 160 dpi, font scale 1.0: focused `BraceSemanticContentTest` passed 3/3 (71 Gradle tasks), covering headings through theme changes, RTL list collection/order semantics, and readable noninteractive quote/code semantics.
- Eight aligned `0.1.0-SNAPSHOT` artifacts published to Maven Local (308 tasks). Each has an AAR, sources JAR, KDoc JAR, POM, and Gradle Module Metadata. The separate Maven-coordinate-only consumer compiled with `BraceHeading2` and `BraceOrderedList` (37 tasks). This is local verification, not a Maven Central release.
- Default inset text contrast from the platform-neutral token source: 13.65:1 light, 17.16:1 dark, 19.54:1 high-contrast light, 21.0:1 high-contrast dark. Muted quote citation text against the same inset surface: 5.59:1, 10.53:1, 12.22:1, and 15.47:1 respectively. Brand and scoped override colors still need consumer review.
- Earlier M36 topic branch, before the current-main replay: full core API 36 suite had 174 tests total, 173 passed, one pre-existing overlay test skipped, zero failures. This full suite has not been rerun on the replay; the current combined build and focused device test above passed.
- On the earlier M36 topic branch, the 320×640 catalog heading detail was visually inspected in light and dark high-contrast modes, and 2× system text remained scrollable. That visual pass has not been repeated after the current-main replay. Manual TalkBack and brand/scoped-override visual review remain pending.

## Limits and remaining work

Android exposes heading status but no HTML H1–H6 level, so rank is visual and document order remains the author's responsibility. Code blocks preserve lines and scroll horizontally; syntax highlighting is outside this adapter. List APIs accept short plain-text collections; callers needing rich content or large lists should use custom lazy Compose lists. No public version is assigned until release. The wider pinned core, select, datetime, icon, and table inventories remain in progress or planned, as generated coverage shows.

## Next branch

The next focused branch is `joelromanpr/m37-sliders` for controlled Slider and RangeSlider interactions; M38 follows for MultiSlider. Both require their own local and hosted review evidence.
