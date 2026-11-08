package com.anisync.android.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The two wide roles must render with the axes the frames use, and with the optical size Figma
 * picks for the size, or every wide line ends up a few percent off its frame.
 */
class WideRoleTest {

    private val typography = defaultExpressiveTypography()

    @Test
    fun `heroWide is black at width 125 and fully round`() {
        val axes = axes(typography.heroWide.style(80.sp, 84.sp, (-2).sp))
        assertEquals(900f, axes.getValue("wght"))
        assertEquals(125f, axes.getValue("wdth"))
        assertEquals(100f, axes.getValue("ROND"))
        assertEquals(FontWeight.W900, typography.heroWide.style(80.sp, 84.sp).fontWeight)
    }

    @Test
    fun `titleWide is extra bold at width 112 and fully round`() {
        val axes = axes(typography.titleWide.style(26.sp, 32.sp))
        assertEquals(800f, axes.getValue("wght"))
        assertEquals(112f, axes.getValue("wdth"))
        assertEquals(100f, axes.getValue("ROND"))
        assertEquals(FontWeight.W800, typography.titleWide.style(26.sp, 32.sp).fontWeight)
    }

    @Test
    fun `optical size follows the font size and stops at the axis maximum`() {
        assertEquals(26f, axes(typography.titleWide.style(26.sp, 32.sp)).getValue("opsz"))
        assertEquals(80f, axes(typography.heroWide.style(80.sp, 84.sp)).getValue("opsz"))
        assertEquals(144f, axes(typography.heroWide.style(200.sp, 196.sp)).getValue("opsz"))
    }

    @Test
    fun `size, line height and tracking come from the use`() {
        val style = typography.heroWide.style(112.sp, 116.sp, (-5).sp)
        assertEquals(112.sp, style.fontSize)
        assertEquals(116.sp, style.lineHeight)
        assertEquals((-5).sp, style.letterSpacing)
    }

    @Test
    fun `one family per size is built once and reused`() {
        val role = typography.heroWide
        assertSame(role.style(80.sp, 84.sp).fontFamily, role.style(80.sp, 80.sp, (-3).sp).fontFamily)
        assertNotSame(role.style(80.sp, 84.sp).fontFamily, role.style(96.sp, 96.sp).fontFamily)
    }

    @Test
    fun `a font playground weight reaches the wide roles`() {
        val overridden = defaultExpressiveTypography(
            TypographyOverrides(display = FontAxisOverrides(weight = 500f))
        )
        val style = overridden.heroWide.style(80.sp, 84.sp)
        assertEquals(FontWeight.W500, style.fontWeight)
        assertEquals(500f, axes(style).getValue("wght"))
    }

    @Test
    fun `where the width axis is not drawn the wide roles keep the regular width`() {
        val role = WideRole(TypographyAxisConfig.heroWide, FontAxisOverrides(width = 150f), drawsWidth = false)
        val axes = axes(role.style(80.sp, 84.sp))
        assertFalse("wdth" in axes)
        assertEquals(900f, axes.getValue("wght"))
        assertEquals(100f, axes.getValue("ROND"))
        assertEquals(80f, axes.getValue("opsz"))
    }

    private fun axes(style: TextStyle): Map<String, Float> {
        val font = (style.fontFamily as FontListFontFamily).fonts.single() as ResourceFont
        return font.variationSettings.settings.associate { it.axisName to it.toVariationValue(null) }
    }
}
