# Tooltips and toast notifications

This `brace-core` source slice implements the pinned Blueprint Tooltip, Toast, and OverlayToaster rows as Android Compose APIs. All three rows remain **in progress**. The [coverage ledger](coverage.md) records their source, sample, tests, and release status.

## Tooltip

```kotlin
BraceTooltip(
    text = "Imports include archived records",
    target = { BraceButton("Import", onClick = ::startImport) },
)
```

`BraceTooltip` provides brief, noninteractive help. Its target stays in normal layout; Material 3 handles pointer hover, touch long press, and the TalkBack show action. Brace keeps the tooltip open while keyboard focus remains on the target and clamps its position inside the window. The target needs its own accessible label and touch target. Set `enabled = false` to suppress the tooltip and dismiss one already showing. Text should be localized by the application. Use [BracePopover](drawers-popovers.md) when the content contains controls or needs more room. Material 3 supplies touch and pointer timing; its tooltip animation is outside Brace's motion tokens for now. Browser hover-only behavior, DOM target wrappers, and web intent props have no direct Android surface.

## Toast and host

```kotlin
val toasts = rememberBraceToastState(maxVisible = 3)
Box(Modifier.fillMaxSize()) {
    ScreenContent(
        onSaved = {
            toasts.show(
                BraceToastSpec(
                    message = "Changes saved",
                    intent = BraceToastIntent.Success,
                ),
            )
        },
    )
    BraceToastHost(toasts, position = BraceToastPosition.BottomEnd)
}
```

Put `BraceToastHost` as the last child of a full-screen `Box`. It is nonmodal: the rest of the screen remains interactive and a new toast does not take keyboard focus. `BottomEnd` is the Android default and follows RTL. Blueprint defaults to top center; choose `TopCenter` to match that placement. The host also supports the other top and bottom start, center, and end positions. Each toast uses Brace intent colors, a polite live region by default, a 48 dp close control, and optional action. Set `announceAssertively = true` only for an urgent message. Provide localized action labels. The default close description comes from Brace Android resources (English and Spanish); override `closeContentDescription` for other locales or wording. A toast with a positive timeout normally lasts 5 seconds; Android accessibility services may extend that duration. Hover and keyboard focus pause its timer. Set `durationMillis = 0` for a message that stays until dismissed.

```kotlin
toasts.show(
    BraceToastSpec(
        message = "Upload failed",
        intent = BraceToastIntent.Danger,
        durationMillis = 0,
        actionLabel = "Retry",
        onAction = ::retryUpload,
    ),
    key = "upload-status",
)
```

`BraceToastState` replaces Blueprint's static `OverlayToaster.create()` entry point with a scoped, observable Compose state holder. The default `maxVisible = 3` is an intentional Android adaptation; Blueprint's toaster has no equivalent default cap. Calling `show` with a key replaces that message and restarts its timer. New messages are placed nearest the selected edge; the oldest is evicted at the cap. A burst can therefore remove a message before TalkBack announces it. Apps should avoid using short-lived toasts as the sole channel for critical information; make such information available in persistent screen content. `dismiss` and `clear` report their reasons through an optional callback; `clearOnEscape` clears while keyboard focus is within the host. The state is scoped to its composition and is not saved across process death. A standalone `BraceToast` is also available when the caller wants to own removal directly.

Blueprint's DOM portal and global React toaster handle have no Android equivalent. Compose keeps the host with its screen, preserving explicit lifetime and allowing more than one screen to manage notifications independently. Blueprint CSS classes, web transition hooks, and exact toaster animation timing remain follow-up parity work.

## Verification and limitations

The source slice includes device tests for tooltip triggers and enabled state, toast action and dismissal paths, timeout behavior, keyboard focus, accessibility semantics, host bounds, and queue management. Local and hosted test results are tracked in the [M6 report](milestones/m6-tooltip-toast.md). Manual TalkBack, pointer, light/dark/high-contrast, and form-factor review remain before stable status. No Maven Central version has shipped.
