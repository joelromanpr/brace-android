package io.github.joelromanpr.brace.datetime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.braceShortcutEditable
import java.time.Clock
import java.time.LocalDate
import java.util.Locale

/**
 * Two localized date fields and a shared [BraceDateRangePicker] in a Compose popover.
 *
 * [value] is controlled by the caller, which should save its endpoints as ISO date strings.
 * Valid drafts emit immediately; invalid dates, unavailable endpoints, and a start later than
 * the end remain visible with an announced error. [onInvalidInput] fires when an invalid draft
 * loses focus or the IME Done action runs. The two drafts, active endpoint, and popup state are
 * saved over activity recreation. Alt+Down on either field opens the calendar to edit that
 * endpoint; the explicit calendar button starts the normal two-click range selection.
 * The popup closes after a complete range is selected when [closeOnCompleteSelection] is true.
 */
@Composable
public fun BraceDateRangeField(
    value: BraceDateRange,
    onValueChange: (BraceDateRange) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isDateEnabled: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
    readOnly: Boolean = false,
    allowSingleDayRange: Boolean = false,
    shortcuts: List<BraceDateRangeShortcut> = emptyList(),
    showClear: Boolean = true,
    closeOnCompleteSelection: Boolean = true,
    validationError: String? = null,
    onInvalidInput: (BraceRangeBoundary, String) -> Unit = { _, _ -> },
    clock: Clock = Clock.systemDefaultZone(),
) {
    require(minDate == null || maxDate == null || !minDate.isAfter(maxDate)) {
        "minDate must be on or before maxDate"
    }
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val formatter = remember(resolvedLocale) { localizedDateFormatter(resolvedLocale) }
    val labels = rangeLabels(resolvedLocale)
    val colors = BraceTheme.colors.components.dateRangeInput
    val metrics = BraceTheme.componentMetrics.dateRangeInput
    var startDraft by rememberSaveable { mutableStateOf(value.start?.let(formatter::format).orEmpty()) }
    var endDraft by rememberSaveable { mutableStateOf(value.end?.let(formatter::format).orEmpty()) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var activeBoundaryName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeBoundary = activeBoundaryName?.let(BraceRangeBoundary::valueOf)
    var startEditing by remember { mutableStateOf(false) }
    var endEditing by remember { mutableStateOf(false) }
    var lastStart by remember { mutableStateOf(value.start) }
    var lastEnd by remember { mutableStateOf(value.end) }
    var lastLocale by remember { mutableStateOf(resolvedLocale) }
    var lastInvalid by remember { mutableStateOf<Pair<BraceRangeBoundary, String>?>(null) }

    fun candidateRange(boundary: BraceRangeBoundary, draft: String): BraceDateRange? {
        val parsed = if (draft.isBlank()) null else parseLocalizedDate(draft, resolvedLocale) ?: return null
        if (parsed != null && !canSelectDate(parsed, minDate, maxDate, isDateEnabled)) return null
        val start = if (boundary == BraceRangeBoundary.Start) parsed else value.start
        val end = if (boundary == BraceRangeBoundary.End) parsed else value.end
        if (start != null && end != null &&
            (start.isAfter(end) || (start == end && !allowSingleDayRange))) return null
        val next = BraceDateRange(start, end)
        return if (start != null && end != null &&
            !canSelectContinuousRange(next, minDate, maxDate, isDateEnabled)) null else next
    }
    fun commit(boundary: BraceRangeBoundary, draft: String) {
        candidateRange(boundary, draft)?.let { next ->
            lastInvalid = null
            if (next != value) onValueChange(next)
        }
    }
    fun finish(boundary: BraceRangeBoundary) {
        val draft = if (boundary == BraceRangeBoundary.Start) startDraft else endDraft
        val next = candidateRange(boundary, draft)
        if (next == null) {
            val rejected = boundary to draft
            if (lastInvalid != rejected) {
                onInvalidInput(boundary, draft)
                lastInvalid = rejected
            }
        } else {
            commit(boundary, draft)
            val canonical = (if (boundary == BraceRangeBoundary.Start) next.start else next.end)
                ?.let(formatter::format).orEmpty()
            if (boundary == BraceRangeBoundary.Start) startDraft = canonical else endDraft = canonical
        }
    }
    LaunchedEffect(value.start, value.end, resolvedLocale) {
        val localeChanged = resolvedLocale != lastLocale
        if (localeChanged || (value.start != lastStart &&
                (!startEditing || parseLocalizedDate(startDraft, resolvedLocale) != value.start))) {
            startDraft = value.start?.let(formatter::format).orEmpty()
        }
        if (localeChanged || (value.end != lastEnd &&
                (!endEditing || parseLocalizedDate(endDraft, resolvedLocale) != value.end))) {
            endDraft = value.end?.let(formatter::format).orEmpty()
        }
        lastStart = value.start
        lastEnd = value.end
        lastLocale = resolvedLocale
    }
    fun draftError(boundary: BraceRangeBoundary, draft: String): String? {
        if (draft.isBlank()) return validationError
        val candidate = parseLocalizedDate(draft, resolvedLocale) ?: return labels.invalid
        if (!canSelectDate(candidate, minDate, maxDate, isDateEnabled)) return labels.unavailable
        if (boundary == BraceRangeBoundary.Start && value.end != null && candidate.isAfter(value.end)) {
            return labels.overlapping
        }
        if (boundary == BraceRangeBoundary.End && value.start != null && candidate.isBefore(value.start)) {
            return labels.overlapping
        }
        val start = if (boundary == BraceRangeBoundary.Start) candidate else value.start
        val end = if (boundary == BraceRangeBoundary.End) candidate else value.end
        if (start != null && end != null &&
            (start == end && !allowSingleDayRange ||
                !canSelectContinuousRange(BraceDateRange(start, end), minDate, maxDate,
                    isDateEnabled))) return labels.unavailable
        return validationError
    }
    val startError = draftError(BraceRangeBoundary.Start, startDraft)
    val endError = draftError(BraceRangeBoundary.End, endDraft)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(metrics.fieldGap)) {
        Text(label, color = if (enabled) colors.content else colors.disabledContent,
            style = BraceTheme.typography.label)
        BracePopover(
            expanded = expanded,
            onDismissRequest = { expanded = false; activeBoundaryName = null },
            title = labels.title,
            target = {
                Column(verticalArrangement = Arrangement.spacedBy(metrics.fieldGap)) {
                    RangeEndpointTextField(
                        label = labels.start, value = startDraft,
                        onValueChange = { startDraft = it; lastInvalid = null; commit(BraceRangeBoundary.Start, it) },
                        onFinished = { finish(BraceRangeBoundary.Start) },
                        onFocusChanged = { startEditing = it },
                        onOpenCalendar = { activeBoundaryName = BraceRangeBoundary.Start.name; expanded = true },
                        errorMessage = startError, enabled = enabled, readOnly = readOnly,
                        placeholder = formatter.format(LocalDate.of(2026, 1, 1)),
                    )
                    RangeEndpointTextField(
                        label = labels.end, value = endDraft,
                        onValueChange = { endDraft = it; lastInvalid = null; commit(BraceRangeBoundary.End, it) },
                        onFinished = { finish(BraceRangeBoundary.End) },
                        onFocusChanged = { endEditing = it },
                        onOpenCalendar = { activeBoundaryName = BraceRangeBoundary.End.name; expanded = true },
                        errorMessage = endError, enabled = enabled, readOnly = readOnly,
                        placeholder = formatter.format(LocalDate.of(2026, 12, 31)),
                    )
                    BraceButton(labels.open, onClick = {
                        activeBoundaryName = null
                        expanded = true
                    }, enabled = enabled, variant = BraceButtonVariant.Outline)
                }
            },
        ) {
            BraceDateRangePicker(
                value = value,
                onValueChange = { next ->
                    onValueChange(next)
                    startDraft = next.start?.let(formatter::format).orEmpty()
                    endDraft = next.end?.let(formatter::format).orEmpty()
                    if (closeOnCompleteSelection && next.start != null && next.end != null) {
                        expanded = false
                        activeBoundaryName = null
                    }
                },
                locale = resolvedLocale,
                minDate = minDate,
                maxDate = maxDate,
                isDateEnabled = isDateEnabled,
                enabled = enabled,
                allowSingleDayRange = allowSingleDayRange,
                boundaryToModify = activeBoundary,
                shortcuts = shortcuts,
                showClear = showClear,
                clock = clock,
            )
        }
        listOfNotNull(startError, endError).distinct().forEach { message ->
            Text(message, color = BraceTheme.colors.semantic.danger, style = BraceTheme.typography.label)
        }
    }
}

