# M36 — semantic content adapters

**Status:** local implementation in review; no public artifact or stable inventory claim.

## Delivered in this slice

- Native Compose equivalents for the pinned H1–H6, Blockquote, Code, Pre, OL, and UL HTML wrappers in `brace-core`.
- Inventory, generated coverage, catalog samples and copyable usage, documentation, device tests, and an independent Maven consumer reference updated together.
- Existing Brace semantic colors and typography drive light/dark/contrast/brand variants; text follows Android font scaling, RTL, and collection/heading semantics.

## Verification

- Inventory, token, and documentation generation: pass, 147 rows, 0/121 applicable rows stable.
- Default inset text contrast from the platform-neutral token source: 13.65:1 light, 17.16:1 dark, 19.54:1 high-contrast light, 21.0:1 high-contrast dark. Muted quote citation text against the same inset surface: 5.59:1, 10.53:1, 12.22:1, and 15.47:1 respectively. Brand and scoped override colors still need consumer review.
- Kotlin API, broad build/lint, API 36 device suite, Maven Local, and independent consumer: pending the shared validation lane.
- Manual TalkBack, large-text visual inspection, and representative app layout: pending.

## Limits and remaining work

Android exposes heading status but no HTML H1–H6 level, so rank is visual and document order remains the author's responsibility. Code blocks preserve lines and scroll horizontally; syntax highlighting is outside this adapter. List APIs accept short plain-text collections; callers needing rich content or large lists should use custom lazy Compose lists. No public version is assigned until release. The wider pinned core, select, datetime, icon, and table inventories remain in progress or planned, as generated coverage shows.

## Next branch

Validate this branch after M32 PanelStack, M33 DateRange, and M34 TimezoneSelect release the shared Gradle/ADB lane. Then continue with the remaining form and navigation rows in a focused branch.
