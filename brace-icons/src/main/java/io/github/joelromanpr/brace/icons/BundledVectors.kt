package io.github.joelromanpr.brace.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Original 24-unit line drawings by Brace Android contributors; see the bundled asset manifest. */
internal object BundledVectors {
    private fun vector(
        name: String,
        autoMirror: Boolean = false,
        draw: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = "brace-$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).apply(draw).build()

    private val ink = SolidColor(Color.Black)

    val add: ImageVector = vector("add") {
        path(fill = null, stroke = ink, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
            moveTo(12f, 4f); lineTo(12f, 20f)
            moveTo(4f, 12f); lineTo(20f, 12f)
        }
    }
    val check: ImageVector = vector("check") {
        path(fill = null, stroke = ink, strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(4f, 12f); lineTo(10f, 18f); lineTo(20f, 6f)
        }
    }
    val chevronForward: ImageVector = vector("chevron-forward", autoMirror = true) {
        path(fill = null, stroke = ink, strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
        }
    }
    val close: ImageVector = vector("close") {
        path(fill = null, stroke = ink, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
            moveTo(5f, 5f); lineTo(19f, 19f)
            moveTo(19f, 5f); lineTo(5f, 19f)
        }
    }
    val edit: ImageVector = vector("edit") {
        path(fill = null, stroke = ink, strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(4f, 20f); lineTo(9f, 19f); lineTo(20f, 8f)
            lineTo(16f, 4f); lineTo(5f, 15f); close()
            moveTo(14f, 6f); lineTo(18f, 10f)
        }
    }
    val help: ImageVector = vector("help") {
        path(fill = null, stroke = ink, strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 3f)
            curveTo(17f, 3f, 21f, 7f, 21f, 12f)
            curveTo(21f, 17f, 17f, 21f, 12f, 21f)
            curveTo(7f, 21f, 3f, 17f, 3f, 12f)
            curveTo(3f, 7f, 7f, 3f, 12f, 3f)
            close()
            moveTo(9f, 9f)
            curveTo(9f, 7f, 10f, 6f, 12f, 6f)
            curveTo(14f, 6f, 15f, 7f, 15f, 9f)
            curveTo(15f, 11f, 12f, 11f, 12f, 14f)
            moveTo(12f, 17f); lineTo(12.01f, 17f)
        }
    }
    val info: ImageVector = vector("info") {
        path(fill = null, stroke = ink, strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 3f)
            curveTo(17f, 3f, 21f, 7f, 21f, 12f)
            curveTo(21f, 17f, 17f, 21f, 12f, 21f)
            curveTo(7f, 21f, 3f, 17f, 3f, 12f)
            curveTo(3f, 7f, 7f, 3f, 12f, 3f)
            close()
            moveTo(12f, 11f); lineTo(12f, 17f)
            moveTo(12f, 7f); lineTo(12.01f, 7f)
        }
    }
    val more: ImageVector = vector("more") {
        path(fill = null, stroke = ink, strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
            moveTo(5f, 12f); lineTo(5.01f, 12f)
            moveTo(12f, 12f); lineTo(12.01f, 12f)
            moveTo(19f, 12f); lineTo(19.01f, 12f)
        }
    }
    val remove: ImageVector = vector("remove") {
        path(fill = null, stroke = ink, strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
            moveTo(4f, 12f); lineTo(20f, 12f)
        }
    }
    val search: ImageVector = vector("search") {
        path(fill = null, stroke = ink, strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(10f, 4f)
            curveTo(13.4f, 4f, 16f, 6.6f, 16f, 10f)
            curveTo(16f, 13.4f, 13.4f, 16f, 10f, 16f)
            curveTo(6.6f, 16f, 4f, 13.4f, 4f, 10f)
            curveTo(4f, 6.6f, 6.6f, 4f, 10f, 4f)
            close()
            moveTo(15f, 15f); lineTo(21f, 21f)
        }
    }
    val warning: ImageVector = vector("warning") {
        path(fill = null, stroke = ink, strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 3f); lineTo(22f, 20f); lineTo(2f, 20f); close()
            moveTo(12f, 9f); lineTo(12f, 14f)
            moveTo(12f, 17f); lineTo(12.01f, 17f)
        }
    }

    val all: Map<String, ImageVector> = mapOf(
        BraceIcons.Add.value to add,
        BraceIcons.Check.value to check,
        BraceIcons.ChevronForward.value to chevronForward,
        BraceIcons.Close.value to close,
        BraceIcons.Edit.value to edit,
        BraceIcons.Help.value to help,
        BraceIcons.Info.value to info,
        BraceIcons.More.value to more,
        BraceIcons.Remove.value to remove,
        BraceIcons.Search.value to search,
        BraceIcons.Warning.value to warning,
    )
}
