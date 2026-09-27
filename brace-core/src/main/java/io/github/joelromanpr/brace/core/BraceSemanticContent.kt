package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.braceandroid.foundation.BraceTheme

/** Visual heading rank. Android accessibility exposes heading status, not an HTML heading level. */
public enum class BraceHeadingLevel { One, Two, Three, Four, Five, Six }

/**
 * Token-styled heading with Android heading semantics and natural font scaling.
 * [level] chooses visual hierarchy; TalkBack can navigate headings but Android
 * does not expose the six HTML heading ranks. Use ranks in document order.
 */
@Composable
public fun BraceHeading(
    text: String,
    modifier: Modifier = Modifier,
    level: BraceHeadingLevel = BraceHeadingLevel.Two,
) {
    val type = BraceTheme.typography
    val style = when (level) {
        BraceHeadingLevel.One -> type.display
        BraceHeadingLevel.Two -> type.title
        BraceHeadingLevel.Three -> type.subtitle
        BraceHeadingLevel.Four -> type.bodyStrong
        BraceHeadingLevel.Five -> type.label
        BraceHeadingLevel.Six -> type.caption
    }
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        color = BraceTheme.colors.semantic.onSurface,
        style = style,
    )
}

/** H1 HTML text styling mapped to a Compose heading. */
@Composable
public fun BraceHeading1(text: String, modifier: Modifier = Modifier) {
    BraceHeading(text, modifier, BraceHeadingLevel.One)
}

/** H2 HTML text styling mapped to a Compose heading. */
@Composable
public fun BraceHeading2(text: String, modifier: Modifier = Modifier) {
    BraceHeading(text, modifier, BraceHeadingLevel.Two)
}

/** H3 HTML text styling mapped to a Compose heading. */
@Composable
public fun BraceHeading3(text: String, modifier: Modifier = Modifier) {
    BraceHeading(text, modifier, BraceHeadingLevel.Three)
}

/** H4 HTML text styling mapped to a Compose heading. */
@Composable
public fun BraceHeading4(text: String, modifier: Modifier = Modifier) {
    BraceHeading(text, modifier, BraceHeadingLevel.Four)
}

/** H5 HTML text styling mapped to a Compose heading. */
@Composable
public fun BraceHeading5(text: String, modifier: Modifier = Modifier) {
    BraceHeading(text, modifier, BraceHeadingLevel.Five)
}

/** H6 HTML text styling mapped to a Compose heading. */
@Composable
public fun BraceHeading6(text: String, modifier: Modifier = Modifier) {
    BraceHeading(text, modifier, BraceHeadingLevel.Six)
}

/**
 * A quoted passage and optional source. Both remain ordinary readable text;
 * the border and inset are visual only. Long passages grow with font scaling.
 */
@Composable
public fun BraceBlockquote(
    text: String,
    modifier: Modifier = Modifier,
    citation: String? = null,
) {
    val colors = BraceTheme.colors.semantic
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceInset)
            .border(BraceTheme.sizing.borderWidth, colors.borderStrong)
            .padding(BraceTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm),
    ) {
        Text(text, color = colors.onSurface, style = BraceTheme.typography.body)
        if (!citation.isNullOrBlank()) {
            Text(citation, color = colors.onSurfaceMuted, style = BraceTheme.typography.label)
        }
    }
}

/** Inline selectable monospace text using semantic inset colors. */
@Composable
public fun BraceCode(text: String, modifier: Modifier = Modifier) {
    val colors = BraceTheme.colors.semantic
    SelectionContainer {
        Text(
            text = text,
            modifier = modifier.background(colors.surfaceInset)
                .padding(horizontal = BraceTheme.spacing.xs),
            color = colors.onSurface,
            style = BraceTheme.typography.code,
        )
    }
}

/**
 * A selectable preformatted code block. Lines are preserved and scroll
 * horizontally instead of shrinking text when the viewport is narrow.
 */
@Composable
public fun BraceCodeBlock(text: String, modifier: Modifier = Modifier) {
    val colors = BraceTheme.colors.semantic
    SelectionContainer {
        Row(
            modifier = modifier.fillMaxWidth()
                .background(colors.surfaceInset)
                .border(BraceTheme.sizing.borderWidth, colors.border)
                .horizontalScroll(rememberScrollState())
                .padding(BraceTheme.spacing.md),
        ) {
            Text(
                text = text,
                color = colors.onSurface,
                style = BraceTheme.typography.code,
                softWrap = false,
            )
        }
    }
}

/**
 * A short ordered text list with collection semantics. [start] supplies the
 * visible first number. For very long lists, use a LazyColumn of text items.
 */
@Composable
public fun BraceOrderedList(
    items: List<String>,
    modifier: Modifier = Modifier,
    start: Int = 1,
) {
    Column(
        modifier = modifier.semantics { collectionInfo = CollectionInfo(items.size, 1) },
        verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
    ) {
        items.forEachIndexed { index, item ->
            Text(
                text = "${start.toLong() + index}. $item",
                modifier = Modifier.semantics {
                    collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                },
                color = BraceTheme.colors.semantic.onSurface,
                style = BraceTheme.typography.body,
            )
        }
    }
}

/** A short unordered text list with one spoken text node per bullet. */
@Composable
public fun BraceUnorderedList(items: List<String>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics { collectionInfo = CollectionInfo(items.size, 1) },
        verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
    ) {
        items.forEachIndexed { index, item ->
            Text(
                text = "• $item",
                modifier = Modifier.semantics {
                    collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                },
                color = BraceTheme.colors.semantic.onSurface,
                style = BraceTheme.typography.body,
            )
        }
    }
}
