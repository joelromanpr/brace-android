package io.github.joelromanpr.brace.blueprinticons

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.icons.BraceIconIntent
import io.github.joelromanpr.brace.icons.BraceIconName
import io.github.joelromanpr.brace.icons.BraceIconRegistry
import io.github.joelromanpr.brace.icons.BraceIconSize
import io.github.joelromanpr.brace.icons.BraceIcons
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/** Pinned artwork resolution; display size remains a separate Brace sizing token. */
public enum class BraceBlueprintIconResolution(public val pixels: Int) { Px16(16), Px20(20) }

/** Searchable metadata transcribed from the pinned Blueprint icons.json file. */
public data class BraceBlueprintIconMetadata(
    public val name: String,
    public val displayName: String,
    public val group: String,
    public val tags: String,
    public val codepoint: Int,
)

private data class Glyph(
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val path: String,
)

private data class Entry(
    val metadata: BraceBlueprintIconMetadata,
    val small: Glyph,
    val large: Glyph,
)

/**
 * An opt-in collection of 706 Blueprint glyphs from the pinned 6.18.0 comparison commit.
 * [load] reads the 844 KB asset synchronously; call it on a background dispatcher at application
 * startup, retain the result, and pass it to the Compose APIs. Each requested [ImageVector] is
 * parsed once and cached; neither Compose rendering nor name lookup performs file I/O.
 * This pack never mutates [BraceIconRegistry.Default] or the original Brace drawings.
 */
public class BraceBlueprintIconPack private constructor(
    private val entries: Map<String, Entry>,
) {
    private val vectors: ConcurrentHashMap<Pair<String, BraceBlueprintIconResolution>, ImageVector> =
        ConcurrentHashMap()

    /** All pinned names; returns an immutable snapshot. */
    public val names: Set<String> get() = entries.keys.toSet()

    /** Number of glyphs in the pack. */
    public val size: Int get() = entries.size

    /** Metadata for a runtime key, or null for an unknown key. */
    public fun metadata(name: String): BraceBlueprintIconMetadata? = entries[name]?.metadata

    /** Search name, display name, group, and tags. Empty queries return the first [limit] names. */
    public fun search(query: String, limit: Int = 50): List<BraceBlueprintIconMetadata> {
        require(limit >= 0) { "Search limit cannot be negative" }
        val normalized = query.trim().lowercase(Locale.ROOT)
        return entries.values.asSequence().map { it.metadata }.filter { metadata ->
            normalized.isEmpty() || listOf(metadata.name, metadata.displayName, metadata.group, metadata.tags)
                .any { it.lowercase(Locale.ROOT).contains(normalized) }
        }.take(limit).toList()
    }

    /** Lazily resolves a typed name. The empty `blank` glyph is a valid vector. */
    public fun find(
        name: BraceIconName,
        resolution: BraceBlueprintIconResolution = BraceBlueprintIconResolution.Px20,
    ): ImageVector? = find(name.value, resolution)

    /** Lazily resolves a runtime name; returns null for an unknown key. */
    public fun find(
        name: String,
        resolution: BraceBlueprintIconResolution = BraceBlueprintIconResolution.Px20,
    ): ImageVector? {
        val entry = entries[name] ?: return null
        return vectors.computeIfAbsent(name to resolution) {
            val glyph = if (resolution == BraceBlueprintIconResolution.Px16) entry.small else entry.large
            val builder = ImageVector.Builder(
                name = "blueprint-${resolution.pixels}-$name",
                defaultWidth = glyph.viewBoxWidth.dp,
                defaultHeight = glyph.viewBoxHeight.dp,
                viewportWidth = glyph.viewBoxWidth,
                viewportHeight = glyph.viewBoxHeight,
            )
            if (glyph.path.isNotEmpty()) {
                builder.addPath(
                    pathData = PathParser().parsePathString(glyph.path).toNodes(),
                    fill = SolidColor(Color.Black),
                )
            }
            builder.build()
        }
    }

    public companion object {
        @Volatile private var sharedPack: BraceBlueprintIconPack? = null

        /**
         * Reads the packaged manifest and validates the pinned name count. Do this off the UI
         * thread when first launching an app; reuse the pack for all screens. No network is used.
         */
        public fun load(context: Context): BraceBlueprintIconPack =
            sharedPack ?: synchronized(this) {
                sharedPack ?: readPack(context).also { sharedPack = it }
            }

        private fun readPack(context: Context): BraceBlueprintIconPack {
            val json = context.assets.open("brace-blueprint-icons.json").bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            require(root.getInt("schemaVersion") == 1) { "Unsupported Blueprint glyph manifest" }
            require(root.getJSONObject("source").getString("commit") ==
                "a60d4c92257612808fbfac81cfeee4fcba91a8b4") { "Unexpected Blueprint glyph source" }
            val icons = root.getJSONArray("icons")
            require(icons.length() == 706) { "Incomplete Blueprint glyph pack" }
            val entries = LinkedHashMap<String, Entry>(icons.length())
            for (index in 0 until icons.length()) {
                val icon = icons.getJSONObject(index)
                val name = icon.getString("name")
                val sizes = icon.getJSONObject("sizes")
                fun glyph(key: String): Glyph {
                    val source = sizes.getJSONObject(key)
                    val viewBox = source.getJSONArray("viewBox")
                    return Glyph(
                        viewBoxWidth = viewBox.getDouble(2).toFloat(),
                        viewBoxHeight = viewBox.getDouble(3).toFloat(),
                        path = source.getString("path"),
                    )
                }
                val metadata = BraceBlueprintIconMetadata(
                    name = name,
                    displayName = icon.getString("displayName"),
                    group = icon.getString("group"),
                    tags = icon.getString("tags"),
                    codepoint = icon.getInt("codepoint"),
                )
                require(entries.put(name, Entry(metadata, glyph("16"), glyph("20"))) == null) {
                    "Duplicate Blueprint glyph name: $name"
                }
            }
            require(entries.keys == BraceBlueprintIconNames.all.map { it.value }.toSet()) {
                "Pinned Blueprint glyph names differ from generated Kotlin names"
            }
            return BraceBlueprintIconPack(entries)
        }
    }
}

