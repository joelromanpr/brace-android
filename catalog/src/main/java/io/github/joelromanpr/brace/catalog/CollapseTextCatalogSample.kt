package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceCollapse
import io.github.joelromanpr.brace.core.BraceSectionCard
import io.github.joelromanpr.brace.core.BraceText

@Composable
internal fun CollapseCatalogSample() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var keepMounted by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        BraceButton(
            if (expanded) "Hide details" else "Show details",
            onClick = { expanded = !expanded },
            modifier = Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
            variant = BraceButtonVariant.Outline,
        )
        BraceButton(
            if (keepMounted) "Unmount when closed" else "Keep mounted",
            onClick = { keepMounted = !keepMounted },
            variant = BraceButtonVariant.Outline,
        )
        BraceCollapse(expanded = expanded, keepContentMounted = keepMounted) {
            BraceSectionCard {
                BraceText("Quarterly details remain in reading order when expanded.")
            }
        }
    }
}

@Composable
internal fun TextCatalogSample() {
    var narrow by rememberSaveable { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        BraceText(
            "A long project title that overflows a narrow row",
            modifier = Modifier.width(if (narrow) 140.dp else 280.dp),
            ellipsize = true,
        )
        BraceText("This longer description wraps onto multiple lines when needed.")
        BraceText("Status", title = "Current report status")
        BraceButton(if (narrow) "Widen text" else "Narrow text", onClick = { narrow = !narrow })
        Text("Tap and hold truncated text to see it all", color = BraceTheme.colors.semantic.onSurfaceMuted)
    }
}
