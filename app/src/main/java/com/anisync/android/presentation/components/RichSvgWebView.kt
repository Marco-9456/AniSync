package com.anisync.android.presentation.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Rich-text images that turn out to be SVG are rendered here in a WebView rather than through
 * Coil. Coil rasterizes SVG via AndroidSVG, which cannot render `<foreignObject>`, CSS/SMIL
 * animation, or nested-SVG `<image>` data URIs — so badge logos (shields.io `logo=…`) drop and
 * live widgets (spotify-github-profile "now playing") collapse to their bare background. A
 * WebView is a full browser engine and renders all of these correctly.
 *
 * The WebView is hardened for untrusted, user-authored content: JavaScript disabled (CSS
 * animations still run without it), file/content access off, and a strict CSP that permits only
 * inline styles plus data:/https: images and fonts — no scripts, objects, or frames.
 */

internal sealed interface RichImgKind {
    data object Loading : RichImgKind
    /** [naturalWidthDp] is the SVG's own px width (≈ dp), used to size+center small badges. */
    data class Svg(
        val html: String,
        val aspectRatio: Float,
        val naturalWidthDp: Float?
    ) : RichImgKind
    data object Raster : RichImgKind
}

internal object RichSvgResolver {

    private const val USER_AGENT = "Mozilla/5.0 (Android; AniSync) AppleWebKit/537.36"
    private const val TIMEOUT_MS = 8_000

    private val RASTER_EXTENSIONS = listOf(
        ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp", ".avif", ".heic", ".heif", ".ico"
    )

    private val cache = ConcurrentHashMap<String, RichImgKind>()

    /**
     * Synchronous best-guess used as the initial render state so images with a known raster
     * extension never block on the network or flash a skeleton. Anything else starts [Loading]
     * and is resolved by [resolve].
     */
    fun quickKind(url: String): RichImgKind {
        cache[url]?.let { return it }
        val path = url.substringBefore('?').lowercase()
        return if (RASTER_EXTENSIONS.any { path.endsWith(it) }) RichImgKind.Raster
        else RichImgKind.Loading
    }

    suspend fun resolve(url: String): RichImgKind = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }

        val path = url.substringBefore('?').lowercase()
        if (RASTER_EXTENSIONS.any { path.endsWith(it) }) {
            return@withContext RichImgKind.Raster.also { cache[url] = it }
        }

        val result = runCatching {
            val contentType = headContentType(url)
            val byExtension = path.endsWith(".svg")
            // Probe the body only when the type is SVG, unknown (no/failed HEAD), or the path
            // says .svg. A confirmed non-SVG content type (e.g. image/jpeg from an extension-less
            // CDN like i.scdn.co) short-circuits to Raster without downloading the body.
            val mightBeSvg = byExtension ||
                contentType == null ||
                contentType.contains("svg", ignoreCase = true)

            if (!mightBeSvg) {
                RichImgKind.Raster
            } else {
                val text = fetchText(url)
                if (text != null && text.contains("<svg", ignoreCase = true)) {
                    RichImgKind.Svg(buildSvgHtml(text), svgAspectRatio(text), svgWidthDp(text))
                } else {
                    RichImgKind.Raster
                }
            }
        }.getOrDefault(RichImgKind.Raster)

        cache[url] = result
        result
    }

    private fun headContentType(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            connection.connect()
            connection.contentType
        } catch (e: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchText(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "image/svg+xml,*/*")
        }
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Aspect ratio from the SVG's viewBox (preferred) or width/height attributes. */
    private fun svgAspectRatio(svg: String): Float {
        Regex(
            """viewBox\s*=\s*["']\s*[-\d.]+[\s,]+[-\d.]+[\s,]+([\d.]+)[\s,]+([\d.]+)""",
            RegexOption.IGNORE_CASE
        ).find(svg)?.let {
            val w = it.groupValues[1].toFloatOrNull()
            val h = it.groupValues[2].toFloatOrNull()
            if (w != null && h != null && h > 0f) return w / h
        }
        val w = Regex("""\bwidth\s*=\s*["']([\d.]+)""").find(svg)?.groupValues?.get(1)?.toFloatOrNull()
        val h = Regex("""\bheight\s*=\s*["']([\d.]+)""").find(svg)?.groupValues?.get(1)?.toFloatOrNull()
        if (w != null && h != null && h > 0f) return w / h
        return DEFAULT_ASPECT_RATIO
    }

    /**
     * The SVG's intrinsic px width from the `<svg>` width attribute (≈ dp). Used to size and
     * center small badges instead of stretching them to full width. Percentage widths and
     * missing attributes return null (caller fills width). viewBox width is intentionally NOT
     * used here — services like shields.io scale the viewBox 10× while keeping a real px width.
     */
    private fun svgWidthDp(svg: String): Float? =
        Regex("""<svg\b[^>]*?\bwidth\s*=\s*["']([\d.]+)(?:px)?["']""", RegexOption.IGNORE_CASE)
            .find(svg)?.groupValues?.get(1)?.toFloatOrNull()?.takeIf { it > 0f }

    private fun buildSvgHtml(svg: String): String {
        // JS is disabled in the WebView, but strip <script> anyway as defense-in-depth.
        val sanitized = svg.replace(Regex("(?is)<script.*?</script>"), "")
        return """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta http-equiv="Content-Security-Policy"
                  content="default-src 'none'; img-src data: https:; style-src 'unsafe-inline' https:; font-src data: https:;">
            <style>
              html,body{margin:0;padding:0;background:transparent;overflow:hidden}
              svg{display:block;width:100%;height:auto}
            </style>
            </head><body>$sanitized</body></html>
        """.trimIndent()
    }

    const val DEFAULT_ASPECT_RATIO = 2f
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun RichSvgWebView(
    html: String,
    linkUrl: String?,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    isClickable = false
                    isLongClickable = false
                    settings.apply {
                        javaScriptEnabled = false
                        allowFileAccess = false
                        allowContentAccess = false
                        // Lay out at the view's own width (1 CSS px ≈ 1 dp) so `svg{width:100%}`
                        // fills the view exactly — no wide viewport that leaves the badge small
                        // and left-aligned inside an oversized WebView.
                        loadWithOverviewMode = false
                        useWideViewPort = false
                    }
                    // Default client keeps everything in-WebView; taps are handled by the overlay.
                    webViewClient = WebViewClient()
                    tag = html
                    loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }
            },
            // Reload only when the document actually changes (e.g. recycled in a list).
            update = { webView ->
                if (webView.tag != html) {
                    webView.tag = html
                    webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // The WebView is display-only; a transparent overlay turns the whole image into the
        // surrounding link (matching Coil-rendered linked images) without letting the WebView
        // intercept navigation.
        if (linkUrl != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { onLinkClick(linkUrl) }
            )
        }
    }
}
