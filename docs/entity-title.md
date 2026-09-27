# Entity title

This source slice maps pinned Blueprint [EntityTitle documentation](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/entity-title/entity-title.mdx) and [public props](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/entity-title/entityTitle.tsx) to one native Compose API. The API is available in the `0.1.0-alpha01` preview; the inventory row remains **in progress** with no stable acceptance version.

```kotlin
BraceEntityTitle(
    title = "Quarterly report",
    subtitle = "Updated today",
    icon = { Text("◆", color = LocalContentColor.current) },
    tags = { BraceTag("Draft") },
    onTitleClick = { openReport() },
)
```

## Appearance and behavior

The title is an Android heading in accessibility semantics. The default visual style uses the Brace body-strong token; `BraceEntityTitleStyle.Subtitle`, `Title`, and `Display` provide stronger prominence without HTML heading tags. `fill = true` fills available width; the default wraps content. Built-in title and subtitle strings wrap at large font scales. `ellipsize = true` limits them to one line within a constrained width, keeps their full spoken strings, and shows measured-overflow help through the tooltip target. Title hover and subtitle touch long press passed on API 36; subtitle hover remains an open emulator interaction check. A short title shares its line with tags; when the title consumes the width, tags wrap to the next line rather than clipping.

The icon is a decorative Compose slot. It takes the component's `LocalContentColor`, so an icon from the `brace-icons` artifact or a caller-owned vector can share the theme. Tags are a separate slot after the title; actionable `BraceTag` instances keep their own focus stops. Rich `titleContent` and `subtitleContent` slots can replace visual text while the required strings remain the accessibility labels. Those visual slots should not contain interactive controls and must handle their own overflow.

```kotlin
BraceEntityTitle(
    title = "Compliance report",
    subtitle = "Open findings",
    modifier = Modifier.width(240.dp),
    ellipsize = true,
    fill = true,
    style = BraceEntityTitleStyle.Title,
    tags = { BraceTag("Review"); BraceTag("Priority") },
    onTitleClick = { navigator.openReport(reportId) },
)
```

Blueprint's `titleURL` becomes `onTitleClick`, a caller-owned native navigation callback. Only the title activates it; tags never trigger title navigation. The title receives a 48 dp target, a visible token-colored focus ring, and keyboard, mouse, and touch activation. Text-only titles are not added to the keyboard tab order. Apps should use a localized title and perform their own internal navigation or external-intent policy.

## Loading, theme, and density

`loading = true` replaces the icon, title, and optional subtitle with static token-styled placeholders. It removes title/tag actions and announces a loading label; no source content leaks into the accessibility tree. Static placeholders have no motion and therefore honor reduced-motion settings. Component colors and dimensions are generated from `tokens/v1/brace.tokens.json`; semantic roles resolve light, dark, high contrast, brand, and scoped overrides. Compact and comfortable density change the title/tag gap through `BraceTheme` while keeping the component metrics as upper bounds.

The catalog demonstrates loading, width, title prominence, tags, and navigation counts. Focused Compose tests cover heading/reading order, a single native labeled action, independent tag activation, keyboard title activation, loading, measured overflow, tag wrapping, rich slots, width, RTL and 2x text, and automated accessibility checks. The [EntityTitle PR](https://github.com/joelromanpr/brace-android/pull/35) records local and hosted checks, including a 320 × 640 visual review. Manual TalkBack, tablet/physical-device review, and the subtitle hover check remain before this row can become stable. No Blueprint assets or code were copied into the implementation.
