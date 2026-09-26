# M36 — semantic content adapters

**Status:** local implementation in review; no public artifact or stable inventory claim.

## Delivered in this slice

- Native Compose equivalents for the pinned H1–H6, Blockquote, Code, Pre, OL, and UL HTML wrappers in `brace-core`.
- Inventory, generated coverage, catalog samples and copyable usage, documentation, device tests, and an independent Maven consumer reference updated together.
- Existing Brace semantic colors and typography drive light/dark/contrast/brand variants; text follows Android font scaling, RTL, and collection/heading semantics.

## Verification

- Inventory, token, and documentation generation: pass, 147 rows, 0/121 applicable rows stable.
- Default inset text contrast from the platform-neutral token source: 13.65:1 light, 17.16:1 dark, 19.54:1 high-contrast light, 21.0:1 high-contrast dark. Muted quote citation text against the same inset surface: 5.59:1, 10.53:1, 12.22:1, and 15.47:1 respectively. Brand and scoped override colors still need consumer review.
- Exact core API dump/check, Kotlin compilation, and broad `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug`: passed (467 tasks). API 36 semantic-content tests: 3/3 passed. Full core suite: 174 tests total, 173 passed, one pre-existing overlay test skipped, zero failures. Maven Local publication of four artifacts passed (156 tasks); the separate consumer app compiled from those artifacts (37 tasks).
- The 320×640 catalog heading detail was visually inspected in light and dark high-contrast modes; 2× system text remained scrollable. Manual TalkBack and brand/scoped-override visual review remain pending.

## Limits and remaining work

Android exposes heading status but no HTML H1–H6 level, so rank is visual and document order remains the author's responsibility. Code blocks preserve lines and scroll horizontally; syntax highlighting is outside this adapter. List APIs accept short plain-text collections; callers needing rich content or large lists should use custom lazy Compose lists. No public version is assigned until release. The wider pinned core, select, datetime, icon, and table inventories remain in progress or planned, as generated coverage shows.

## Next branch

The next focused branch is `joelromanpr/m37-sliders` for controlled Slider and RangeSlider interactions; M38 follows for MultiSlider. Both require their own local and hosted review evidence.
