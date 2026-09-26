package io.github.joelromanpr.brace.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

private val validIconName = Regex("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")

/** A canonical, lowercase icon key. Custom keys follow the same rule as bundled keys. */
public data class BraceIconName(val value: String) {
    init {
        require(validIconName.matches(value)) {
            "Icon names must be lowercase kebab-case and start with a letter"
        }
    }
}

/** Semantic icon sizes that follow the active BraceTheme sizing tokens. */
public enum class BraceIconSize { Small, Medium, Large }

/** Semantic tint roles for an informative icon. A custom tint can override the resolved role. */
public enum class BraceIconIntent { Default, Primary, Success, Warning, Danger }

/**
 * The original vectors bundled with `brace-icons`. This is a small practical set, not the full
 * pinned Blueprint glyph catalog. Their names and provenance are listed in the artifact manifest.
 */
public object BraceIcons {
    public val Add: BraceIconName = BraceIconName("add")
    public val Check: BraceIconName = BraceIconName("check")
    public val ChevronForward: BraceIconName = BraceIconName("chevron-forward")
    public val Close: BraceIconName = BraceIconName("close")
    public val Edit: BraceIconName = BraceIconName("edit")
    public val Help: BraceIconName = BraceIconName("help")
    public val Info: BraceIconName = BraceIconName("info")
    public val More: BraceIconName = BraceIconName("more")
    public val Remove: BraceIconName = BraceIconName("remove")
    public val Search: BraceIconName = BraceIconName("search")
    public val Warning: BraceIconName = BraceIconName("warning")
}

/**
 * An immutable lookup table of icon vectors. [register] returns a new registry and can replace a
 * bundled icon in that registry without changing any other screen. Unknown names resolve to
 * [fallback] (or the bundled Help glyph if that key is absent). This replaces web module loading:
 * Android has no JavaScript chunks, network icon loader, icon font, or dynamic import step.
 */
public class BraceIconRegistry private constructor(
    private val vectors: Map<String, ImageVector>,
) {
    /** All names available in this registry. The returned set is a snapshot. */
    public val names: Set<String> get() = vectors.keys.toSet()

    /** Returns a vector for a typed name, or null when it is unavailable. */
    public fun find(name: BraceIconName): ImageVector? = vectors[name.value]

    /** Returns a vector for a dynamic name, or null when it is unavailable. */
    public fun find(name: String): ImageVector? = vectors[name]

    /** Resolves a dynamic name, then [fallback], then the original bundled Help vector. */
    public fun resolve(name: String, fallback: BraceIconName = BraceIcons.Help): ImageVector =
        find(name) ?: find(fallback) ?: BundledVectors.help

    /** Returns a copy with [vector] registered under [name]; existing registries are unchanged. */
    public fun register(name: BraceIconName, vector: ImageVector): BraceIconRegistry =
        BraceIconRegistry(vectors + (name.value to vector))

    /** Validates a dynamic key and returns a copy with [vector] registered under it. */
    public fun register(name: String, vector: ImageVector): BraceIconRegistry =
        register(BraceIconName(name), vector)

    public companion object {
        /** The bundled Brace vectors. They are all original Apache-2.0 artwork. */
        public val Default: BraceIconRegistry = BraceIconRegistry(BundledVectors.all)

        /** An empty registry for apps that opt into only their own vectors. */
        public fun empty(): BraceIconRegistry = BraceIconRegistry(emptyMap())
    }
}

/** Current registry for a Compose subtree. The default provides the bundled vectors. */
public val LocalBraceIconRegistry = staticCompositionLocalOf { BraceIconRegistry.Default }

/** Scopes a custom registry to [content] without changing the registry used by other screens. */
@Composable
public fun BraceIconRegistryProvider(
    registry: BraceIconRegistry,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalBraceIconRegistry provides registry, content = content)
}

/**
 * Draws a typed icon. Pass null [contentDescription] when its meaning is already in nearby text;
 * a meaningful nonblank description makes a standalone informative icon one TalkBack stop. Icons
 * are not controls. [intent] selects a Brace semantic color, [customSize] allows an app-specific
 * dp dimension, and [tint] overrides the resolved role. Use [BraceIconButton] for an action with
 * a 48dp touch target and keyboard focus.
 */
@Composable
public fun BraceIcon(
    name: BraceIconName,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: BraceIconSize = BraceIconSize.Medium,
    customSize: Dp? = null,
    intent: BraceIconIntent = BraceIconIntent.Default,
    tint: Color? = null,
    registry: BraceIconRegistry? = null,
) {
    BraceIconByName(
        name = name.value,
        contentDescription = contentDescription,
        modifier = modifier,
        size = size,
        customSize = customSize,
        intent = intent,
        tint = tint,
        registry = registry,
    )
}

/**
 * Draws an icon by a runtime name. Missing names show [fallback]; if the custom registry does not
 * contain that fallback, the original bundled Help vector appears. This path is useful for
 * server-driven data and never performs network or disk work while composing.
 */
@Composable
public fun BraceIconByName(
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: BraceIconSize = BraceIconSize.Medium,
    customSize: Dp? = null,
    intent: BraceIconIntent = BraceIconIntent.Default,
    tint: Color? = null,
    registry: BraceIconRegistry? = null,
    fallback: BraceIconName = BraceIcons.Help,
) {
    require(customSize == null || customSize > 0.dp) { "A custom icon size must be positive" }
    require(contentDescription == null || contentDescription.isNotBlank()) {
        "An announced icon needs a nonblank content description"
    }
    val active = registry ?: LocalBraceIconRegistry.current
    val tokens = BraceTheme.sizing
    val dimension = customSize ?: when (size) {
        BraceIconSize.Small -> tokens.iconSm
        BraceIconSize.Medium -> tokens.iconMd
        BraceIconSize.Large -> tokens.iconLg
    }
    val semantic = BraceTheme.colors.semantic
    val intentColor = when (intent) {
        BraceIconIntent.Default -> semantic.onSurface
        BraceIconIntent.Primary -> semantic.primary
        BraceIconIntent.Success -> semantic.success
        BraceIconIntent.Warning -> semantic.warning
        BraceIconIntent.Danger -> semantic.danger
    }
    Image(
        imageVector = active.resolve(name, fallback),
        contentDescription = contentDescription,
        modifier = modifier.size(dimension),
        colorFilter = ColorFilter.tint(tint ?: intentColor),
    )
}

/**
 * An icon-only action with a token-driven 48dp minimum target, one labeled TalkBack stop, and
 * visible keyboard focus. The inner icon stays decorative because [label] names the action.
 */
@Composable
public fun BraceIconButton(
    name: BraceIconName,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: BraceIconSize = BraceIconSize.Medium,
    registry: BraceIconRegistry? = null,
) {
    require(label.isNotBlank()) { "An icon button needs a nonblank action label" }
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = BraceTheme.colors.semantic
    val shape = RoundedCornerShape(BraceTheme.shape.sm)
    val container = when {
        !enabled -> colors.disabledContainer
        pressed -> colors.pressed
        hovered -> colors.hover
        else -> colors.surface
    }
    Box(
        modifier = Modifier
            .defaultMinSize(
                minWidth = BraceTheme.sizing.touchTarget,
                minHeight = BraceTheme.sizing.touchTarget,
            )
            .then(modifier)
            .focusRequester(focusRequester)
            .background(container, shape)
            .border(
                if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                if (focused) colors.focusRing else colors.border,
                shape,
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        BraceIcon(
            name = name,
            contentDescription = null,
            size = iconSize,
            tint = if (enabled) colors.onSurface else colors.disabledContent,
            registry = registry,
        )
    }
}
