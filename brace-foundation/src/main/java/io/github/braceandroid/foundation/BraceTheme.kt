package io.github.braceandroid.foundation

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

/** The base color mode for a Brace theme scope. */
enum class BraceColorMode { Light, Dark }

/** Standard or increased color and boundary contrast. */
enum class BraceContrast { Standard, High }

/** Compact data views or more spacious controls. Both retain 48 dp touch targets. */
enum class BraceDensity { Compact, Comfortable }

/** System animation preference, forced full motion, or reduced motion. */
enum class BraceMotion { System, Full, Reduced }

/**
 * Brand accent colors. Supply a [primary] and [onPrimary] pair with suitable
 * text contrast. Optional colors override the derived hover, focus and subtle
 * roles. All component color tokens are rebuilt from these semantic roles.
 */
@Immutable
data class BraceBrandColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryHover: Color? = null,
    val primaryPressed: Color? = null,
    val primarySubtle: Color? = null,
    val onPrimarySubtle: Color? = null,
    val focusRing: Color? = null,
    val selection: Color? = null,
    val onSelection: Color? = null,
)

/**
 * Values replaced only within a nested [BraceTheme] scope. Color overrides are
 * per semantic role; other token groups can be replaced as typed values.
 */
@Immutable
data class BraceThemeOverrides(
    val colors: BraceSemanticColorOverrides = BraceSemanticColorOverrides(),
    val typography: BraceTypographyTokens? = null,
    val spacing: BraceSpacingTokens? = null,
    val sizing: BraceSizingTokens? = null,
    val shape: BraceShapeTokens? = null,
    val elevation: BraceElevationTokens? = null,
    val motion: BraceMotionTokens? = null,
    val componentMetrics: BraceComponentMetrics? = null,
)

@Immutable
private data class BraceThemeValues(
    val mode: BraceColorMode,
    val contrast: BraceContrast,
    val density: BraceDensity,
    val motion: BraceMotion,
    val systemMotionReduced: Boolean,
    val brand: BraceBrandColors?,
    val colors: BraceColorScheme,
    val typography: BraceTypographyTokens,
    val spacing: BraceSpacingTokens,
    val sizing: BraceSizingTokens,
    val shape: BraceShapeTokens,
    val elevation: BraceElevationTokens,
    val motionBase: BraceMotionTokens,
    val motionTokens: BraceMotionTokens,
    val densityTokens: BraceDensityTokens,
    val componentMetrics: BraceComponentMetrics,
)

private val LocalBraceTheme = compositionLocalOf<BraceThemeValues?> { null }

/**
 * Brace's token-driven Compose theme.
 *
 * Omitted arguments inherit from a parent Brace scope. At the root, mode
 * follows the system dark setting; contrast and density default to Standard
 * and Comfortable. Change any argument from state to switch at runtime.
 * Nested calls can change one token group without changing the app theme.
 *
 * `BraceTheme(brand = BraceBrandColors(Color(0xFF005B4A), Color.White)) { ... }`
 */
object BraceTheme {
    @Composable
    operator fun invoke(
        mode: BraceColorMode? = null,
        contrast: BraceContrast? = null,
        density: BraceDensity? = null,
        motion: BraceMotion? = null,
        brand: BraceBrandColors? = null,
        overrides: BraceThemeOverrides = BraceThemeOverrides(),
        content: @Composable () -> Unit,
    ) {
        val parent = LocalBraceTheme.current
        val resolvedMode = mode ?: parent?.mode
            ?: if (isSystemInDarkTheme()) BraceColorMode.Dark else BraceColorMode.Light
        val resolvedContrast = contrast ?: parent?.contrast ?: BraceContrast.Standard
        val resolvedDensity = density ?: parent?.density ?: BraceDensity.Comfortable
        val resolvedMotion = motion ?: parent?.motion ?: BraceMotion.System
        val resolvedBrand = brand ?: parent?.brand

        val base = defaultColors(resolvedMode, resolvedContrast)
        val colorModeChanged = parent != null &&
            (parent.mode != resolvedMode || parent.contrast != resolvedContrast)
        val inheritedSemantic = if (parent == null || colorModeChanged) {
            base.semantic
        } else parent.colors.semantic
        val branded = if (brand != null || colorModeChanged) {
            inheritedSemantic.withBrand(resolvedBrand, resolvedMode)
        } else inheritedSemantic
        val semantic = branded.withOverrides(overrides.colors)
        val colors = BraceColorScheme(semantic, buildBraceComponentColors(semantic))

        val motionPreferenceIsReduced = when {
            resolvedMotion != BraceMotion.System -> false
            parent?.motion == BraceMotion.System -> parent?.systemMotionReduced ?: false
            else -> systemReducedMotion()
        }
        val baseMotion = overrides.motion ?: parent?.motionBase ?: BraceTokenDefaults.motion
        val resolvedMotionTokens = if (
            resolvedMotion == BraceMotion.Reduced || motionPreferenceIsReduced
        ) baseMotion.withoutAnimation() else baseMotion
        val values = BraceThemeValues(
            mode = resolvedMode,
            contrast = resolvedContrast,
            density = resolvedDensity,
            motion = resolvedMotion,
            systemMotionReduced = motionPreferenceIsReduced,
            brand = resolvedBrand,
            colors = colors,
            typography = overrides.typography ?: parent?.typography ?: BraceTokenDefaults.typography,
            spacing = overrides.spacing ?: parent?.spacing ?: BraceTokenDefaults.spacing,
            sizing = overrides.sizing ?: parent?.sizing ?: BraceTokenDefaults.sizing,
            shape = overrides.shape ?: parent?.shape ?: BraceTokenDefaults.shape,
            elevation = overrides.elevation ?: parent?.elevation ?: BraceTokenDefaults.elevation,
            motionBase = baseMotion,
            motionTokens = resolvedMotionTokens,
            densityTokens = when (resolvedDensity) {
                BraceDensity.Compact -> BraceTokenDefaults.compact
                BraceDensity.Comfortable -> BraceTokenDefaults.comfortable
            },
            componentMetrics = overrides.componentMetrics ?: parent?.componentMetrics
                ?: BraceTokenDefaults.componentMetrics,
        )
        CompositionLocalProvider(LocalBraceTheme provides values, content = content)
    }

