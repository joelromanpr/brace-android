# M20: Links

**State:** source complete on `joelromanpr/m20-links`; awaiting hosted CI on the M30-based branch. Link and AnchorButton remain **in progress** in the [coverage ledger](../coverage.md). No Maven Central release is claimed.

## What this adds

- `BraceLinkDestination.Uri` and `.Action` for web/deep-link and in-app destinations.
- `BraceLink` with token colors, underline options, disabled state, 48 dp target, keyboard/focus/hover behavior, RTL, and an accessible destination label.
- `BraceLinkButton` with the current Brace button styles; loading and disabled states cannot navigate.
- Interactive catalog examples, [usage guide](../links.md), a real Android gallery capture, tests, and an updated public API baseline.

The inventory maps the pinned Blueprint 6.18.0 Link and AnchorButton docs to these Android APIs. Browser-only anchor attributes use Android URI handling or app navigation callbacks. See [the pinned baseline](../../BLUEPRINT_BASELINE.md) for the comparison source.

## Evidence on the M30-based tree

| Check | Result |
| --- | --- |
| Source integrity | Token and both pinned icon generators, coverage generation, JavaScript syntax, Link contrast, and Git whitespace checks passed. |
| Site | `node scripts/build-docs.mjs` passed: **148 rows, 14 real Android captures, 48 guides, 49 HTML pages, 2,716 local links; none missing**. The catalog includes a copyable time-picker example and valid table selection examples. |
| Build | `build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck :catalog:assembleDebug` passed **836 tasks**. `:catalog:assembleDebug :catalog:assembleRelease :catalog:lint` passed **702 tasks** after restoring the time-picker code example. |
| Device | API 36 `BraceLinkTest` passed **10/10**, with zero failures or skips. It covers routing, disabled/loading behavior, semantics, touch bounds, keyboard focus, RTL, large text, high contrast, and automated accessibility checks. |
| Maven Local | All **eight** aligned snapshot Android artifacts published with AAR, sources, KDoc, POM, and Gradle metadata (**308 tasks**). The separate coordinate-only consumer assembled offline (**37 tasks**). |
| Hosted | The earlier M55 source head passed [verify and API 34 instrumentation](https://github.com/joelromanpr/brace-android/actions/runs/36282602922) and [CodeQL](https://github.com/joelromanpr/brace-android/actions/runs/36282602879). The M30-based head needs fresh hosted checks. |

## Remaining acceptance

Manual TalkBack listening and current dark/high-contrast gallery review remain. Inline links inside rich text and the broader Button styles remain planned. The [coverage ledger](../coverage.md) is the authority for status; generated stable coverage is **0/122** applicable rows.
