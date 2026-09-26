package io.github.joelromanpr.brace.core

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One stack entry. [id] identifies this panel instance and must be unique within the stack. */
public data class BracePanel(
    val id: String,
    val title: String,
) {
    init {
        require(id.isNotBlank()) { "A panel ID must not be blank" }
        require(title.isNotBlank()) { "A panel title must not be blank" }
    }
}

/**
 * A saveable stack whose first panel cannot be closed. Use [rememberBracePanelStackState] to
 * retain the stack across Activity recreation. Open a new [BracePanel] with a distinct [BracePanel.id].
 */
@Stable
public class BracePanelStackState internal constructor(initialStack: List<BracePanel>) {
    private val panels = mutableStateListOf<BracePanel>().apply { addAll(initialStack) }

    init {
        require(initialStack.isNotEmpty()) { "A panel stack requires a root panel" }
        require(initialStack.map { it.id }.distinct().size == initialStack.size) {
            "Panel IDs must be unique within the stack"
        }
    }

    /** The root-first stack. The last entry is active. */
    public val stack: List<BracePanel> get() = panels.toList()

    /** The currently visible panel. */
    public val activePanel: BracePanel get() = panels.last()

    /** Adds [panel] above the current panel. Its ID must be new to this stack. */
    public fun openPanel(panel: BracePanel) {
        require(panels.none { it.id == panel.id }) { "Panel ID already exists: ${panel.id}" }
        panels.add(panel)
    }

    /** Closes the top panel and returns it, or returns null at the unclosable root. */
    public fun closePanel(): BracePanel? = if (panels.size <= 1) null else panels.removeAt(panels.lastIndex)
}

private val PanelStackStateSaver = listSaver<BracePanelStackState, String>(
    save = { state -> state.stack.flatMap { listOf(it.id, it.title) } },
    restore = { values ->
        require(values.size >= 2 && values.size % 2 == 0) { "Invalid saved panel stack" }
        BracePanelStackState(values.chunked(2).map { BracePanel(it[0], it[1]) })
    },
)

/**
 * Creates an internally managed stack rooted at [initialPanel]. The stack itself survives Activity
 * recreation. Changing the root ID creates a new stack. A panel's own state survives covering and
 * uncovering when [BracePanelStack] uses its default `preservePanelState = true`.
 */
@Composable
public fun rememberBracePanelStackState(initialPanel: BracePanel): BracePanelStackState =
    rememberSaveable(initialPanel.id, saver = PanelStackStateSaver) {
        BracePanelStackState(listOf(initialPanel))
    }

/** Actions and destination metadata available while rendering a panel. */
public interface BracePanelScope {
    /** The panel represented by this content. */
    public val panel: BracePanel

    /** The destination beneath [panel], if one exists. */
    public val previousPanel: BracePanel?

    /** Requests a new panel at the top of the stack. */
    public fun openPanel(panel: BracePanel)

    /** Requests a pop. This does nothing at the root. */
    public fun closePanel()
}

private class PanelScopeImpl(
    override val panel: BracePanel,
    override val previousPanel: BracePanel?,
    private val isActive: Boolean,
    private val onOpen: (BracePanel) -> Unit,
    private val onClose: () -> Unit,
) : BracePanelScope {
    override fun openPanel(panel: BracePanel) { if (isActive) onOpen(panel) }
    override fun closePanel() { if (isActive) onClose() }
}

/**
 * An internally managed panel stack. [content] renders the active destination and receives
 * [BracePanelScope.openPanel] and [BracePanelScope.closePanel] actions. The root cannot pop.
 * [onOpen] and [onClose] observe completed stack changes.
 *
 * The host should supply a bounded height for scrolling panel content. Only the active panel is
 * accessible after navigation. [preservePanelState] retains `rememberSaveable` content state while a
 * panel is covered; ordinary `remember` state follows Compose's composition lifecycle.
 * Android Back, Escape, and the labeled header action all pop the same destination. Focus moves
 * to the header action or title; when the header is hidden, it moves to the first focusable child.
 * Directional transitions mirror in RTL and become instantaneous when Brace motion is reduced.
 */
@Composable
public fun BracePanelStack(
    state: BracePanelStackState,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    preservePanelState: Boolean = true,
    onOpen: ((BracePanel) -> Unit)? = null,
    onClose: ((BracePanel) -> Unit)? = null,
    content: @Composable BracePanelScope.() -> Unit,
) {
    BracePanelStackContent(
        stack = state.stack,
        modifier = modifier,
        showHeader = showHeader,
        preservePanelState = preservePanelState,
        onOpenPanel = { panel -> state.openPanel(panel); onOpen?.invoke(panel) },
        onClosePanel = { panel -> state.closePanel()?.let { onClose?.invoke(it) } },
        content = content,
    )
}

