# File picker field

`BraceFilePickerField` adapts Blueprint's web `FileInput` to Android's system document picker. It is a controlled visual field in `brace-core`; its inventory row is **in progress**. It uses Brace input and button state tokens, and it does not read or upload selected files.

```kotlin
var names by rememberSaveable { mutableStateOf(emptyList<String>()) }
BraceFilePickerField(
    label = "Attachments",
    selectedNames = names,
    onFilesPicked = { uris -> names = uris.mapIndexed { index, _ -> "Document ${index + 1}" } },
    mimeTypes = listOf("application/pdf", "image/*"),
    multiple = true,
)
```

The whole field is one 48 dp or larger button target. Tap, mouse click, Enter, Space, or its TalkBack action opens `OpenDocument` or `OpenMultipleDocuments`. `mimeTypes` maps the HTML `accept` attribute, and `multiple` maps the browser's multiple-file option. The callback receives document URIs only when the user picks one or more files; cancellation leaves caller state unchanged. `selectedNames` is display-only and must be updated by the app. `buttonText`, `placeholder`, `helperText`, `errorText`, `enabled`, `size`, and `fill` cover Blueprint's visible field states. Compact density keeps the accessible target. The field automatically follows RTL layout and uses a visible keyboard focus ring even when an error border is present.

An Android document URI is a capability, not an uploaded file path. The app decides when to read it, queries its display name on a background dispatcher, and keeps that name and any URI state it needs. If access must survive a device restart, the app can call `contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)` inside `onFilesPicked` when the provider grants it, persist the URI as well, and handle later permission revocation; access can still disappear if the document is moved or deleted. See [Android's document picker contracts](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.OpenDocument) and [Storage Access Framework permission guidance](https://developer.android.com/training/data-storage/shared/documents-files). No broad storage permission is required for this picker. Camera capture is a separate Android action and is not added to this file-input row.

The catalog includes a live single/multiple, disabled, and error sample whose name and availability come from the generated inventory. Device tests use a fake `ActivityResultRegistry` to verify the document contracts and MIME types, result and cancellation behavior, and disabled state. They exercise touch, Enter, Space, mouse, the native accessibility click action, RTL, dark high contrast, and the 48 dp target. The catalog field was visually reviewed at 320 dp in light and dark high contrast, plus 200% Android text. A manual system document picker journey and human TalkBack pass remain before stable status.