/**
 * Draws a typed Blueprint glyph. A null description keeps decorative artwork out of TalkBack.
 * Set `mirrorInRtl` only for glyphs whose meaning follows layout direction.
 * For an action, use [io.github.joelromanpr.brace.icons.BraceIconButton] with the requested vector
 * registered in a scoped registry; the button supplies a 48dp target and focus treatment.
 */
@Composable
public fun BraceBlueprintIcon(
    pack: BraceBlueprintIconPack,
    name: BraceIconName,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: BraceIconSize = BraceIconSize.Medium,
    resolution: BraceBlueprintIconResolution = if (size == BraceIconSize.Small)
        BraceBlueprintIconResolution.Px16 else BraceBlueprintIconResolution.Px20,
    customSize: Dp? = null,
    intent: BraceIconIntent = BraceIconIntent.Default,
    tint: Color? = null,
    mirrorInRtl: Boolean = false,
) {
    BraceBlueprintIconByName(pack, name.value, contentDescription, modifier, size, resolution,
        customSize, intent, tint, mirrorInRtl)
}

/**
 * Draws a runtime Blueprint name. Unknown names show the original Brace Help glyph, so callers
 * should use [BraceBlueprintIconPack.find] to set an accurate spoken description for fallbacks.
 * Set `mirrorInRtl` only for glyphs whose meaning follows layout direction.
 */
@Composable
public fun BraceBlueprintIconByName(
    pack: BraceBlueprintIconPack,
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: BraceIconSize = BraceIconSize.Medium,
    resolution: BraceBlueprintIconResolution = if (size == BraceIconSize.Small)
        BraceBlueprintIconResolution.Px16 else BraceBlueprintIconResolution.Px20,
    customSize: Dp? = null,
    intent: BraceIconIntent = BraceIconIntent.Default,
    tint: Color? = null,
    mirrorInRtl: Boolean = false,
) {
    require(customSize == null || customSize > 0.dp) { "A custom icon size must be positive" }
    require(contentDescription == null || contentDescription.isNotBlank()) {
        "An announced icon needs a nonblank content description"
    }
    val semantic = BraceTheme.colors.semantic
    val intentColor = when (intent) {
        BraceIconIntent.Default -> semantic.onSurface
        BraceIconIntent.Primary -> semantic.primary
        BraceIconIntent.Success -> semantic.success
        BraceIconIntent.Warning -> semantic.warning
        BraceIconIntent.Danger -> semantic.danger
    }
    val dimension = customSize ?: when (size) {
        BraceIconSize.Small -> BraceTheme.sizing.iconSm
        BraceIconSize.Medium -> BraceTheme.sizing.iconMd
        BraceIconSize.Large -> BraceTheme.sizing.iconLg
    }
    val direction = LocalLayoutDirection.current
    Image(
        imageVector = pack.find(name, resolution) ?: BraceIconRegistry.Default.resolve(BraceIcons.Help.value),
        contentDescription = contentDescription,
        modifier = modifier.size(dimension).graphicsLayer {
            scaleX = if (mirrorInRtl && direction == LayoutDirection.Rtl) -1f else 1f
        },
        colorFilter = ColorFilter.tint(tint ?: intentColor),
    )
}
