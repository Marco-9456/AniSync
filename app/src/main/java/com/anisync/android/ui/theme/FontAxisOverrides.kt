package com.anisync.android.ui.theme

import androidx.compose.runtime.Immutable
import com.anisync.android.ui.theme.FontAxisOverrides.Companion.None
import kotlinx.serialization.Serializable

/**
 * Runtime variable-font axis overrides for a single Material 3 role category, driven by the
 * developer "font playground".
 *
 * Each axis is nullable: a `null` axis means "leave the per-role [TypographyAxisConfig] preset
 * alone". A non-null axis replaces that axis for every role in its category.
 *
 * When [isActive] is `false` ([None]), that category renders with its normal preset typography.
 */
@Immutable
@Serializable
data class FontAxisOverrides(
    val weight: Float? = null,
    val width: Float? = null,
    val opticalSize: Float? = null,
    val slant: Float? = null,
    val roundness: Float? = null,
) {
    /** True when at least one axis is being overridden. */
    val isActive: Boolean
        get() = weight != null || width != null || opticalSize != null ||
            slant != null || roundness != null

    companion object {
        /** The neutral state — no axis overridden; the category uses its per-role presets. */
        val None = FontAxisOverrides()
    }
}

/** The Material 3 role categories the font playground can target individually. */
enum class TypeCategory {
    DISPLAY,
    HEADLINE,
    TITLE,
    BODY,
    LABEL,
}

/**
 * Per-category variable-font axis overrides for the whole app. Persisted by `AppSettings` as
 * JSON and threaded through [AppTheme] so the developer font playground can tweak each M3 role
 * category (and, via [withAll], all of them at once) live.
 */
@Immutable
@Serializable
data class TypographyOverrides(
    val display: FontAxisOverrides = FontAxisOverrides.None,
    val headline: FontAxisOverrides = FontAxisOverrides.None,
    val title: FontAxisOverrides = FontAxisOverrides.None,
    val body: FontAxisOverrides = FontAxisOverrides.None,
    val label: FontAxisOverrides = FontAxisOverrides.None,
) {
    /** True when any category has at least one active axis override. */
    val isActive: Boolean
        get() = display.isActive || headline.isActive || title.isActive ||
            body.isActive || label.isActive

    fun forCategory(category: TypeCategory): FontAxisOverrides = when (category) {
        TypeCategory.DISPLAY -> display
        TypeCategory.HEADLINE -> headline
        TypeCategory.TITLE -> title
        TypeCategory.BODY -> body
        TypeCategory.LABEL -> label
    }

    fun withCategory(category: TypeCategory, overrides: FontAxisOverrides): TypographyOverrides =
        when (category) {
            TypeCategory.DISPLAY -> copy(display = overrides)
            TypeCategory.HEADLINE -> copy(headline = overrides)
            TypeCategory.TITLE -> copy(title = overrides)
            TypeCategory.BODY -> copy(body = overrides)
            TypeCategory.LABEL -> copy(label = overrides)
        }

    /** "All" shortcut — applies the same [overrides] to every category at once. */
    fun withAll(overrides: FontAxisOverrides): TypographyOverrides =
        TypographyOverrides(overrides, overrides, overrides, overrides, overrides)

    companion object {
        /** The neutral state — every category uses its per-role presets. */
        val None = TypographyOverrides()
    }
}

/**
 * Applies non-null [overrides] on top of a per-role [FontAxes] preset.
 *
 * Note the weight override flattens the W400/W500/W700 hierarchy for that category while
 * active — that is the intended playground behaviour; clearing it (Reset) restores it.
 */
fun FontAxes.merge(overrides: FontAxisOverrides): FontAxes = copy(
    weight = overrides.weight ?: weight,
    width = overrides.width ?: width,
    opticalSize = overrides.opticalSize ?: opticalSize,
    slant = overrides.slant ?: slant,
    roundness = overrides.roundness ?: roundness,
)
