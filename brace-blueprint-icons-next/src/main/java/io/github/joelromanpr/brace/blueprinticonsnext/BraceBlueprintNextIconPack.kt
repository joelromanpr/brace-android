package io.github.joelromanpr.brace.blueprinticonsnext

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.icons.BraceIconIntent
import io.github.joelromanpr.brace.icons.BraceIconName
import io.github.joelromanpr.brace.icons.BraceIconRegistry
import io.github.joelromanpr.brace.icons.BraceIconSize
import io.github.joelromanpr.brace.icons.BraceIcons
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/** A next-generation Blueprint glyph's published artwork variant. */
public enum class BraceBlueprintNextIconVariant { Outlined, Filled }

/** Searchable metadata copied from pinned `@blueprintjs/icons/next` sources. */
public data class BraceBlueprintNextIconMetadata(
    public val name: String,
    public val tags: List<String>,
    public val hasFilled: Boolean,
)

private data class Glyph(val width: Float, val height: Float, val path: String)
private data class Entry(
    val metadata: BraceBlueprintNextIconMetadata,
    val outlined: Glyph,
    val filled: Glyph?,
)

/**
 * An opt-in pack of 695 canonical outlined icons and 386 filled variants from pinned
 * `@blueprintjs/icons/next`. [load] synchronously reads a packaged JSON asset; load on a
 * background dispatcher and retain the pack. Vector paths parse lazily once per name and
 * variant, with no file or network I/O during Compose rendering. Original Brace icons and
 * the separate legacy Blueprint pack are not modified.
 */
public class BraceBlueprintNextIconPack private constructor(
    private val entries: Map<String, Entry>,
    private val legacyMap: Map<String, String>,
) {
    private val vectors: ConcurrentHashMap<Pair<String, BraceBlueprintNextIconVariant>, ImageVector> =
        ConcurrentHashMap()

    /** Canonical next glyph names, returned as an immutable snapshot. */
    public val names: Set<String> get() = entries.keys.toSet()

    /** Number of distinct canonical next glyphs. */
    public val size: Int get() = entries.size

    /** Number of glyphs with an additional filled variant. */
    public val filledCount: Int get() = entries.values.count { it.filled != null }

    /** Metadata for a canonical name, or null for an unknown name. */
    public fun metadata(name: String): BraceBlueprintNextIconMetadata? = entries[name]?.metadata

    /**
     * Convert a legacy Blueprint icon name to its canonical next name. All 706 legacy names
     * have a pinned mapping; this does not rename the original Brace icon registry.
     */
    public fun nextNameForLegacy(name: String): BraceIconName? =
        legacyMap[name]?.let(::BraceIconName)

    /** Search canonical names and pinned tags. Empty queries return the first [limit] entries. */
    public fun search(query: String, limit: Int = 50): List<BraceBlueprintNextIconMetadata> {
        require(limit >= 0) { "Search limit cannot be negative" }
        val normalized = query.trim().lowercase(Locale.ROOT)
        return entries.values.asSequence().map { it.metadata }.filter { metadata ->
            normalized.isEmpty() || metadata.name.lowercase(Locale.ROOT).contains(normalized) ||
                metadata.tags.any { it.lowercase(Locale.ROOT).contains(normalized) }
        }.take(limit).toList()
    }

    /** Find exactly the requested typed variant; null means the name or variant is absent. */
    public fun find(
        name: BraceIconName,
        variant: BraceBlueprintNextIconVariant = BraceBlueprintNextIconVariant.Outlined,
    ): ImageVector? = find(name.value, variant)

    /** Find exactly the requested runtime variant; missing filled artwork returns null. */
    public fun find(
        name: String,
        variant: BraceBlueprintNextIconVariant = BraceBlueprintNextIconVariant.Outlined,
    ): ImageVector? {
        val entry = entries[name] ?: return null
        val glyph = when (variant) {
            BraceBlueprintNextIconVariant.Outlined -> entry.outlined
            BraceBlueprintNextIconVariant.Filled -> entry.filled ?: return null
        }
        return vectors.computeIfAbsent(name to variant) {
            ImageVector.Builder(
                name = "blueprint-next-${variant.name.lowercase(Locale.ROOT)}-$name",
                defaultWidth = glyph.width.dp,
                defaultHeight = glyph.height.dp,
                viewportWidth = glyph.width,
                viewportHeight = glyph.height,
            ).apply {
                if (glyph.path.isNotEmpty()) addPath(
                    pathData = PathParser().parsePathString(glyph.path).toNodes(),
                    fill = SolidColor(Color.Black),
                )
            }.build()
        }
    }

    public companion object {
        @Volatile private var sharedPack: BraceBlueprintNextIconPack? = null

        /** Read and validate the local pack once; call off the main thread on first use. */
        public fun load(context: Context): BraceBlueprintNextIconPack =
            sharedPack ?: synchronized(this) {
                sharedPack ?: readPack(context).also { sharedPack = it }
            }

        private fun readPack(context: Context): BraceBlueprintNextIconPack {
            val json = context.assets.open("brace-blueprint-icons-next.json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            require(root.getInt("schemaVersion") == 1 &&
                root.getJSONObject("source").getString("commit") ==
                "a60d4c92257612808fbfac81cfeee4fcba91a8b4") {
                "Unexpected Blueprint next glyph source"
            }
            val icons = root.getJSONArray("icons")
            require(icons.length() == 695) { "Incomplete Blueprint next glyph pack" }
            val entries = LinkedHashMap<String, Entry>(icons.length())
            fun glyph(source: JSONObject): Glyph {
                val box = source.getJSONArray("viewBox")
                return Glyph(box.getDouble(2).toFloat(), box.getDouble(3).toFloat(),
                    source.getString("path"))
            }
            for (index in 0 until icons.length()) {
                val icon = icons.getJSONObject(index)
                val name = icon.getString("name")
                val tags = icon.getJSONArray("tags")
                val tagList = Collections.unmodifiableList(List(tags.length()) { tags.getString(it) })
                val filled = if (icon.isNull("filled")) null else glyph(icon.getJSONObject("filled"))
                val entry = Entry(
                    BraceBlueprintNextIconMetadata(name, tagList, filled != null),
                    glyph(icon.getJSONObject("outlined")), filled,
                )
                require(entries.put(name, entry) == null) { "Duplicate next glyph: $name" }
            }
            require(entries.values.count { it.filled != null } == 386) {
                "Incomplete Blueprint next filled variants"
            }
            require(entries.keys == BraceBlueprintNextIconNames.all.map { it.value }.toSet()) {
                "Generated next names differ from packaged glyphs"
            }
            val names = root.getJSONObject("legacyNameMap")
            require(names.length() == 706) { "Incomplete Blueprint legacy name mapping" }
            val legacy = LinkedHashMap<String, String>(names.length())
            for (name in names.keys()) {
                val next = names.getString(name)
                require(next in entries) { "Unknown next glyph mapping: $name" }
                legacy[name] = next
            }
            return BraceBlueprintNextIconPack(entries, legacy)
        }
    }
}

