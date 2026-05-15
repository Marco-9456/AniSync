package com.anisync.android.ui.theme

/**
 * Single source of truth for variable-font axis values, grouped by Material 3 role.
 *
 * This is the ONE place to make global axis adjustments. Want every headline wider, or the
 * whole app rounder? Edit one line here — every [androidx.compose.ui.text.font.FontFamily]
 * in [AppTypography] (and the editorial families in [ExpressiveTypography]) is derived from
 * these presets, so the change propagates app-wide without touching a single composable.
 *
 * `opticalSize` (opsz) is set per role group to roughly track that role's rendered size band —
 * that is precisely what the optical-size axis is for. Smaller opsz opens up letterforms for
 * legibility at text sizes; larger opsz tightens them for display sizes. See
 * https://fonts.google.com/knowledge/glossary/axis_in_variable_fonts
 *
 * The live "font playground" developer screen overrides these presets at runtime via
 * [FontAxisOverrides] — see [buildAppTypography].
 */
object TypographyAxisConfig {

    /** The active variable-font axis registry. */
    val registry: VariableFontAxes = GoogleSansFlexAxes

    // `roundness = 100f` is the app-wide default — AniSync ships fully rounded; the playground
    // can still override it per category.

    /** Display roles (57–36sp): large, tight letterforms. */
    val display = FontAxes(weight = 400f, opticalSize = 144f, width = 100f, roundness = 100f)

    /** Headline roles (32–24sp). Weight lifted to 500 per MD3 Expressive guidance. */
    val headline = FontAxes(weight = 500f, opticalSize = 48f, width = 100f, roundness = 100f)

    /** Title roles (22–14sp). */
    val title = FontAxes(weight = 500f, opticalSize = 18f, width = 100f, roundness = 100f)

    /** Body roles (16–12sp): opsz tracks the text size for maximum legibility. */
    val body = FontAxes(weight = 400f, opticalSize = 14f, width = 100f, roundness = 100f)

    /** Label roles (14–11sp): smallest opsz, most open letterforms. */
    val label = FontAxes(weight = 500f, opticalSize = 11f, width = 100f, roundness = 100f)

    /** Tabular numerics for the editorial / statistics surfaces (see [ExpressiveTypography]). */
    val numeric = FontAxes(weight = 700f, opticalSize = 48f, width = 100f, roundness = 100f)
}
