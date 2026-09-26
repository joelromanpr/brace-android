package io.github.joelromanpr.brace.datetime

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.braceShortcutEditable
import java.time.Clock
import java.time.LocalDate
import java.util.Locale

/**
 * Controlled date-only text field with an anchored [BraceDatePicker].
 *
 * The value is a [LocalDate], avoiding an implicit midnight time zone conversion. The draft
 * text and popup state survive activity recreation. Valid localized text emits [onValueChange]
 * immediately; invalid or unavailable text remains visible and is announced as an error.
 * [onInvalidInput] fires when the user finishes an invalid draft. Blank text clears the date.
 * The calendar button, or Alt+Down from the field, opens the picker. [shortcuts] and
 * [canClearSelection] pass through to the calendar, and [closeOnSelection] controls dismissal.
 * The caller owns [value]
 * and should save it (for example as its ISO date string) when restoration is needed.
 */
@Composable
public fun BraceDateField(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isDateEnabled: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
    readOnly: Boolean = false,
    canClearSelection: Boolean = true,
    showActions: Boolean = true,
    closeOnSelection: Boolean = true,
    shortcuts: List<BraceDateShortcut> = emptyList(),
    placeholder: String? = null,
    supportingText: String? = null,
    validationError: String? = null,
    onInvalidInput: (String) -> Unit = {},
    clock: Clock = Clock.systemDefaultZone(),
) {
    require(minDate == null || maxDate == null || !minDate.isAfter(maxDate)) {
        "minDate must be on or before maxDate"
    }
    val hostConfiguration = LocalConfiguration.current
    val resolvedLocale = locale ?: hostConfiguration.locales[0] ?: Locale.getDefault()
    val context = LocalContext.current
    val localizedContext = remember(context, hostConfiguration, resolvedLocale) {
        val configuration = Configuration(hostConfiguration)
        configuration.setLocale(resolvedLocale)
        context.createConfigurationContext(configuration)
    }
    val openLabel = remember(localizedContext, label) {
        localizedContext.getString(R.string.brace_date_open_calendar, label)
    }
    val invalidMessage = remember(localizedContext) { localizedContext.getString(R.string.brace_date_invalid) }
    val unavailableMessage = remember(localizedContext) { localizedContext.getString(R.string.brace_date_out_of_range) }
    val calendarTitle = remember(localizedContext) { localizedContext.getString(R.string.brace_date_calendar_title) }
    val formatter = remember(resolvedLocale) { localizedDateFormatter(resolvedLocale) }
    val colors = BraceTheme.colors.components.dateInput
    val metrics = BraceTheme.componentMetrics.dateInput
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val focusRequester = remember { FocusRequester() }
    val fieldSource = remember { MutableInteractionSource() }
    val fieldFocused by fieldSource.collectIsFocusedAsState()
    val calendarSource = remember { MutableInteractionSource() }
    val calendarRequester = remember { FocusRequester() }
    val calendarFocused by calendarSource.collectIsFocusedAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(value?.let(formatter::format).orEmpty()) }
    var editing by remember { mutableStateOf(false) }
    var lastValue by remember { mutableStateOf(value) }
    var lastLocale by remember { mutableStateOf(resolvedLocale) }
    var lastInvalidNotification by remember { mutableStateOf<String?>(null) }
    val parsed = remember(draft, resolvedLocale) { parseLocalizedDate(draft, resolvedLocale) }
    val hasDraft = draft.isNotBlank()
    val invalid = hasDraft && parsed == null
    val unavailable = parsed != null && !canSelectDate(parsed, minDate, maxDate, isDateEnabled)
    val errorMessage = when {
        validationError != null -> validationError
        invalid -> invalidMessage
        unavailable -> unavailableMessage
        else -> null
    }
    LaunchedEffect(value, resolvedLocale) {
        if (resolvedLocale != lastLocale || (value != lastValue && (!editing || value != parsed))) {
            draft = value?.let(formatter::format).orEmpty()
        }
        lastValue = value
        lastLocale = resolvedLocale
    }
    fun finishDraft() {
        val candidate = parseLocalizedDate(draft, resolvedLocale)
        when {
            draft.isBlank() -> { if (value != null) onValueChange(null); draft = "" }
            candidate == null || !canSelectDate(candidate, minDate, maxDate, isDateEnabled) -> {
                if (draft != lastInvalidNotification) {
                    onInvalidInput(draft)
                    lastInvalidNotification = draft
                }
            }
            else -> {
                lastInvalidNotification = null
                if (candidate != value) onValueChange(candidate)
                draft = formatter.format(candidate)
            }
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = BraceTheme.sizing.touchTarget)
                .then(if (enabled) Modifier.pointerInput(focusRequester) {
                    detectTapGestures(onTap = { focusRequester.requestFocus() })
                } else Modifier)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(label, color = if (enabled) colors.content else colors.disabledContent,
                style = BraceTheme.typography.label)
        }
        BracePopover(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            title = calendarTitle,
            target = {
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (enabled) colors.container else colors.disabledContainer, shape)
                        .border(
                            if (fieldFocused || calendarFocused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                            when {
                                errorMessage != null -> colors.errorBorder
                                fieldFocused || calendarFocused -> colors.focusedBorder
                                else -> colors.border
                            }, shape,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { next ->
                            draft = next
                            lastInvalidNotification = null
                            if (next.isBlank()) {
                                if (value != null) onValueChange(null)
                            } else {
                                val candidate = parseLocalizedDate(next, resolvedLocale)
                                if (candidate != null && canSelectDate(candidate, minDate, maxDate, isDateEnabled) && candidate != value) {
                                    onValueChange(candidate)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                            .heightIn(min = BraceTheme.sizing.touchTarget)
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                if (editing && !state.isFocused) finishDraft()
                                editing = state.isFocused
                            }
                            .onPreviewKeyEvent { event ->
                                if (enabled && event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionDown && event.isAltPressed
                                ) { expanded = true; true } else false
                            }
                            .then(if (enabled && !readOnly) Modifier.braceShortcutEditable() else Modifier)
                            .semantics {
                                contentDescription = label
                                if (errorMessage != null) error(errorMessage)
                                else if (supportingText != null) stateDescription = supportingText
                            },
                        enabled = enabled,
                        readOnly = readOnly,
                        singleLine = true,
                        textStyle = BraceTheme.typography.body.copy(
                            color = if (enabled) colors.content else colors.disabledContent,
                        ),
                        cursorBrush = SolidColor(BraceTheme.colors.semantic.primary),
                        interactionSource = fieldSource,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { finishDraft() }),
                        decorationBox = { inner ->
                            Box(Modifier.padding(horizontal = metrics.horizontalPadding), contentAlignment = Alignment.CenterStart) {
                                if (draft.isEmpty()) {
                                    Text(placeholder ?: formatter.format(LocalDate.of(2026, 12, 31)),
                                        color = colors.placeholder, style = BraceTheme.typography.body)
                                }
                                inner()
                            }
                        },
                    )
                    Box(
                        Modifier.size(BraceTheme.sizing.touchTarget)
                            .focusRequester(calendarRequester)
                            .clearAndSetSemantics {
                                contentDescription = openLabel
                                role = Role.Button
                                if (!enabled) disabled() else {
                                    focused = calendarFocused
                                    onClick { expanded = true; true }
                                    requestFocus { calendarRequester.requestFocus() }
                                }
                            }
                            .clickable(enabled = enabled, role = Role.Button,
                                interactionSource = calendarSource, indication = null) { expanded = true }
                            .border(
                                if (calendarFocused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                                if (calendarFocused) colors.focusRing else androidx.compose.ui.graphics.Color.Transparent,
                                shape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(BraceTheme.sizing.iconMd)) {
                            val stroke = 1.8.dp.toPx()
                            drawRoundRect(
                                color = if (enabled) colors.calendarIcon else colors.disabledContent,
                                cornerRadius = CornerRadius(2.dp.toPx()),
                                style = Stroke(stroke),
                            )
                            drawLine(
                                color = if (enabled) colors.calendarIcon else colors.disabledContent,
                                start = Offset(0f, size.height * 0.35f),
                                end = Offset(size.width, size.height * 0.35f),
                                strokeWidth = stroke,
                            )
                            repeat(2) { row -> repeat(2) { col ->
                                drawCircle(
                                    color = if (enabled) colors.calendarIcon else colors.disabledContent,
                                    radius = 1.4.dp.toPx(),
                                    center = Offset(size.width * (0.35f + col * 0.3f), size.height * (0.55f + row * 0.22f)),
                                )
                            } }
                        }
                    }
                }
            },
        ) {
            BraceDatePicker(
                value = value,
                onValueChange = { date ->
                    onValueChange(date)
                    draft = date?.let(formatter::format).orEmpty()
                    if (closeOnSelection) expanded = false
                },
                locale = resolvedLocale,
                minDate = minDate,
                maxDate = maxDate,
                isDateEnabled = isDateEnabled,
                enabled = enabled,
                canClearSelection = canClearSelection,
                showActions = showActions,
                shortcuts = shortcuts,
                clock = clock,
            )
        }
        val helper = errorMessage ?: supportingText
        if (helper != null) {
            Text(helper,
                color = if (errorMessage != null) BraceTheme.colors.semantic.danger
                    else BraceTheme.colors.semantic.onSurfaceMuted,
                style = BraceTheme.typography.label)
        }
    }
}
