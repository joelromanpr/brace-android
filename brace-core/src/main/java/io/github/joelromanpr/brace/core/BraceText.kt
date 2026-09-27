package io.github.joelromanpr.brace.core

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.braceandroid.foundation.BraceTheme

/**
 * Token-styled text that can truncate to one line while keeping its full spoken text.
 *
 * [ellipsize] draws an ellipsis when the available width is insufficient. When
 * overflow is measured, pointer hover or touch long press reveals the full text
 * in a [BraceTooltip]. An explicit [title] overrides that tooltip content and
 * is available even without overflow.
 * The tooltip target retains a 48 dp minimum touch height. Android text
 * semantics always retain the untruncated [text]. The component
 * wraps normally when [ellipsize] is false and follows font scale and RTL text
 * direction from Compose.
 */
@Composable
public fun BraceText(
    text: String,
    modifier: Modifier = Modifier,
    ellipsize: Boolean = false,
    title: String? = null,
) {
    var isOverflowing by remember(text, ellipsize) { mutableStateOf(false) }
    val explicitTitle = title?.takeIf { it.isNotBlank() }
    val textContent: @Composable () -> Unit = {
        Text(
            text = text,
            color = BraceTheme.colors.semantic.onSurface,
            style = BraceTheme.typography.body,
            maxLines = if (ellipsize) 1 else Int.MAX_VALUE,
            softWrap = !ellipsize,
            overflow = if (ellipsize) TextOverflow.Ellipsis else TextOverflow.Clip,
            onTextLayout = { result ->
                if (ellipsize) isOverflowing = result.hasVisualOverflow
            },
            modifier = Modifier.semantics { contentDescription = text },
        )
    }
    if (ellipsize || explicitTitle != null) {
        BraceTooltip(
            text = explicitTitle ?: text,
            target = textContent,
            modifier = modifier.defaultMinSize(minHeight = BraceTheme.sizing.touchTarget),
            enabled = explicitTitle != null || isOverflowing,
        )
    } else {
        Text(
            text = text,
            color = BraceTheme.colors.semantic.onSurface,
            style = BraceTheme.typography.body,
            modifier = modifier,
        )
    }
}