    /** Semantic and component state colors of the current scope. */
    val colors: BraceColorScheme
        @Composable get() = LocalBraceTheme.current?.colors ?: BraceTokenDefaults.light

    /** Named text styles that scale with Android font size preferences. */
    val typography: BraceTypographyTokens
        @Composable get() = LocalBraceTheme.current?.typography ?: BraceTokenDefaults.typography

    val spacing: BraceSpacingTokens
        @Composable get() = LocalBraceTheme.current?.spacing ?: BraceTokenDefaults.spacing

    val sizing: BraceSizingTokens
        @Composable get() = LocalBraceTheme.current?.sizing ?: BraceTokenDefaults.sizing

    val shape: BraceShapeTokens
        @Composable get() = LocalBraceTheme.current?.shape ?: BraceTokenDefaults.shape

    val elevation: BraceElevationTokens
        @Composable get() = LocalBraceTheme.current?.elevation ?: BraceTokenDefaults.elevation

    /** Resolved motion durations; all are zero when reduced motion is active. */
    val motionTokens: BraceMotionTokens
        @Composable get() = LocalBraceTheme.current?.motionTokens ?: BraceTokenDefaults.motion

    val densityTokens: BraceDensityTokens
        @Composable get() = LocalBraceTheme.current?.densityTokens ?: BraceTokenDefaults.comfortable

    val componentMetrics: BraceComponentMetrics
        @Composable get() = LocalBraceTheme.current?.componentMetrics
            ?: BraceTokenDefaults.componentMetrics

    val colorMode: BraceColorMode
        @Composable get() = LocalBraceTheme.current?.mode ?: BraceColorMode.Light

    val contrast: BraceContrast
        @Composable get() = LocalBraceTheme.current?.contrast ?: BraceContrast.Standard

    val density: BraceDensity
        @Composable get() = LocalBraceTheme.current?.density ?: BraceDensity.Comfortable
}

private fun defaultColors(mode: BraceColorMode, contrast: BraceContrast): BraceColorScheme =
    when (mode) {
        BraceColorMode.Light -> if (contrast == BraceContrast.High) {
            BraceTokenDefaults.highContrastLight
        } else BraceTokenDefaults.light
        BraceColorMode.Dark -> if (contrast == BraceContrast.High) {
            BraceTokenDefaults.highContrastDark
        } else BraceTokenDefaults.dark
    }

internal fun BraceSemanticColors.withBrand(
    brand: BraceBrandColors?,
    mode: BraceColorMode,
): BraceSemanticColors {
    if (brand == null) return this
    val target = if (mode == BraceColorMode.Light) Color.Black else Color.White
    return copy(
        primary = brand.primary,
        onPrimary = brand.onPrimary,
        primaryHover = brand.primaryHover ?: lerp(brand.primary, target, 0.12f),
        primaryPressed = brand.primaryPressed ?: lerp(brand.primary, target, 0.22f),
        primarySubtle = brand.primarySubtle ?: lerp(brand.primary, surface, 0.9f),
        onPrimarySubtle = brand.onPrimarySubtle ?: brand.primary,
        focusRing = brand.focusRing ?: brand.primary,
        selection = brand.selection ?: lerp(brand.primary, surface, 0.78f),
        onSelection = brand.onSelection ?: brand.primary,
    )
}

internal fun BraceMotionTokens.withoutAnimation() = copy(
    fast = 0,
    instant = 0,
    normal = 0,
    slow = 0,
)

@Composable
private fun systemReducedMotion(): Boolean {
    val context = LocalContext.current
    var reduced by remember(context) { mutableStateOf(readSystemReducedMotion(context)) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = readSystemReducedMotion(context)
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return reduced
}

private fun readSystemReducedMotion(context: Context): Boolean = try {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
} catch (_: SecurityException) {
    false
}
