package io.github.joelromanpr.brace.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton

/** Compiles against Maven coordinates only, with no dependency on the source checkout. */
class ConsumerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BraceTheme {
                var count by remember { mutableStateOf(0) }
                BraceButton(label = "Saved $count", onClick = { count++ })
            }
        }
    }
}
