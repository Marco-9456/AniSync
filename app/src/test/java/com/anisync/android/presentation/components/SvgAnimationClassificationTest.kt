package com.anisync.android.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SvgAnimationClassificationTest {

    // Trimmed from a real lastfmstats.gothicblue.com widget: an animated feTurbulence seed feeding
    // feDisplacementMap ("scribble"), plus cheap text marquees outside the filter. The animated
    // filter must win — the whole document freezes.
    private val scribbleSvg = """
        <svg xmlns="http://www.w3.org/2000/svg" width="100%" height="195" viewBox="0 0 470 195">
        <defs>
          <filter id="scribble" x="-20%" y="-20%" width="140%" height="140%">
            <feTurbulence type="fractalNoise" baseFrequency="0.05" numOctaves="5" seed="300" result="noise">
              <animate attributeName="seed" from="1" to="1024" dur="128s" repeatCount="indefinite" />
            </feTurbulence>
            <feDisplacementMap in="SourceGraphic" in2="noise" scale="3" xChannelSelector="R" yChannelSelector="G"/>
          </filter>
        </defs>
        <text x="38" y="20">Track title
          <animate attributeName="x" values="38; 17; 38; 58; 38" dur="10s" repeatCount="indefinite" />
        </text>
        </svg>
    """.trimIndent()

    @Test
    fun `animated filter classifies as Heavy`() {
        assertEquals(SvgAnimation.Heavy, RichSvgResolver.classifyAnimation(scribbleSvg))
    }

    @Test
    fun `smil outside a static filter classifies as Light`() {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="400" height="100">
            <defs>
              <filter id="grain"><feTurbulence type="fractalNoise" baseFrequency="0.8"/></filter>
            </defs>
            <rect width="400" height="100" filter="url(#grain)"/>
            <text x="10" y="50">Now playing
              <animate attributeName="x" values="10;-200" dur="8s" repeatCount="indefinite"/>
            </text>
            </svg>
        """.trimIndent()
        assertEquals(SvgAnimation.Light, RichSvgResolver.classifyAnimation(svg))
    }

    @Test
    fun `css keyframes classify as Light`() {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="300" height="80">
            <style>@keyframes slide{from{transform:translateX(0)}to{transform:translateX(-100px)}}
            .marquee{animation:slide 6s linear infinite}</style>
            <text class="marquee" x="0" y="40">Scrolling</text>
            </svg>
        """.trimIndent()
        assertEquals(SvgAnimation.Light, RichSvgResolver.classifyAnimation(svg))
    }

    @Test
    fun `embedded gif classifies as Light`() {
        val dataUri = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
            <image href="data:image/gif;base64,R0lGODlh" width="100" height="100"/>
            </svg>
        """.trimIndent()
        val httpsRef = """
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
            <image href="https://example.com/loader.gif" width="100" height="100"/>
            </svg>
        """.trimIndent()
        assertEquals(SvgAnimation.Light, RichSvgResolver.classifyAnimation(dataUri))
        assertEquals(SvgAnimation.Light, RichSvgResolver.classifyAnimation(httpsRef))
    }

    @Test
    fun `static badge classifies as None`() {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" width="124" height="20">
            <linearGradient id="b" x2="0" y2="100%"><stop offset="0" stop-opacity=".1"/></linearGradient>
            <rect width="124" height="20" rx="3" fill="#555"/>
            <text x="10" y="14">build passing</text>
            </svg>
        """.trimIndent()
        assertEquals(SvgAnimation.None, RichSvgResolver.classifyAnimation(svg))
    }

    @Test
    fun `setting attributes do not false-positive as smil set element`() {
        val svg = """<svg width="10" height="10"><settings x="1"/><rect width="10" height="10"/></svg>"""
        assertEquals(SvgAnimation.None, RichSvgResolver.classifyAnimation(svg))
    }

    @Test
    fun `stripSmil removes self-closed and paired animation elements`() {
        val stripped = RichSvgResolver.stripSmil(scribbleSvg)

        assertFalse(stripped.contains("<animate", ignoreCase = true))
        // Base attributes and surrounding structure survive, so the frozen frame renders.
        assertTrue(stripped.contains("seed=\"300\""))
        assertTrue(stripped.contains("feDisplacementMap"))
        assertTrue(stripped.contains("Track title"))
    }

    @Test
    fun `stripSmil removes paired animate with closing tag`() {
        val svg = """<svg><text>hi<animate attributeName="x" values="0;5">fallback</animate></text></svg>"""
        val stripped = RichSvgResolver.stripSmil(svg)
        assertFalse(stripped.contains("animate"))
        assertTrue(stripped.contains("<text>hi</text>"))
    }
}
