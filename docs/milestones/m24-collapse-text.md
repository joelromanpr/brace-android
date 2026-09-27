# M24 · Collapse and Text

**Status:** in-progress source. Collapse and Text have no first release version; applicable released coverage remains **0/122**.

## Included

- `BraceCollapse` provides controlled disclosure, token-timed height changes, a reduced-motion snap, hidden-content input protection, and optional retained composition.
- `BraceText` provides semantic text styling, ellipsis, the full spoken string, and overflow or explicit-title help.
- The slice includes a catalog example, guide, API baseline, inventory evidence, Compose tests, and a separate Maven-coordinate consumer use.

## Verification

The earlier M11-based slice passed a **377-task** build/lint/API/catalog gate, **11/11** API 36 interaction and accessibility tests, foundation/core/icons Maven Local publication (**115 tasks**), and an independent consumer build (**37 tasks**). Hosted verify and API 34 instrumentation passed on that earlier head. The focused replay on merged selection and table-editing main passed core API, Android test compilation, catalog, token, and inventory checks; **11/11** API 36 Collapse/Text tests passed. All **eight** aligned artifacts were published to Maven Local with AAR, sources, documentation, POM, and Gradle metadata, and the independent coordinate-only Android consumer built. The Tree milestone is now integrated; token, coverage, JavaScript, and Pages checks pass with **148 rows, 24 real captures, 68 guides**. The catalog sample is isolated from the large component dispatcher. Tree-integrated hosted checks remain pending.

## Limits

The caller owns the disclosure trigger and expanded semantics. Manual TalkBack and further device-size review remain before stable status. EntityTitle is a separate planned slice.
