package io.github.braceandroid.foundation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Ensures a visual control has at least the foundation touch target in layout.
 * Use around compact controls; visual density does not shrink the hit target.
 */
fun Modifier.braceMinimumTouchTarget(): Modifier = sizeIn(
    minWidth = BraceTokenDefaults.sizing.touchTarget,
    minHeight = BraceTokenDefaults.sizing.touchTarget,
)

/** Draws a token-colored visible keyboard focus boundary when [focused]. */
@Composable
fun Modifier.braceFocusOutline(
    focused: Boolean,
    shape: Shape = RoundedCornerShape(BraceTheme.shape.sm),
    width: Dp = BraceTheme.sizing.focusRingWidth,
): Modifier = if (focused) border(width, BraceTheme.colors.semantic.focusRing, shape) else this

/**
 * WCAG relative-luminance contrast ratio of a foreground on an opaque background.
 * Semi-transparent foregrounds are composited onto [background]. The result is
 * in 1.0..21.0. A custom brand should reach at least 4.5 for ordinary text.
 */
fun braceContrastRatio(foreground: Color, background: Color): Double {
    require(background.alpha >= 0.999f) { "Background must be opaque" }
    val back = background.convert(ColorSpaces.Srgb)
    val front = foreground.convert(ColorSpaces.Srgb)
    val alpha = front.alpha.toDouble()
    fun luminance(color: Color): Double {
        fun linear(channel: Float): Double {
            val value = channel.toDouble()
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }
    val resolved = Color(
        red = (front.red * alpha + back.red * (1.0 - alpha)).toFloat(),
        green = (front.green * alpha + back.green * (1.0 - alpha)).toFloat(),
        blue = (front.blue * alpha + back.blue * (1.0 - alpha)).toFloat(),
        alpha = 1f,
    )
    val a = luminance(resolved)
    val b = luminance(back)
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}