/**
 * Draw a typed next-generation glyph with Brace theme colors and sizing. If [variant] is
 * Filled but unavailable for this name, the outlined glyph is drawn. Set [mirrorInRtl]
 * only for directional artwork whose meaning follows layout direction. A null
 * [contentDescription] means decorative artwork; icon actions should use a scoped
 * [io.github.joelromanpr.brace.icons.BraceIconButton] with a named 48dp target.
 */
@Composable
public fun BraceBlueprintNextIcon(
    pack: BraceBlueprintNextIconPack,
    name: BraceIconName,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: BraceIconSize = BraceIconSize.Medium,
    variant: BraceBlueprintNextIconVariant = BraceBlueprintNextIconVariant.Outlined,
    customSize: Dp? = null,
    intent: BraceIconIntent = BraceIconIntent.Default,
    tint: Color? = null,
    mirrorInRtl: Boolean = false,
) {
    BraceBlueprintNextIconByName(pack, name.value, contentDescription, modifier, size,
        variant, customSize, intent, tint, mirrorInRtl)
}

/**
 * Draw a runtime canonical next name. Unknown names use Brace's original Help glyph; callers
 * should provide an accurate fallback description. Missing filled artwork uses the outlined
 * version of the same glyph. See [BraceBlueprintNextIconPack.nextNameForLegacy] for old names.
 */
@Composable
public fun BraceBlueprintNextIconByName(
    pack: BraceBlueprintNextIconPack,
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: BraceIconSize = BraceIconSize.Medium,
    variant: BraceBlueprintNextIconVariant = BraceBlueprintNextIconVariant.Outlined,
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
    val vector = pack.find(name, variant)
        ?: pack.find(name, BraceBlueprintNextIconVariant.Outlined)
        ?: BraceIconRegistry.Default.resolve(BraceIcons.Help.value)
    Image(
        imageVector = vector,
        contentDescription = contentDescription,
        modifier = modifier.size(dimension).graphicsLayer {
            scaleX = if (mirrorInRtl && direction == LayoutDirection.Rtl) -1f else 1f
        },
        colorFilter = ColorFilter.tint(tint ?: intentColor),
    )
}
