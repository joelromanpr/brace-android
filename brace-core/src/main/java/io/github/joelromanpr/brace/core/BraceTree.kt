package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag as semanticsTestTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/**
 * One stable-key item in [BraceTree]. Child keys must be unique across the entire tree.
 *
 * [hasChildren] permits a branch whose children will be loaded after expansion. [leadingContent]
 * is decorative; [label] and [secondaryLabel] provide the complete accessible name. Use the
 * model's [key] for selection and expansion instead of Blueprint's positional DOM node path.
 */
public data class BraceTreeNode(
    val key: String,
    val label: String,
    val children: List<BraceTreeNode> = emptyList(),
    val secondaryLabel: String? = null,
    val enabled: Boolean = true,
    val hasChildren: Boolean = children.isNotEmpty(),
    val leadingContent: (@Composable () -> Unit)? = null,
)

/** Saveable selection and expansion holder for a [BraceTree] controlled by its caller. */
@Stable
public class BraceTreeState internal constructor(
    expanded: Set<String>,
    selected: Set<String>,
) {
    public var expandedKeys: Set<String> by mutableStateOf(expanded)
    public var selectedKeys: Set<String> by mutableStateOf(selected)

    public companion object {
        /** Restores stable keys when the host Activity or Compose state is recreated. */
        public val Saver: Saver<BraceTreeState, ArrayList<String>> = Saver(
            save = { state ->
                arrayListOf(state.expandedKeys.size.toString()).apply {
                    addAll(state.expandedKeys)
                    addAll(state.selectedKeys)
                }
            },
            restore = { saved ->
                val expandedCount = saved.first().toInt()
                BraceTreeState(saved.drop(1).take(expandedCount).toSet(), saved.drop(expandedCount + 1).toSet())
            },
        )
    }
}

/** Remember expansion and selection across configuration changes and process recreation. */
@Composable
public fun rememberBraceTreeState(
    initialExpandedKeys: Set<String> = emptySet(),
    initialSelectedKeys: Set<String> = emptySet(),
): BraceTreeState = rememberSaveable(saver = BraceTreeState.Saver) {
    BraceTreeState(initialExpandedKeys, initialSelectedKeys)
}

private data class VisibleTreeNode(
    val node: BraceTreeNode,
    val depth: Int,
    val parentKey: String?,
    val siblingIndex: Int,
    val siblingCount: Int,
)

private fun flattenTree(nodes: List<BraceTreeNode>, expandedKeys: Set<String>): List<VisibleTreeNode> {
    val keys = HashSet<String>()
    val visible = ArrayList<VisibleTreeNode>()
    fun visit(siblings: List<BraceTreeNode>, depth: Int, parentKey: String?, ancestorVisible: Boolean) {
        siblings.forEachIndexed { index, node ->
            require(node.key.isNotBlank()) { "Tree node keys must not be blank" }
            require(keys.add(node.key)) { "Duplicate tree node key: ${node.key}" }
            require(node.label.isNotBlank()) { "Tree node labels must not be blank" }
            if (ancestorVisible) visible += VisibleTreeNode(node, depth, parentKey, index, siblings.size)
            visit(node.children, depth + 1, node.key, ancestorVisible && node.key in expandedKeys)
        }
    }
    visit(nodes, 0, null, true)
    return visible
}

/**
 * A controlled, multi-rooted Compose tree for dense navigation and object hierarchies.
 *
 * [expandedKeys] and [selectedKeys] are never changed internally: callbacks propose the next
 * sets. Use [rememberBraceTreeState] in a screen that needs a saveable local controller.
 * [multiSelect] makes activation toggle a key; single selection replaces the set. The active
 * keyboard row is independent of selection and follows Up/Down/Home/End and logical
 * expand/collapse arrows (reversed in RTL). Enter or Space activates it; Tab leaves the tree.
 *
 * One Compose semantics node per visible row announces label, level, sibling position,
 * selection, expansion and a custom expand/collapse action. Disabled rows expose no actions.
 * Rows stay at least 48 dp high in either density. [maxHeight] bounds the lazy viewport and
 * must allow one touch target. Large text may wrap, and deep indentation is capped to keep text visible on narrow screens.
 */
