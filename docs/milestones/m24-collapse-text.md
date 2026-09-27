# M24 · Collapse and Text

**Status:** in-progress source. Collapse and Text have no first release version; applicable released coverage remains **0/122**.

## Included

- `BraceCollapse` provides controlled disclosure, token-timed height changes, a reduced-motion snap, hidden-content input protection, and optional retained composition.
- `BraceText` provides semantic text styling, ellipsis, the full spoken string, and overflow or explicit-title help.
- The slice includes a catalog example, guide, API baseline, inventory evidence, Compose tests, and a separate Maven-coordinate consumer use.

## Verification

The earlier M11-based slice passed a **377-task** build/lint/API/catalog gate, **11/11** API 36 interaction and accessibility tests, foundation/core/icons Maven Local publication (**115 tasks**), and an independent consumer build (**37 tasks**). Hosted verify and API 34 instrumentation passed on that earlier head. The current focused replay on merged selection and table-editing main passes token, coverage, JavaScript, and Pages checks: **148 rows, 23 real captures, 66 guides**. Its new catalog sample is isolated from the large component dispatcher. Current-head compile, device, Maven consumer, and hosted checks are pending.

## Limits

The caller owns the disclosure trigger and expanded semantics. Manual TalkBack and further device-size review remain before stable status. EntityTitle is a separate planned slice.
