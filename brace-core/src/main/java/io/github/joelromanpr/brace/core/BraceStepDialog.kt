package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

/**
 * One named panel in a [BraceStepDialog]. [id] must be unique within its dialog.
 *
 * [canAdvance] is caller-owned validation state: set it to false while required fields are
 * incomplete or a request is loading. [validate] runs only when moving forward or completing;
 * return false and expose the error within [content] to keep the user on this step. The dialog
 * retains each panel's `rememberSaveable` state across step changes. Keep business data in
 * caller-owned saveable state when it must outlive the dialog itself.
 */
public class BraceDialogStep(
    public val id: String,
    public val title: String,
    public val description: String? = null,
    public val canAdvance: Boolean = true,
    public val validate: () -> Boolean = { true },
    public val content: @Composable ColumnScope.() -> Unit,
) {
    init {
        require(id.isNotBlank()) { "BraceDialogStep id must not be blank" }
        require(title.isNotBlank()) { "BraceDialogStep title must not be blank" }
    }
}

/** Placement of the step navigation. [Auto] uses a top rail on narrow screens. */
public enum class BraceStepNavigation { Auto, Top, Start, End }

/** Supply app-localized labels, including a positional phrase such as "Step 1 of 3". */
public class BraceStepDialogLabels(
    public val steps: String = "Steps",
    public val back: String = "Back",
    public val next: String = "Next",
    public val complete: String = "Complete",
    public val close: String = "Close dialog",
    public val current: String = "Current step",
    public val available: String = "Available step",
    public val upcoming: String = "Upcoming step",
    public val position: (Int, Int) -> String = { index, count -> "Step $index of $count" },
)

/**
 * A controlled, sequential Compose dialog for multi-part forms and setup flows.
 *
 * The caller owns [open] and [selectedStepId], applies [onStepChange] to its state, and closes
 * the dialog in [onDismissRequest]. A step may only advance when [BraceDialogStep.canAdvance] is
 * true and its validator accepts the transition. Previously visited steps can be selected; a
 * return to a later visited step still validates the current step. [onComplete] is called only
 * after validating the final step, and does not dismiss automatically.
 *
 * The underlying [BraceDialog] provides the modal Android window, TalkBack pane, Back and
 * outside-touch handling. Escape follows [dismissOnBackPress]. The active panel heading takes
 * keyboard focus on step changes; the caller can attach [focusReturnRequester] to the launcher
 * to restore focus when [open] becomes false. Button labels and spoken status are provided by
 * [labels] so applications can localize them. The step rail uses 48 dp touch targets.
 *
 * [resetVisitedOnOpen] resets rail history to the selected step when reopening. The caller must
 * reset [selectedStepId] itself if it wants to restart at the first step.
 */
@Composable
public fun BraceStepDialog(
    open: Boolean,
    selectedStepId: String,
    onStepChange: (newStepId: String, previousStepId: String) -> Unit,
    onDismissRequest: () -> Unit,
    onComplete: () -> Unit,
    steps: List<BraceDialogStep>,
    title: String,
    modifier: Modifier = Modifier,
    labels: BraceStepDialogLabels = BraceStepDialogLabels(),
    navigation: BraceStepNavigation = BraceStepNavigation.Auto,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = false,
    resetVisitedOnOpen: Boolean = true,
    focusReturnRequester: FocusRequester? = null,
) {
    val currentIndex = requireStepDialogConfiguration(title, selectedStepId, steps)

    val holder = rememberSaveableStateHolder()
    var highestVisited by rememberSaveable { mutableIntStateOf(currentIndex) }
    var wasOpen by remember { mutableStateOf(false) }
    val headingFocus = remember { FocusRequester() }
    val current = steps[currentIndex]

    LaunchedEffect(open, selectedStepId, resetVisitedOnOpen) {
        if (open && !wasOpen && resetVisitedOnOpen) highestVisited = currentIndex
        if (open) highestVisited = maxOf(highestVisited, currentIndex)
        if (!open && wasOpen) focusReturnRequester?.requestFocus()
        wasOpen = open
    }
    fun changeTo(index: Int) {
        if (index == currentIndex || index !in steps.indices) return
        if (index > currentIndex && (!current.canAdvance || !current.validate())) return
        onStepChange(steps[index].id, current.id)
    }

    BraceDialog(
        open = open,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = title,
        closeContentDescription = labels.close,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        actions = {
            if (currentIndex > 0) {
                BraceButton(
                    label = labels.back,
                    onClick = { changeTo(currentIndex - 1) },
                    intent = BraceButtonIntent.Secondary,
                )
            }
            if (currentIndex == steps.lastIndex) {
                BraceButton(
                    label = labels.complete,
                    onClick = { if (current.canAdvance && current.validate()) onComplete() },
                    enabled = current.canAdvance,
                )
            } else {
                BraceButton(
                    label = labels.next,
                    onClick = { changeTo(currentIndex + 1) },
                    enabled = current.canAdvance,
                )
            }
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && dismissOnBackPress) {
                        onDismissRequest()
                        true
                    } else false
                },
        ) {
            val sideRail = maxWidth >= 480.dp && navigation != BraceStepNavigation.Top
            val rail: @Composable () -> Unit = {
                StepNavigation(
                    steps = steps,
                    currentIndex = currentIndex,
                    highestVisited = highestVisited,
                    labels = labels,
                    vertical = sideRail,
                    onStepClick = ::changeTo,
                )
            }
            val panel: @Composable () -> Unit = {
                Column(
                    modifier = Modifier.fillMaxWidth().semantics { isTraversalGroup = true },
                    verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
                ) {
                    LaunchedEffect(selectedStepId) { headingFocus.requestFocus() }
                    Text(
                        text = current.title,
                        modifier = Modifier
                            .focusRequester(headingFocus)
                            .focusable()
                            .semantics {
                                heading()
                                liveRegion = LiveRegionMode.Polite
                            },
                        color = BraceTheme.colors.components.dialog.content,
                        style = BraceTheme.typography.subtitle,
                    )
                    Text(
                        text = labels.position(currentIndex + 1, steps.size),
                        color = BraceTheme.colors.semantic.onSurfaceMuted,
                        style = BraceTheme.typography.caption,
                    )
                    if (current.description != null) {
                        Text(
                            text = current.description,
                            color = BraceTheme.colors.semantic.onSurfaceMuted,
                            style = BraceTheme.typography.body,
                        )
                    }
                    val panelScope = this
                    holder.SaveableStateProvider(current.id) { current.content(panelScope) }
                }
            }
            if (sideRail) {
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                    if (navigation != BraceStepNavigation.End) rail()
                    Column(Modifier.weight(1f)) { panel() }
                    if (navigation == BraceStepNavigation.End) rail()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                    rail()
                    panel()
                }
            }
        }
    }
}

