#!/usr/bin/env python3
"""Check BraceLink text against its default token surfaces and interaction fills.

Normal themes require WCAG AA 4.5:1 for body text; high-contrast themes require
7:1. Caller-supplied inherited colors, brand colors, and theme overrides are
outside this default-token check and remain the caller's responsibility.
"""

import json
from pathlib import Path

source = json.loads((Path(__file__).resolve().parents[1] / "tokens/v1/brace.tokens.json").read_text())
palette = source["palette"]


def luminance(value):
    if value.startswith("{palette."):
        value = palette[value[9:-1]]
    digits = value.lstrip("#")[-6:]
    channels = [int(digits[index:index + 2], 16) / 255 for index in (0, 2, 4)]
    linear = [part / 12.92 if part <= 0.04045 else ((part + 0.055) / 1.055) ** 2.4 for part in channels]
    return sum(a * b for a, b in zip(linear, (0.2126, 0.7152, 0.0722)))


def contrast(first, second):
    lighter, darker = sorted((luminance(first), luminance(second)), reverse=True)
    return (lighter + 0.05) / (darker + 0.05)


failed = []
for theme_name, theme in source["themes"].items():
    high = theme_name.startswith("highContrast")
    threshold = 7.0 if high else 4.5
    pressed_surface = "surfaceInset" if high else "pressed"
    colors = {
        "primary": ("primary", "primaryHover", "primary" if high else "primaryPressed"),
        "success": ("success", "success", "success"),
        "warning": ("warning", "warning", "warning"),
        "danger": ("danger", "danger" if high else "dangerHover", "dangerPressed"),
        "inherit": ("onSurface", "onSurface", "onSurface"),
    }
    for intent, (rest, hover, pressed) in colors.items():
        pairs = [(rest, background, "rest") for background in ("background", "surface", "surfaceRaised", "surfaceInset")]
        pairs += [(hover, "hover", "hover"), (pressed, pressed_surface, "pressed")]
        for foreground, background, state in pairs:
            ratio = contrast(theme[foreground], theme[background])
            if ratio + 1e-9 < threshold:
                failed.append(f"{theme_name}/{intent}/{state} {foreground} on {background}: {ratio:.2f}:1 < {threshold:.1f}:1")
    disabled_ratio = contrast(theme["disabledContent"], theme["surface"])
    if disabled_ratio < 4.5:
        failed.append(f"{theme_name}/disabled on surface: {disabled_ratio:.2f}:1 < 4.5:1")

if failed:
    raise SystemExit("Link token contrast failures:\n" + "\n".join(failed))
print("Link token contrast: all normal-theme text pairs >= 4.5:1; high-contrast pairs >= 7:1")
