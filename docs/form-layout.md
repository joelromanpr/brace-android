# Labels and control groups

This `brace-core` source slice maps the pinned Blueprint 6.18.0 Label and ControlGroup components to Android Compose. Both inventory rows are available in the `1.0.0` release and remain **in progress**. The [generated coverage ledger](coverage.md) records release status.

## Field label

```kotlin
var format by rememberSaveable { mutableStateOf("CSV") }
BraceFieldLabel(
    label = "Export format",
    spokenLabel = "Export format, $format",
) { controlModifier ->
    BraceButton(
        label = format,
        onClick = { format = if (format == "CSV") "JSON" else "CSV" },
        modifier = controlModifier,
    )
}
```

Apply `controlModifier` to the outer focusable node of one control. Tapping the visual label moves focus to that control; its TalkBack announcement uses `spokenLabel`, which defaults to `label`. The label itself is hidden from TalkBack traversal and does not create a keyboard focus stop. Supply a localized `spokenLabel` when the control's action or current value needs to be part of the announcement. Label activation has a 48 dp minimum height. `enabled = false` dims the label and stops its focus action; disable the child separately. The label and control use Brace typography, semantic colors, spacing, and target sizing.

Use `BraceFormField` for helper text, error state, required announcements, or inline forms. Blueprint's HTML `<label>` wrapper, `htmlFor`, and arbitrary DOM attributes map to a Compose focus requester, control modifier, and Android semantics. The wrapper does not select a value or own child state. See the pinned [Label documentation](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/forms/label.mdx).

## Control group

```kotlin
var query by rememberSaveable { mutableStateOf("") }
BraceControlGroup(fill = true, accessibilityLabel = "Search records") {
    Item { controlModifier ->
        BraceTextField(query, { query = it }, "Query", modifier = controlModifier)
    }
    Item(fill = false) { controlModifier ->
        BraceButton("Search", onClick = { search(query) }, modifier = controlModifier)
    }
}
```

ControlGroup arranges **distinct controls**, with a Brace spacing token between them. The default is horizontal; set `vertical = true` for a column. With `fill = true`, the group occupies available space and its nonfixed items divide the main axis equally. Vertical equal fill requires a bounded parent height, such as a fixed-height container; a scrolling parent with unbounded height cannot allocate equal weighted space. Set `Item(fill = false)` to retain one control's natural size, or use `Item(fill = true)` to expand a specific control in an otherwise nonfilling group. Apply each item's `controlModifier` to its outer node. Each child retains its own touch, keyboard, focus, enabled state, and TalkBack action. The optional localized `accessibilityLabel` names the traversal group without replacing child labels. In a horizontal group, Compose mirrors item order under RTL. Children must each supply an accessible 48 dp target where interactive.

The pinned [ControlGroup documentation](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/forms/control-group.mdx) describes a lightweight flex wrapper. Brace uses Row or Column, a focus traversal group, and spacing; it does not join child borders or create a selection control. HTML attributes and CSS flex classes have no separate Android API.

## Verification and limits

The focused device suite covers label touch and mouse activation, keyboard traversal without an extra label stop, control semantics, disabled and large-text RTL high-contrast states, separate group actions, equal and fixed fill, vertical sizing, RTL order, and an automated Compose accessibility check. Manual TalkBack and real hardware keyboard/pointer review remain before stable status. Exact HTML flex-basis and class-specific border stacking are web mechanisms and are not reproduced as public Android APIs.