@Composable
private fun RangeEndpointTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onFinished: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onOpenCalendar: () -> Unit,
    errorMessage: String?,
    enabled: Boolean,
    readOnly: Boolean,
    placeholder: String,
) {
    val colors = BraceTheme.colors.components.dateRangeInput
    val metrics = BraceTheme.componentMetrics.dateRangeInput
    val shape = RoundedCornerShape(metrics.cornerRadius)
    var focused by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xxs)) {
        Text(label, color = if (enabled) colors.content else colors.disabledContent,
            style = BraceTheme.typography.label, modifier = Modifier.clearAndSetSemantics {})
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = BraceTheme.sizing.touchTarget)
                .background(if (enabled) colors.container else colors.disabledContainer, shape)
                .border(if (focused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                    when {
                        errorMessage != null -> colors.errorBorder
                        focused -> colors.focusedBorder
                        else -> colors.border
                    }, shape)
                .onFocusChanged { state ->
                    if (focused && !state.isFocused) onFinished()
                    focused = state.isFocused
                    onFocusChanged(state.isFocused)
                }
                .onPreviewKeyEvent { event ->
                    if (enabled && event.type == KeyEventType.KeyDown &&
                        event.key == Key.DirectionDown && event.isAltPressed
                    ) { onOpenCalendar(); true } else false
                }
                .then(if (enabled && !readOnly) Modifier.braceShortcutEditable() else Modifier)
                .semantics {
                    contentDescription = label
                    if (errorMessage != null) error(errorMessage)
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            textStyle = BraceTheme.typography.body.copy(
                color = if (enabled) colors.content else colors.disabledContent),
            cursorBrush = SolidColor(BraceTheme.colors.semantic.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onFinished() }),
            decorationBox = { inner ->
                androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = metrics.horizontalPadding)) {
                    if (value.isEmpty()) Text(placeholder, color = colors.placeholder,
                        style = BraceTheme.typography.body)
                    inner()
                }
            },
        )
    }
}
