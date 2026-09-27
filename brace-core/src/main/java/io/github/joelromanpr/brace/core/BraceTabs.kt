package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceTheme

/**
 * A tab title in [BraceTabs]. IDs must be unique in one strip and stable across recomposition.
 * [label] is the spoken name, even when [leadingIcon] is drawn. [badge] is appended to the
 * spoken name; use a localized [accessibilityLabel] when a numeric badge needs context.
 */
public data class BraceTab(
    val id: String,
    val label: String,
    val enabled: Boolean = true,
    val leadingIcon: (@Composable () -> Unit)? = null,
    val badge: String? = null,
    val accessibilityLabel: String? = null,
)

/** The tab strip direction and placement of its active panel. */
public enum class BraceTabsOrientation { Horizontal, Vertical }

/** Medium or large tab typography. Every actionable tab retains a 48 dp minimum target. */
public enum class BraceTabSize { Medium, Large }

/**
 * A token-styled tab strip and optional panel.
 *
 * Pass [selectedTabId] with [onTabSelected] for controlled state; the caller must save that ID.
 * Omit [selectedTabId] for saveable internal state starting at [initialSelectedTabId], or at the
 * first enabled tab. An unknown controlled ID deliberately leaves the strip and panel unselected.
 * Arrow keys move focus through enabled tabs without changing selection; Enter and Space activate
 * the focused tab. Horizontal arrows follow the physical direction in RTL. Touch, mouse, and
 * TalkBack selection use the same callback. The horizontal strip scrolls to keep focused and
 * selected tabs reachable at large text sizes. The vertical rail stacks above its panel on
 * narrow screens. [trailingContent] stays outside the horizontal scrolling viewport.
 *
 * [panel] is omitted for a strip whose content lives elsewhere; use [BraceTabPanel] in that case.
 * Panel content leaves composition when inactive and its `rememberSaveable` values are retained.
 */
