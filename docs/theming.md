# Theming Brace

`brace-foundation` owns Brace's Compose theme and generated design tokens. Every Brace component reads semantic or component tokens from `BraceTheme`. Primitive colors are exposed for tooling and brand exploration, but app and component code should normally use semantic roles.

## Token source and version

The v1 token contract is at `1.2.0` after the additive selection-card component group. The platform-neutral source is [`tokens/v1/brace.tokens.json`](../tokens/v1/brace.tokens.json). Its `version` is the token contract version and can evolve separately from the library's Maven version. The source records primitive palettes, semantic light/dark/high-contrast roles, typography styles and scales, spacing, sizing, shape, elevation, motion, compact/comfortable density, and visual states and dimensions for each component family. Logical lengths map to Android `dp`; type sizes map to `sp`; motion maps to milliseconds. Colors use `#RRGGBB` or `#AARRGGBB`.

Run `python3 scripts/generate_tokens.py` after editing the JSON. Commit both source and generated Kotlin. `python3 scripts/generate_tokens.py --check` fails if they differ. The generator validates semantic-role completeness across modes, references, dimensions, typography references, and component families. The generated file carries a SHA-256 of its source, giving iOS and Flutter maintainers a way to verify they consumed the same version. Add new tokens to the JSON first; do not edit `GeneratedBraceTokens.kt` directly.

The generated Android types include `BracePalette`, `BraceSemanticColors`, `BraceComponentColors`, `BraceTypographyTokens`, `BraceTypeScaleTokens`, and typed spacing, sizing, shape, elevation, motion, density, and component metric groups. `BraceTokenDefaults` exposes the four base color schemes and primitive scales. The foundation module ships no third-party icons or visual assets.

## App theme and runtime changes

```kotlin
var dark by rememberSaveable { mutableStateOf(false) }
var highContrast by rememberSaveable { mutableStateOf(false) }
var compact by rememberSaveable { mutableStateOf(false) }
var greenBrand by rememberSaveable { mutableStateOf(false) }

BraceTheme(
    mode = if (dark) BraceColorMode.Dark else BraceColorMode.Light,
    contrast = if (highContrast) BraceContrast.High else BraceContrast.Standard,
    density = if (compact) BraceDensity.Compact else BraceDensity.Comfortable,
    brand = if (greenBrand) {
        BraceBrandColors(primary = Color(0xFF005B4A), onPrimary = Color.White)
    } else null,
) {
    // Controls that change dark, highContrast, compact, or greenBrand can live here.
    // Any change recomposes Brace children with the resolved tokens.
    Text(
        text = "A dense workspace",
        color = BraceTheme.colors.semantic.onSurface,
        style = BraceTheme.typography.body,
    )
}
```

At the root, omitted `mode` follows the Android dark setting. `contrast` defaults to `Standard`, `density` to `Comfortable`, and `motion` to `System`. A nested `BraceTheme` inherits its parent settings. Theme controls are caller state; `rememberSaveable`, a `ViewModel`, or persisted user preferences can restore them across configuration changes and process recreation.

Brand colors replace the primary family, derived hover and pressed colors, focus ring, selection, and related component states. For a fully specified brand, supply optional `primaryHover`, `primaryPressed`, `primarySubtle`, `onPrimarySubtle`, `focusRing`, `selection`, and `onSelection`. Check contrast after customization: the theme does not silently alter caller-supplied brand colors.

## Scoped overrides

```kotlin
BraceTheme {
    Workspace()
    BraceTheme(
        overrides = BraceThemeOverrides(
            colors = BraceSemanticColorOverrides(surface = Color(0xFFFFFAEF)),
            typography = BraceTokenDefaults.typography.copy(
                title = BraceTokenDefaults.typography.title.copy(fontSize = 24.sp),
            ),
        ),
    ) {
        InspectorPanel()
    }
}
```

A semantic color override rebuilds component state colors in that nested scope. Other typed groups can be replaced with `BraceThemeOverrides` (`spacing`, `sizing`, `shape`, `elevation`, `motion`, `componentMetrics`, and `typography`). When a nested scope selects a different light/dark or contrast mode, it starts from that mode's base colors and reapplies the inherited brand. Keep local overrides small and review their contrast in every mode they support.

The additive token contract version `1.1.0` adds select, radio, segmented-control, date-picker, and date-input semantic visual states and dimensions. Existing token names remain unchanged.

## Using tokens in components

```kotlin
val colors = BraceTheme.colors.components.button
val metrics = BraceTheme.componentMetrics.button
val controlHeight = BraceTheme.densityTokens.controlHeightDp
val touchTarget = BraceTheme.sizing.touchTarget
```

`BraceTheme.colors.semantic` contains surfaces, content, borders, focus, selection, disabled states, and primary/success/warning/danger roles. `BraceTheme.colors.components` supplies state roles for button, input, checkbox, switch, card, callout, progress, menu, select, dialog, table, and tag. `BraceTheme.componentMetrics` supplies their dimensions. The `BraceTheme.typography` named styles use `sp` and respond to Android font-scale settings. `BraceTokenDefaults.typeScale` exposes primitive size, line-height, weight, and family values for custom styles.

Compact controls are visually shorter, but the token `sizing.touchTarget` remains 48 dp. Use `Modifier.braceMinimumTouchTarget()` or an equivalent 48 dp interactive region. Do not force text into fixed-height containers: use a minimum height so large text can expand. `Modifier.braceFocusOutline(focused)` draws a token-colored 2 dp keyboard focus boundary; pass focus state from the component's interaction source. Keyboard and mouse states should be visually distinct from disabled state.

## Accessibility checks

- **Contrast:** `braceContrastRatio(foreground, opaqueBackground)` computes WCAG relative-luminance contrast after compositing a translucent foreground. Default text pairs are unit-tested at 4.5:1 or higher; high-contrast body text is tested at 7:1 or higher. Re-run checks for custom brand pairs and all surfaces that show text.
- **Large text:** check 1.3x and 2.0x Android font scales. Text must wrap or grow rather than clip, and touch targets must remain reachable.
- **RTL:** Brace uses semantic start/end layout decisions. Test Arabic or Hebrew with Android's forced RTL developer option, checking navigation order and icons that convey direction.
- **Reduced motion:** `BraceMotion.System` observes Android's animator-duration scale. `BraceMotion.Reduced` forces generated durations to zero; `BraceMotion.Full` uses full motion. Applications with their own accessibility preference can pass it explicitly. A motion change should not remove state feedback.
- **Focus and input:** test D-pad, Tab and Shift+Tab, Enter/Space, pointer hover, and TalkBack. Keep focus visible against all four color schemes.
- **Touch:** verify at least 48 by 48 dp interactive regions even in Compact density.

Automated Compose accessibility checks should run for each interactive component where the Android test environment supports them, followed by manual TalkBack and hardware-input checks. Token contrast tests cover the defaults, not arbitrary app brand overrides.

## Material 3 interoperability

Brace does not install a `MaterialTheme` and keeps its own visual identity. Material 3 components can be used inside `BraceTheme`, but they read `MaterialTheme` values until you explicitly map Brace colors and type styles into a Material 3 theme. Likewise, Brace components continue to read Brace tokens inside `MaterialTheme`. For a mixed screen, nest the themes and map the roles you use (`primary`, `onPrimary`, `surface`, `onSurface`, error and typography); test both light and dark outcomes. No Material 3 dynamic-color setting automatically replaces Brace's design tokens.
