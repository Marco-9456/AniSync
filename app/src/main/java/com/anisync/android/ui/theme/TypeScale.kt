package com.anisync.android.ui.theme

/**
 * One row of the Material 3 type scale — size, line height, and tracking for a single role.
 * All values are in `sp` and are converted at assembly time in [AppTypography].
 */
internal data class TypeScaleToken(
    val sizeSp: Float,
    val lineHeightSp: Float,
    val trackingSp: Float,
)

/**
 * The Material 3 type-scale tokens for all 15 roles.
 *
 * Values match m3.material.io/styles/typography/type-scale-tokens and are the single source
 * of size / line-height / tracking numbers for the app — both [AppTypography] (main UI) and
 * `WidgetTypography` (Glance widgets) derive their sizes from here.
 */
internal object M3TypeScale {
    val displayLarge = TypeScaleToken(57f, 64f, -0.25f)
    val displayMedium = TypeScaleToken(45f, 52f, 0f)
    val displaySmall = TypeScaleToken(36f, 44f, 0f)
    val headlineLarge = TypeScaleToken(32f, 40f, 0f)
    val headlineMedium = TypeScaleToken(28f, 36f, 0f)
    val headlineSmall = TypeScaleToken(24f, 32f, 0f)
    val titleLarge = TypeScaleToken(22f, 28f, 0f)
    val titleMedium = TypeScaleToken(16f, 24f, 0.15f)
    val titleSmall = TypeScaleToken(14f, 20f, 0.1f)
    val bodyLarge = TypeScaleToken(16f, 24f, 0.5f)
    val bodyMedium = TypeScaleToken(14f, 20f, 0.25f)
    val bodySmall = TypeScaleToken(12f, 16f, 0.4f)
    val labelLarge = TypeScaleToken(14f, 20f, 0.1f)
    val labelMedium = TypeScaleToken(12f, 16f, 0.5f)
    val labelSmall = TypeScaleToken(11f, 16f, 0.5f)
}
