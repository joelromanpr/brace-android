# Brace design tokens

`v1/brace.tokens.json` is the authored, platform-neutral token source. It is original Brace work under the repository's Apache-2.0 license. The JSON describes colors as hex sRGB, lengths as logical dp-like values, scalable type in sp-like values, and motion in milliseconds. It intentionally contains no Compose or Material 3 references.

The Android generator is `scripts/generate_tokens.py`, and its committed output is `brace-foundation/src/main/java/io/github/braceandroid/foundation/GeneratedBraceTokens.kt`. Run it after each token edit, then run `python3 scripts/generate_tokens.py --check` to verify that output has not drifted. `./gradlew checkTokenGeneration` is the repository task used by CI.

The additive select, radio, segmented control, date picker, date input, and table color and metric groups advance the v1 token contract to `1.1.0`. New platforms should read the JSON directly and generate native typed values. Preserve semantic role names and token version across platforms; platform-specific interaction rules and accessibility checks live in each implementation. A major token version may add or rename roles. Do not copy Blueprint styles or assets into this file.