@Composable
private fun StepNavigation(
    steps: List<BraceDialogStep>,
    currentIndex: Int,
    highestVisited: Int,
    labels: BraceStepDialogLabels,
    vertical: Boolean,
    onStepClick: (Int) -> Unit,
) {
    val gap = BraceTheme.densityTokens.itemGapDp
    if (vertical) {
        Column(
            modifier = Modifier.widthIn(max = 176.dp).semantics { isTraversalGroup = true },
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Text(labels.steps, color = BraceTheme.colors.semantic.onSurfaceMuted,
                style = BraceTheme.typography.label, modifier = Modifier.semantics { heading() })
            steps.forEachIndexed { index, step ->
                StepNavigationItem(step, index, steps.size, currentIndex, highestVisited, labels,
                    modifier = Modifier.fillMaxWidth(), onClick = { onStepClick(index) })
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Text(labels.steps, color = BraceTheme.colors.semantic.onSurfaceMuted,
                style = BraceTheme.typography.label, modifier = Modifier.semantics { heading() })
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .semantics { isTraversalGroup = true },
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                steps.forEachIndexed { index, step ->
                    StepNavigationItem(step, index, steps.size, currentIndex, highestVisited, labels,
                        modifier = Modifier.widthIn(min = BraceTheme.sizing.touchTarget * 2),
                        onClick = { onStepClick(index) })
                }
            }
        }
    }
}

@Composable
private fun StepNavigationItem(
    step: BraceDialogStep,
    index: Int,
    count: Int,
    currentIndex: Int,
    highestVisited: Int,
    labels: BraceStepDialogLabels,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val current = index == currentIndex
    val available = index <= highestVisited
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val semantic = BraceTheme.colors.semantic
    val shape = RoundedCornerShape(BraceTheme.componentMetrics.dialog.cornerRadius)
    val background = if (current) semantic.primarySubtle else semantic.surfaceRaised
    val foreground = when {
        current -> semantic.onPrimarySubtle
        available -> semantic.onSurface
        else -> semantic.disabledContent
    }
    Row(
        modifier = modifier
            .clickable(enabled = available, role = Role.Tab, interactionSource = interaction,
                indication = null, onClick = onClick)
            .semantics {
                contentDescription = "${labels.position(index + 1, count)}, ${step.title}"
                selected = current
                stateDescription = when {
                    current -> labels.current
                    available -> labels.available
                    else -> labels.upcoming
                }
            }
            .background(background, shape)
            .border(
                if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                if (focused) semantic.focusRing else if (current) semantic.primary else semantic.border,
                shape,
            )
            .heightIn(min = BraceTheme.sizing.touchTarget)
            .padding(horizontal = BraceTheme.spacing.sm, vertical = BraceTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
    ) {
        Text("${index + 1}.", color = foreground, style = BraceTheme.typography.label)
        Text(step.title, color = foreground, style = BraceTheme.typography.label)
    }
}

internal fun requireStepDialogConfiguration(
    title: String,
    selectedStepId: String,
    steps: List<BraceDialogStep>,
): Int {
    require(title.isNotBlank()) { "BraceStepDialog title must not be blank" }
    require(steps.isNotEmpty()) { "BraceStepDialog needs at least one step" }
    require(steps.map { it.id }.distinct().size == steps.size) { "BraceDialogStep ids must be unique" }
    val currentIndex = steps.indexOfFirst { it.id == selectedStepId }
    require(currentIndex >= 0) { "selectedStepId must match a BraceDialogStep id" }
    return currentIndex
}
