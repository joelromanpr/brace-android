#!/usr/bin/env python3
"""Generate the Compose token API from the versioned, platform-neutral JSON source.

Run `python3 scripts/generate_tokens.py` after changing tokens, or add `--check`
to fail when the checked-in Kotlin output is stale. No third-party packages are
needed so other Brace platforms can read the same source independently.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "tokens/v1/brace.tokens.json"
TARGET = (
    ROOT
    / "brace-foundation/src/main/java/io/github/braceandroid/foundation/GeneratedBraceTokens.kt"
)
IDENTIFIER = re.compile(r"^[a-z][A-Za-z0-9]*$")
HEX_COLOR = re.compile(r"^#[0-9A-Fa-f]{6}(?:[0-9A-Fa-f]{2})?$|^#[0-9A-Fa-f]{8}$")
REFERENCE = re.compile(r"^\{(palette|semantic)\.([A-Za-z][A-Za-z0-9]*)\}$")


def unique_pairs(pairs: list[tuple[str, object]]) -> dict[str, object]:
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate JSON key: {key}")
        result[key] = value
    return result


def ordered_keys(mapping: dict) -> list[str]:
    for key in mapping:
        if not IDENTIFIER.fullmatch(key):
            raise ValueError(f"Token key must be lower camel case: {key}")
    return sorted(mapping)


def class_name(key: str) -> str:
    return "Brace" + key[0].upper() + key[1:]


def color_literal(value: str, palette: dict[str, str]) -> str:
    match = REFERENCE.fullmatch(value)
    if match:
        if match.group(1) != "palette" or match.group(2) not in palette:
            raise ValueError(f"Invalid palette reference: {value}")
        value = palette[match.group(2)]
    if not HEX_COLOR.fullmatch(value):
        raise ValueError(f"Invalid color: {value}")
    digits = value[1:].upper()
    if len(digits) == 6:
        digits = "FF" + digits
    return f"Color(0x{digits})"


def dimension_literal(value: int | float) -> str:
    if isinstance(value, bool) or not isinstance(value, (int, float)) or value < 0:
        raise ValueError(f"Dimension must be a nonnegative number: {value}")
    return f"{value}.dp"


def data_class(name: str, fields: dict, field_type: str, *, nullable: bool = False) -> list[str]:
    suffix = "? = null" if nullable else ""
    lines = ["@Immutable", f"data class {name}("]
    for key in ordered_keys(fields):
        lines.append(f"    val {key}: {field_type}{suffix},")
    lines.append(")")
    lines.append("")
    return lines


def constructor(name: str, fields: dict, formatter, indent: str = "        ") -> list[str]:
    lines = [f"{name}("]
    for key in ordered_keys(fields):
        lines.append(f"{indent}{key} = {formatter(key, fields[key])},")
    lines.append("    )")
    return lines


def component_class(key: str, kind: str) -> str:
    return f"{class_name(key)}{kind}"


def validate(source: dict) -> None:
    expected_top = {
        "version", "description", "palette", "type", "spacingDp", "sizingDp",
        "shapeDp", "elevationDp", "motionMs", "density", "themes",
        "componentColors", "componentMetricsDp",
    }
    if set(source) != expected_top:
        raise ValueError(f"Unexpected or missing top-level sections: {set(source) ^ expected_top}")
    if not re.fullmatch(r"\d+\.\d+\.\d+", source["version"]):
        raise ValueError("Token version must use semantic versioning")
    palette = source["palette"]
    for key in ordered_keys(palette):
        color_literal(palette[key], {})
    themes = source["themes"]
    if set(themes) != {"light", "dark", "highContrastLight", "highContrastDark"}:
        raise ValueError("Expected light, dark, and both high-contrast themes")
    roles = set(themes["light"])
    for theme_name, theme in themes.items():
        if set(theme) != roles:
            raise ValueError(f"Semantic role mismatch in {theme_name}: {set(theme) ^ roles}")
        for role in ordered_keys(theme):
            color_literal(theme[role], palette)
    for key in ("spacingDp", "sizingDp", "shapeDp", "elevationDp", "motionMs"):
        for name in ordered_keys(source[key]):
            dimension_literal(source[key][name])
    if set(source["density"]) != {"compact", "comfortable"}:
        raise ValueError("Density requires compact and comfortable")
    density_fields = set(source["density"]["compact"])
    for density in source["density"].values():
        if set(density) != density_fields:
            raise ValueError("Density token fields differ")
        for key in ordered_keys(density):
            dimension_literal(density[key])
    for family, fields in source["componentColors"].items():
        ordered_keys({family: None})
        for key in ordered_keys(fields):
            match = REFERENCE.fullmatch(fields[key])
            if not match or match.group(1) != "semantic" or match.group(2) not in roles:
                raise ValueError(f"{family}.{key} must reference a semantic color role")
    if set(source["componentMetricsDp"]) != set(source["componentColors"]):
        raise ValueError("Color and metric component families differ")
    for family, fields in source["componentMetricsDp"].items():
        for key in ordered_keys(fields):
            dimension_literal(fields[key])
    typography = source["type"]
    for style_name, style in typography["styles"].items():
        ordered_keys({style_name: None})
        if set(style) != {"family", "size", "lineHeight", "weight"}:
            raise ValueError(f"Invalid typography style fields: {style_name}")
        for field, collection in (
            ("family", "fontFamilies"), ("size", "sizesSp"),
            ("lineHeight", "lineHeightsSp"), ("weight", "weights"),
        ):
            if style[field] not in typography[collection]:
                raise ValueError(f"Unknown {field} in {style_name}: {style[field]}")
    if typography["fontFamilies"] != {"body": "sans-serif", "mono": "monospace"}:
        raise ValueError("The v1 Android generator supports sans-serif and monospace families")


def render(source: dict, digest: str) -> str:
    palette = source["palette"]
    semantic = source["themes"]["light"]
    components = source["componentColors"]
    metrics = source["componentMetricsDp"]
    lines = [
        "// Generated by scripts/generate_tokens.py. Do not edit by hand.",
        f"// Source: tokens/v1/brace.tokens.json (SHA-256 {digest})",
        "package io.github.braceandroid.foundation",
        "",
        "import androidx.compose.runtime.Immutable",
        "import androidx.compose.ui.graphics.Color",
        "import androidx.compose.ui.text.TextStyle",
        "import androidx.compose.ui.text.font.FontFamily",
        "import androidx.compose.ui.text.font.FontWeight",
        "import androidx.compose.ui.unit.Dp",
        "import androidx.compose.ui.unit.TextUnit",
        "import androidx.compose.ui.unit.dp",
        "import androidx.compose.ui.unit.sp",
        "",
        "/** Primitive colors from the platform-neutral v1 token source. */",
    ]
    lines += data_class("BracePalette", palette, "Color")
    lines += ["/** Semantic colors. Consume these roles instead of palette values in UI code. */"]
    lines += data_class("BraceSemanticColors", semantic, "Color")
    lines += ["/** Nullable semantic overrides for a nested [BraceTheme] scope. */"]
    lines += data_class("BraceSemanticColorOverrides", semantic, "Color", nullable=True)
    lines += ["internal fun BraceSemanticColors.withOverrides(overrides: BraceSemanticColorOverrides) = copy("]
    for key in ordered_keys(semantic):
        lines.append(f"    {key} = overrides.{key} ?: {key},")
    lines += [")", ""]
    for family in ordered_keys(components):
        lines += [f"/** Color roles and visual states for {family} components. */"]
        lines += data_class(component_class(family, "Colors"), components[family], "Color")
    lines += ["/** Color tokens for all component families. */", "@Immutable", "data class BraceComponentColors("]
    for family in ordered_keys(components):
        lines.append(f"    val {family}: {component_class(family, 'Colors')},")
    lines += [")", "", "@Immutable", "data class BraceColorScheme(",
              "    val semantic: BraceSemanticColors,", "    val components: BraceComponentColors,", ")", ""]
    for section, name in (
        ("spacingDp", "BraceSpacingTokens"), ("sizingDp", "BraceSizingTokens"),
        ("shapeDp", "BraceShapeTokens"), ("elevationDp", "BraceElevationTokens"),
    ):
        lines += [f"/** {section} values in Android dp. */"]
        lines += data_class(name, source[section], "Dp")
    lines += ["/** Animation durations in milliseconds. Reduced motion resolves all durations to zero. */"]
    lines += data_class("BraceMotionTokens", source["motionMs"], "Int")
    lines += ["/** Control and layout measures for the selected density. Touch targets remain 48 dp. */"]
    lines += data_class("BraceDensityTokens", source["density"]["compact"], "Dp")
    for family in ordered_keys(metrics):
        lines += [f"/** Dimensional tokens for {family} components. */"]
        lines += data_class(component_class(family, "Metrics"), metrics[family], "Dp")
    lines += ["/** Dimensional tokens for all component families. */", "@Immutable", "data class BraceComponentMetrics("]
    for family in ordered_keys(metrics):
        lines.append(f"    val {family}: {component_class(family, 'Metrics')},")
    lines += [")", ""]
    lines += ["/** Primitive scalable font sizes. */"]
    lines += data_class("BraceFontSizeTokens", source["type"]["sizesSp"], "TextUnit")
    lines += ["/** Primitive scalable line heights. */"]
    lines += data_class("BraceLineHeightTokens", source["type"]["lineHeightsSp"], "TextUnit")
    lines += ["/** Named font weights. */"]
    lines += data_class("BraceFontWeightTokens", source["type"]["weights"], "FontWeight")
    lines += ["/** Named font families. */"]
    lines += data_class("BraceFontFamilyTokens", source["type"]["fontFamilies"], "FontFamily")
    lines += ["/** Primitive type scale for custom text styles. */", "@Immutable", "data class BraceTypeScaleTokens(",
              "    val fontFamilies: BraceFontFamilyTokens,", "    val fontSizes: BraceFontSizeTokens,",
              "    val lineHeights: BraceLineHeightTokens,", "    val weights: BraceFontWeightTokens,", ")", ""]
    lines += ["/** Named Compose text styles generated from platform-neutral type tokens. */", "@Immutable", "data class BraceTypographyTokens("]
    for key in ordered_keys(source["type"]["styles"]):
        lines.append(f"    val {key}: TextStyle,")
    lines += [")", ""]
    lines += ["internal fun buildBraceComponentColors(semantic: BraceSemanticColors) = BraceComponentColors("]
    for family in ordered_keys(components):
        lines.append(f"    {family} = {component_class(family, 'Colors')}(")
        for field in ordered_keys(components[family]):
            ref = REFERENCE.fullmatch(components[family][field])
            lines.append(f"        {field} = semantic.{ref.group(2)},")
        lines.append("    ),")
    lines += [")", ""]
    lines += [
        "/** Generated defaults. The JSON source is also suitable for other platforms. */",
        "object BraceTokenDefaults {",
        f'    const val version: String = "{source["version"]}"',
        "    val palette = BracePalette(",
    ]
    for key in ordered_keys(palette):
        lines.append(f"        {key} = {color_literal(palette[key], {})},")
    lines += ["    )", ""]
    for theme_name in ("light", "dark", "highContrastLight", "highContrastDark"):
        lines.append(f"    private val {theme_name}Semantic = BraceSemanticColors(")
        for key in ordered_keys(source["themes"][theme_name]):
            lines.append(f"        {key} = {color_literal(source['themes'][theme_name][key], palette)},")
        lines += ["    )", f"    val {theme_name} = BraceColorScheme(",
                  f"        semantic = {theme_name}Semantic,",
                  f"        components = buildBraceComponentColors({theme_name}Semantic),",
                  "    )", ""]
    for section, property_name, class_type in (
        ("spacingDp", "spacing", "BraceSpacingTokens"),
        ("sizingDp", "sizing", "BraceSizingTokens"),
        ("shapeDp", "shape", "BraceShapeTokens"),
        ("elevationDp", "elevation", "BraceElevationTokens"),
        ("motionMs", "motion", "BraceMotionTokens"),
    ):
        lines.append(f"    val {property_name} = {class_type}(")
        for key in ordered_keys(source[section]):
            value = source[section][key]
            literal = str(value) if section == "motionMs" else dimension_literal(value)
            lines.append(f"        {key} = {literal},")
        lines += ["    )", ""]
    for density_name in ("compact", "comfortable"):
        lines.append(f"    val {density_name} = BraceDensityTokens(")
        for key in ordered_keys(source["density"][density_name]):
            lines.append(f"        {key} = {dimension_literal(source['density'][density_name][key])},")
        lines += ["    )", ""]
    lines += ["    val componentMetrics = BraceComponentMetrics("]
    for family in ordered_keys(metrics):
        lines.append(f"        {family} = {component_class(family, 'Metrics')}(")
        for key in ordered_keys(metrics[family]):
            lines.append(f"            {key} = {dimension_literal(metrics[family][key])},")
        lines.append("        ),")
    lines += ["    )", ""]
    type_data = source["type"]
    lines += ["    val typeScale = BraceTypeScaleTokens(", "        fontFamilies = BraceFontFamilyTokens("]
    for key in ordered_keys(type_data["fontFamilies"]):
        family = "SansSerif" if type_data["fontFamilies"][key] == "sans-serif" else "Monospace"
        lines.append(f"            {key} = FontFamily.{family},")
    lines += ["        ),", "        fontSizes = BraceFontSizeTokens("]
    for key in ordered_keys(type_data["sizesSp"]):
        lines.append(f"            {key} = {type_data['sizesSp'][key]}.sp,")
    lines += ["        ),", "        lineHeights = BraceLineHeightTokens("]
    for key in ordered_keys(type_data["lineHeightsSp"]):
        lines.append(f"            {key} = {type_data['lineHeightsSp'][key]}.sp,")
    lines += ["        ),", "        weights = BraceFontWeightTokens("]
    for key in ordered_keys(type_data["weights"]):
        lines.append(f"            {key} = FontWeight.W{type_data['weights'][key]},")
    lines += ["        ),", "    )", ""]
    lines += ["    val typography = BraceTypographyTokens("]
    for style_name in ordered_keys(type_data["styles"]):
        style = type_data["styles"][style_name]
        family = "SansSerif" if style["family"] == "body" else "Monospace"
        size = type_data["sizesSp"][style["size"]]
        height = type_data["lineHeightsSp"][style["lineHeight"]]
        weight = type_data["weights"][style["weight"]]
        lines += [f"        {style_name} = TextStyle(",
                  f"            fontFamily = FontFamily.{family},",
                  f"            fontSize = {size}.sp,",
                  f"            lineHeight = {height}.sp,",
                  f"            fontWeight = FontWeight.W{weight},",
                  "        ),"]
    lines += ["    )", "}", ""]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail when generated Kotlin is stale")
    arguments = parser.parse_args()
    try:
        source_bytes = SOURCE.read_bytes()
        source = json.loads(source_bytes, object_pairs_hook=unique_pairs)
        validate(source)
        generated = render(source, hashlib.sha256(source_bytes).hexdigest())
    except (OSError, ValueError, KeyError, TypeError) as error:
        print(f"token generation failed: {error}", file=sys.stderr)
        return 1
    if arguments.check:
        if not TARGET.exists() or TARGET.read_text() != generated:
            print(f"generated Kotlin is stale: {TARGET.relative_to(ROOT)}", file=sys.stderr)
            return 1
        print("Brace tokens are current")
        return 0
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_text(generated)
    print(f"generated {TARGET.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
