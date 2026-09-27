# Brace design tokens

`v1/brace.tokens.json` is the authored, platform-neutral token source. It is original Brace work under the repository's Apache-2.0 license. The JSON describes colors as hex sRGB, lengths as logical dp-like values, scalable type in sp-like values, and motion in milliseconds. It intentionally contains no Compose or Material 3 references.

The Android generator is `scripts/generate_tokens.py`, and its committed output is `brace-foundation/src/main/java/io/github/braceandroid/foundation/GeneratedBraceTokens.kt`. Run it after each token edit, then run `python3 scripts/generate_tokens.py --check` to verify that output has not drifted. `./gradlew checkTokenGeneration` is the repository task used by CI.

The token contract is `1.2.0`. It includes component colors and dimensions for controls, selection cards, sliders, button groups, and tabs. New platforms can read the same JSON and generate native typed values. Preserve semantic role names and token version across platforms; each platform defines its own interaction and accessibility behavior. A major token version may add or rename roles.