@Composable
public fun BraceTabs(
    tabs: List<BraceTab>,
    modifier: Modifier = Modifier,
    selectedTabId: String? = null,
    initialSelectedTabId: String? = null,
    onTabSelected: ((String) -> Unit)? = null,
    orientation: BraceTabsOrientation = BraceTabsOrientation.Horizontal,
    size: BraceTabSize = BraceTabSize.Medium,
    trailingContent: (@Composable () -> Unit)? = null,
    panel: (@Composable (BraceTab) -> Unit)? = null,
) {
    require(tabs.all { it.id.isNotBlank() }) { "BraceTab IDs must not be blank" }
    require(tabs.map { it.id }.distinct().size == tabs.size) { "BraceTab IDs must be unique" }
    var internalId by rememberSaveable { mutableStateOf(initialSelectedTabId ?: tabs.firstOrNull { it.enabled }?.id) }
    val selectedId = if (selectedTabId != null) selectedTabId else {
        internalId?.takeIf { id -> tabs.any { it.id == id && it.enabled } }
            ?: tabs.firstOrNull { it.enabled }?.id
    }
    val selectedTab = tabs.firstOrNull { it.id == selectedId && it.enabled }
    LaunchedEffect(selectedId, selectedTabId) {
        if (selectedTabId == null && internalId != selectedId) internalId = selectedId
    }
    val tabIds = tabs.map { it.id }
    val requesters = remember(tabIds) { tabs.map { FocusRequester() } }
    val direction = LocalLayoutDirection.current
    val metrics = BraceTheme.componentMetrics.tabs
    val gap = BraceTheme.densityTokens.itemGapDp
    val rail = @Composable {
        tabs.forEachIndexed { index, tab ->
            val activate = {
                if (tab.enabled) {
                    if (selectedTabId == null) internalId = tab.id
                    onTabSelected?.invoke(tab.id)
                }
            }
            val focusNeighbor: (Int) -> Boolean = { offset ->
                val enabledIndices = tabs.indices.filter { tabs[it].enabled }
                val enabledPosition = enabledIndices.indexOf(index)
                if (enabledPosition < 0 || enabledIndices.isEmpty()) false else {
                    val next = enabledIndices[(enabledPosition + offset + enabledIndices.size) % enabledIndices.size]
                    requesters[next].requestFocus()
                    true
                }
            }
            key(tab.id) { BraceTabTitle(
                tab = tab,
                selected = tab.id == selectedId && tab.enabled,
                size = size,
                focusRequester = requesters[index],
                onActivate = activate,
                onArrow = { key ->
                    val offset = when (orientation) {
                        BraceTabsOrientation.Vertical -> when (key) {
                            Key.DirectionUp -> -1
                            Key.DirectionDown -> 1
                            else -> null
                        }
                        BraceTabsOrientation.Horizontal -> when (key) {
                            Key.DirectionLeft -> if (direction == LayoutDirection.Rtl) 1 else -1
                            Key.DirectionRight -> if (direction == LayoutDirection.Rtl) -1 else 1
                            else -> null
                        }
                    }
                    offset?.let(focusNeighbor) ?: false
                },
            ) }
        }
    }
    val rootModifier = modifier.background(BraceTheme.colors.components.tabs.container)
        .semantics { isTraversalGroup = true }
    when (orientation) {
        BraceTabsOrientation.Horizontal -> Column(rootModifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())
                        .semantics { isTraversalGroup = true },
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) { rail() }
                if (trailingContent != null) {
                    Spacer(Modifier.padding(start = gap))
                    trailingContent()
                }
            }
            if (panel != null && selectedTab != null) {
                BraceTabPanel(tab = selectedTab, selectedTabId = selectedId, modifier = Modifier.fillMaxWidth()) {
                    panel(selectedTab)
                }
            }
        }
        BraceTabsOrientation.Vertical -> BoxWithConstraints(rootModifier.fillMaxWidth()) {
            val stacked = maxWidth < metrics.verticalStackBreakpoint
            val canScrollRail = !stacked && constraints.hasBoundedHeight
            val tabRail: @Composable () -> Unit = {
                Column(
                    modifier = (if (stacked) Modifier.fillMaxWidth()
                        else Modifier.widthIn(max = metrics.verticalRailMaxWidth))
                        .then(if (canScrollRail) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                        .semantics { isTraversalGroup = true },
                    verticalArrangement = Arrangement.spacedBy(gap),
                ) {
                    rail()
                    trailingContent?.invoke()
                }
            }
            if (stacked) {
                Column {
                    tabRail()
                    if (panel != null && selectedTab != null) {
                        BraceTabPanel(tab = selectedTab, selectedTabId = selectedId,
                            modifier = Modifier.fillMaxWidth()) { panel(selectedTab) }
                    }
                }
            } else {
                Row {
                    tabRail()
                    if (panel != null && selectedTab != null) {
                        BraceTabPanel(tab = selectedTab, selectedTabId = selectedId,
                            modifier = Modifier.weight(1f)) { panel(selectedTab) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BraceTabTitle(
    tab: BraceTab,
    selected: Boolean,
    size: BraceTabSize,
    focusRequester: FocusRequester,
    onActivate: () -> Unit,
    onArrow: (Key) -> Boolean,
) {
    val colors = BraceTheme.colors.components.tabs
    val metrics = BraceTheme.componentMetrics.tabs
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(focused, selected) {
        if (focused || selected) bringIntoView.bringIntoView()
    }
    val background = when {
        !tab.enabled -> Color.Transparent
        pressed -> colors.pressedContainer
        selected -> colors.selectedContainer
        hovered -> colors.hoverContainer
        else -> Color.Transparent
    }
    val contentColor = when {
        !tab.enabled -> colors.disabledContent
        selected -> colors.selectedContent
        else -> colors.content
    }
    val shape = RoundedCornerShape(BraceTheme.shape.sm)
    val minHeight = when (size) {
        BraceTabSize.Medium -> metrics.mediumMinHeight
        BraceTabSize.Large -> metrics.largeMinHeight
    }.coerceAtLeast(BraceTheme.sizing.touchTarget)
    val spokenLabel = tab.accessibilityLabel ?: buildString {
        append(tab.label)
        if (tab.badge != null) append(", ${tab.badge}")
    }
    Column(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoView)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && onArrow(event.key)
            }
            .defaultMinSize(minHeight = minHeight, minWidth = BraceTheme.sizing.touchTarget)
            .background(background, shape)
            .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth, colors.focusRing, shape) else Modifier)
            .clearAndSetSemantics {
                contentDescription = spokenLabel
                text = AnnotatedString(tab.label)
                role = Role.Tab
                this.selected = selected
                if (!tab.enabled) disabled() else {
                    this.focused = focused
                    onClick { onActivate(); true }
                    requestFocus { focusRequester.requestFocus(); true }
                }
            }
            .selectable(
                selected = selected,
                enabled = tab.enabled,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
                onClick = onActivate,
            )
            .padding(horizontal = metrics.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.defaultMinSize(minHeight = (minHeight - metrics.indicatorHeight).coerceAtLeast(BraceTheme.spacing.none)),
            horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (tab.leadingIcon != null) {
                Box(Modifier.clearAndSetSemantics { }) { tab.leadingIcon.invoke() }
            }
            Text(tab.label, color = contentColor,
                style = if (size == BraceTabSize.Large) BraceTheme.typography.subtitle else BraceTheme.typography.bodyStrong,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (tab.badge != null) {
                Text(
                    tab.badge,
                    color = if (selected) colors.selectedBadgeContent else colors.badgeContent,
                    style = BraceTheme.typography.caption,
                    modifier = Modifier.background(
                        if (selected) colors.selectedBadgeContainer else colors.badgeContainer,
                        RoundedCornerShape(metrics.badgeCornerRadius),
                    ).padding(horizontal = BraceTheme.spacing.xs),
                )
            }
        }
        Box(
            Modifier.fillMaxWidth().height(metrics.indicatorHeight).background(
                if (selected) colors.indicator else Color.Transparent,
            ),
        )
    }
}

/**
 * The active panel for a [BraceTab]. This can be placed outside [BraceTabs] for controlled layouts.
 * Only selected content enters the accessibility tree and focus order. Saveable state survives
 * tab switches while this composable remains in composition.
 */
@Composable
public fun BraceTabPanel(
    tab: BraceTab,
    selectedTabId: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val holder = rememberSaveableStateHolder()
    if (tab.enabled && tab.id == selectedTabId) {
        holder.SaveableStateProvider(tab.id) {
            Column(
                modifier = modifier
                    .padding(top = BraceTheme.spacing.sm)
                    .semantics { paneTitle = tab.label; isTraversalGroup = true },
            ) { content() }
        }
    }
}

/** A silent Compose spacer for a custom horizontal tab row, matching Blueprint TabsExpander. */
@Composable
public fun RowScope.BraceTabSpacer() {
    Spacer(Modifier.weight(1f))
}

/** A silent Compose spacer for a custom vertical tab column, matching Blueprint TabsExpander. */
@Composable
public fun ColumnScope.BraceTabSpacer() {
    Spacer(Modifier.weight(1f))
}