@Composable
public fun BraceTree(
    nodes: List<BraceTreeNode>,
    expandedKeys: Set<String>,
    onExpandedKeysChange: (Set<String>) -> Unit,
    selectedKeys: Set<String>,
    onSelectedKeysChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    multiSelect: Boolean = false,
    maxHeight: Dp = 320.dp,
) {
    require(maxHeight >= BraceTheme.sizing.touchTarget) {
        "maxHeight must fit at least one accessible tree row"
    }
    val visible = remember(nodes, expandedKeys) { flattenTree(nodes, expandedKeys) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusInteraction = remember { MutableInteractionSource() }
    val focused by focusInteraction.collectIsFocusedAsState()
    var activeKey by rememberSaveable { mutableStateOf<String?>(null) }
    val enabledRows = visible.filter { it.node.enabled }
    val activeIndex = visible.indexOfFirst { it.node.key == activeKey && it.node.enabled }
    val resolvedActiveIndex = if (activeIndex >= 0) activeIndex else visible.indexOfFirst { it.node.enabled }
    val resolvedActiveKey = visible.getOrNull(resolvedActiveIndex)?.node?.key
    LaunchedEffect(resolvedActiveKey, activeKey) {
        if (activeKey != resolvedActiveKey) activeKey = resolvedActiveKey
    }
    LaunchedEffect(resolvedActiveIndex, focused) {
        if (focused && resolvedActiveIndex >= 0) listState.scrollToItem(resolvedActiveIndex)
    }

    fun select(key: String) {
        val next = if (multiSelect) {
            if (key in selectedKeys) selectedKeys - key else selectedKeys + key
        } else setOf(key)
        onSelectedKeysChange(next)
    }
    fun toggle(key: String) {
        onExpandedKeysChange(if (key in expandedKeys) expandedKeys - key else expandedKeys + key)
    }
    fun moveTo(index: Int) {
        val candidate = visible.getOrNull(index)
        if (candidate?.node?.enabled == true) activeKey = candidate.node.key
    }
    fun moveBy(delta: Int) {
        if (enabledRows.isEmpty()) return
        val current = enabledRows.indexOfFirst { it.node.key == resolvedActiveKey }
        val target = (current + delta).coerceIn(0, enabledRows.lastIndex)
        activeKey = enabledRows[target].node.key
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val expandKey = if (rtl) Key.DirectionLeft else Key.DirectionRight
    val collapseKey = if (rtl) Key.DirectionRight else Key.DirectionLeft
    val colors = BraceTheme.colors.components.tree
    val metrics = BraceTheme.componentMetrics.tree
    val treeShape = RoundedCornerShape(metrics.cornerRadius)
    val expandLabel = stringResource(R.string.brace_tree_expand)
    val collapseLabel = stringResource(R.string.brace_tree_collapse)
    val selectLabel = stringResource(R.string.brace_tree_select)
    val expandedDescription = stringResource(R.string.brace_tree_expanded)
    val collapsedDescription = stringResource(R.string.brace_tree_collapsed)
    val nodeDescription = stringResource(R.string.brace_tree_node_position)

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val indentCap = (maxWidth - BraceTheme.sizing.touchTarget * 2).coerceAtLeast(0.dp)
        val density = LocalDensity.current
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val current = visible.getOrNull(resolvedActiveIndex) ?: return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> { moveBy(-1); true }
                        Key.DirectionDown -> { moveBy(1); true }
                        Key.MoveHome -> { moveTo(visible.indexOfFirst { it.node.enabled }); true }
                        Key.MoveEnd -> { moveTo(visible.indexOfLast { it.node.enabled }); true }
                        expandKey -> {
                            if (current.node.hasChildren || current.node.children.isNotEmpty()) {
                                if (current.node.key !in expandedKeys) toggle(current.node.key)
                                else visible.drop(resolvedActiveIndex + 1)
                                    .takeWhile { it.depth > current.depth }
                                    .firstOrNull { it.parentKey == current.node.key && it.node.enabled }
                                    ?.let { activeKey = it.node.key }
                            }
                            true
                        }
                        collapseKey -> {
                            if ((current.node.hasChildren || current.node.children.isNotEmpty()) && current.node.key in expandedKeys) {
                                toggle(current.node.key)
                            } else current.parentKey?.let { parent ->
                                visible.firstOrNull { it.node.key == parent && it.node.enabled }
                                    ?.let { activeKey = it.node.key }
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter, Key.Spacebar -> { select(current.node.key); true }
                        else -> false
                    }
                }
                .focusable(interactionSource = focusInteraction)
                .semantics {
                    isTraversalGroup = true
                    contentDescription = label
                    collectionInfo = CollectionInfo(visible.size, 1)
                }
                .testTag("brace-tree"),
        ) {
            itemsIndexed(visible, key = { _, entry -> entry.node.key }) { index, entry ->
                val node = entry.node
                val branch = node.hasChildren || node.children.isNotEmpty()
                val expanded = node.key in expandedKeys
                val selectedNow = node.key in selectedKeys
                val active = focused && node.key == resolvedActiveKey
                val hoverSource = remember(node.key) { MutableInteractionSource() }
                val hovered by hoverSource.collectIsHoveredAsState()
                val background = when {
                    selectedNow -> colors.selectedContainer
                    hovered && node.enabled -> colors.hoverContainer
                    else -> colors.container
                }
                val contentColor = when {
                    !node.enabled -> colors.disabledContent
                    selectedNow -> colors.selectedContent
                    else -> colors.content
                }
                val indent = (metrics.indent * entry.depth).coerceAtMost(indentCap)
                val caretPx = with(density) { BraceTheme.sizing.touchTarget.toPx() }
                val indentPx = with(density) { indent.toPx() }
                val spoken = nodeDescription.format(
                    if (node.secondaryLabel == null) node.label else "${node.label}, ${node.secondaryLabel}",
                    entry.depth + 1, entry.siblingIndex + 1, entry.siblingCount,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                        .background(background, treeShape)
                        .then(if (active) Modifier.border(BraceTheme.sizing.focusRingWidth, colors.focusRing, treeShape) else Modifier)
                        .hoverable(hoverSource)
                        .pointerInput(node.key, node.enabled, expandedKeys, selectedKeys, multiSelect, indentPx, caretPx, rtl) {
                            detectTapGestures { offset ->
                                if (!node.enabled) return@detectTapGestures
                                activeKey = node.key
                                focusRequester.requestFocus()
                                val caretStart = if (rtl) size.width - indentPx - caretPx else indentPx
                                if (branch && offset.x >= caretStart && offset.x < caretStart + caretPx) toggle(node.key)
                                else select(node.key)
                            }
                        }
                        .clearAndSetSemantics {
                            semanticsTestTag = "brace-tree-node-${node.key}"
                            contentDescription = spoken
                            role = Role.Button
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                            selected = selectedNow
                            if (branch) stateDescription = if (expanded) expandedDescription else collapsedDescription
                            if (!node.enabled) disabled()
                            else {
                                onClick(label = selectLabel) {
                                    activeKey = node.key
                                    select(node.key)
                                    true
                                }
                                if (branch) customActions = listOf(CustomAccessibilityAction(
                                    if (expanded) collapseLabel else expandLabel,
                                ) {
                                    activeKey = node.key
                                    toggle(node.key)
                                    true
                                })
                            }
                        }
                        .padding(start = indent, end = metrics.horizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(BraceTheme.sizing.touchTarget), contentAlignment = Alignment.Center) {
                        if (branch) Text(
                            if (expanded) "⌄" else if (rtl) "‹" else "›",
                            color = if (node.enabled) colors.caret else colors.disabledContent,
                            style = BraceTheme.typography.subtitle,
                        )
                    }
                    if (node.leadingContent != null) {
                        Box(Modifier.padding(end = BraceTheme.spacing.xs)) { node.leadingContent() }
                    }
                    Column(Modifier.weight(1f).padding(vertical = if (BraceTheme.density == BraceDensity.Compact) BraceTheme.spacing.xxs else BraceTheme.spacing.xs)) {
                        Text(node.label, color = contentColor, style = BraceTheme.typography.body,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (node.secondaryLabel != null) Text(node.secondaryLabel,
                            color = if (node.enabled) colors.mutedContent else colors.disabledContent,
                            style = BraceTheme.typography.caption, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