/**
 * A controlled panel stack. [stack] is root-first and its last panel is active. Callers change
 * [stack] in response to [onOpenPanel] and [onClosePanel]; the component does not mutate it.
 * Supply stable, unique panel IDs and save the caller-owned stack when restoration is required.
 * An empty stack renders nothing; a single root entry cannot be closed.
 */
@Composable
public fun BracePanelStack(
    stack: List<BracePanel>,
    onOpenPanel: (BracePanel) -> Unit,
    onClosePanel: (BracePanel) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    preservePanelState: Boolean = true,
    content: @Composable BracePanelScope.() -> Unit,
) {
    BracePanelStackContent(
        stack = stack,
        modifier = modifier,
        showHeader = showHeader,
        preservePanelState = preservePanelState,
        onOpenPanel = onOpenPanel,
        onClosePanel = onClosePanel,
        content = content,
    )
}

private data class PanelTransitionTarget(
    val panel: BracePanel,
    val previous: BracePanel?,
    val depth: Int,
)

@Composable
private fun BracePanelStackContent(
    stack: List<BracePanel>,
    modifier: Modifier,
    showHeader: Boolean,
    preservePanelState: Boolean,
    onOpenPanel: (BracePanel) -> Unit,
    onClosePanel: (BracePanel) -> Unit,
    content: @Composable BracePanelScope.() -> Unit,
) {
    require(stack.map { it.id }.distinct().size == stack.size) { "Panel IDs must be unique within the stack" }
    if (stack.isEmpty()) return
    val active = stack.last()
    val previous = stack.getOrNull(stack.lastIndex - 1)
    val activeIds = stack.map { it.id }
    val saveableStateHolder = rememberSaveableStateHolder()
    var previousIds by remember { mutableStateOf(activeIds) }
    val currentIds by rememberUpdatedState(activeIds)
    val cleanupScope = rememberCoroutineScope()
    val motionDuration = BraceTheme.motionTokens.normal
    SideEffect {
        val removed = previousIds - activeIds.toSet()
        previousIds = activeIds
        removed.forEach { id ->
            cleanupScope.launch {
                delay(motionDuration.toLong() + 16L)
                if (id !in currentIds) saveableStateHolder.removeState(id)
            }
        }
    }
    val requestFocus = remember(active.id) { FocusRequester() }
    var focusedTarget by remember { mutableStateOf(active.id to showHeader) }
    LaunchedEffect(active.id, showHeader) {
        val targetKey = active.id to showHeader
        if (focusedTarget != targetKey) {
            withFrameNanos { }
            requestFocus.requestFocus()
            focusedTarget = targetKey
        }
    }
    val canClose = stack.size > 1
    val closeActive = { if (canClose) onClosePanel(active) }
    val openPanel: (BracePanel) -> Unit = { panel ->
        require(panel.id !in activeIds) { "Panel ID already exists: ${panel.id}" }
        onOpenPanel(panel)
    }
    BackHandler(enabled = canClose, onBack = closeActive)
    val colors = BraceTheme.colors.components.panelStack
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val target = PanelTransitionTarget(active, previous, stack.size)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(colors.container)
            .border(BraceTheme.sizing.borderWidth, colors.border)
            .onKeyEvent { event ->
                if (canClose && event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    closeActive()
                    true
                } else false
            }
            .semantics { isTraversalGroup = true },
    ) {
        AnimatedContent(
            targetState = target,
            modifier = Modifier.fillMaxWidth(),
            contentKey = { it.panel.id },
            transitionSpec = {
                if (motionDuration <= 0) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val logicalSign = if (isRtl) -1 else 1
                    val push = targetState.depth >= initialState.depth
                    val enterSign = if (push) logicalSign else -logicalSign
                    val exitSign = -enterSign
                    slideInHorizontally(
                        animationSpec = tween(motionDuration),
                        initialOffsetX = { enterSign * it },
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(motionDuration),
                        targetOffsetX = { exitSign * it / 2 },
                    )
                }
            },
            label = "Brace panel navigation",
        ) { destination ->
            val isActive = destination.panel.id == active.id
            val panelScope = PanelScopeImpl(
                panel = destination.panel,
                previousPanel = destination.previous,
                isActive = isActive,
                onOpen = openPanel,
                onClose = closeActive,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (isActive) Modifier.semantics { paneTitle = destination.panel.title }
                        else Modifier.clearAndSetSemantics { }.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                }
                            }
                        }),
            ) {
                if (showHeader) {
                    PanelHeader(
                        panel = destination.panel,
                        previous = destination.previous,
                        active = isActive,
                        focusRequester = if (isActive) requestFocus else null,
                        onBack = closeActive,
                    )
                }
                val panelModifier = Modifier
                    .fillMaxWidth()
                    .then(if (isActive && !showHeader) Modifier
                        .focusRequester(requestFocus)
                        .focusGroup()
                    else Modifier)
                Box(panelModifier) {
                    if (preservePanelState) {
                        saveableStateHolder.SaveableStateProvider(destination.panel.id) {
                            panelScope.content()
                        }
                    } else panelScope.content()
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(
    panel: BracePanel,
    previous: BracePanel?,
    active: Boolean,
    focusRequester: FocusRequester?,
    onBack: () -> Unit,
) {
    val colors = BraceTheme.colors.components.panelStack
    val metrics = BraceTheme.componentMetrics.panelStack
    val compact = BraceTheme.density == BraceDensity.Compact
    val horizontalPadding = if (compact) BraceTheme.spacing.md else metrics.horizontalPadding
    val verticalPadding = if (compact) BraceTheme.spacing.none else metrics.verticalPadding
    var titleFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget.coerceAtLeast(metrics.minHeight))
            .background(colors.header)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(metrics.itemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (previous != null) {
            PanelBackButton(
                previous = previous,
                modifier = Modifier.widthIn(max = metrics.backMaxWidth),
                focusRequester = focusRequester,
                onBack = onBack,
            )
        }
        val titleModifier = Modifier
            .weight(1f)
            .then(if (active && previous == null && focusRequester != null) {
                Modifier.focusRequester(focusRequester)
                    .onFocusChanged { titleFocused = it.isFocused }
                    .focusable()
            } else Modifier)
            .then(if (titleFocused) Modifier.border(
                BraceTheme.sizing.focusRingWidth, colors.focusRing,
            ) else Modifier)
            .semantics { heading() }
        Text(
            text = panel.title,
            modifier = titleModifier,
            color = colors.title,
            style = BraceTheme.typography.subtitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    Spacer(
        Modifier.fillMaxWidth().height(BraceTheme.sizing.borderWidth)
            .background(colors.border).clearAndSetSemantics { },
    )
}

@Composable
private fun PanelBackButton(
    previous: BracePanel,
    modifier: Modifier,
    focusRequester: FocusRequester?,
    onBack: () -> Unit,
) {
    val colors = BraceTheme.colors.components.panelStack
    val metrics = BraceTheme.componentMetrics.panelStack
    val label = stringResource(R.string.brace_panel_stack_back_to, previous.title)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(BraceTheme.shape.sm)
    val container = when {
        pressed -> colors.backPressed
        hovered -> colors.backHover
        else -> colors.header
    }
    Row(
        modifier = Modifier
            .defaultMinSize(minWidth = BraceTheme.sizing.touchTarget, minHeight = BraceTheme.sizing.touchTarget)
            .then(modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .background(container, shape)
            .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth, colors.focusRing, shape) else Modifier)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick(label) { onBack(); true }
                if (focusRequester != null) requestFocus { focusRequester.requestFocus() }
            }
            .clickable(
                role = Role.Button,
                onClickLabel = label,
                interactionSource = interaction,
                indication = null,
                onClick = onBack,
            )
            .padding(horizontal = BraceTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(metrics.itemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val iconSize = BraceTheme.sizing.iconSm
        val strokeWidth = BraceTheme.sizing.borderStrongWidth
        Canvas(Modifier.size(iconSize).clearAndSetSemantics { }) {
            val x = size.width * if (isRtl) 0.62f else 0.38f
            val farX = size.width * if (isRtl) 0.34f else 0.66f
            val stroke = strokeWidth.toPx()
            drawLine(colors.backContent, Offset(farX, size.height * 0.22f), Offset(x, size.height * 0.5f), stroke, cap = StrokeCap.Round)
            drawLine(colors.backContent, Offset(x, size.height * 0.5f), Offset(farX, size.height * 0.78f), stroke, cap = StrokeCap.Round)
        }
        Text(
            text = previous.title,
            color = colors.backContent,
            style = BraceTheme.typography.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
