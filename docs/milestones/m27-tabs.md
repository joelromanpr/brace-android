# M27 · Tabs

**Status:** in-progress source. Tabs, Tab, TabPanel, and TabsExpander have no first release version; released coverage remains **0/122 applicable rows**.

## Included

- Stable-ID tabs, controlled or saveable selection, active panels with saved content, horizontal and width-adaptive vertical layouts, disabled and badge states, a trailing action slot, and a Compose spacer.
- Keyboard, touch, mouse, RTL, focus, selected/disabled semantics, high contrast, large text, and reduced-motion behavior.
- Platform-neutral tab tokens, generated Kotlin, a catalog example, guide, API baseline, inventory evidence, Compose tests, and independent consumer use.

## Verification

The earlier source passed a **377-task** build/lint/API/catalog gate, **12/12** API 36 Tabs tests, foundation/core/icons Maven Local publication (**115 tasks**), and an independent consumer (**37 tasks**). Hosted verify and API 34 instrumentation passed on that earlier head. The current replay on merged Tree main passes token generation, coverage, JavaScript syntax, Pages, API baseline checks, foundation token tests, and catalog compilation with **148 rows, 24 real captures, 68 guides**. The focused API 36 Tabs suite passed **12/12**, zero failures/skips. All **eight** aligned Maven Local artifacts include AAR, sources, documentation, POM, and Gradle metadata; the separate coordinate-only consumer built. Tabs examples are isolated from the large catalog dispatcher. Current-head hosted checks remain pending.

## Limits

Interactive title children, Blueprint `fill`, a moving indicator, and automatic descendant focus return after a programmatic panel switch remain open. Manual TalkBack, tablet, and physical mouse review remain before stable status.
