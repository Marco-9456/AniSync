package com.anisync.android.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Rich-text images that turn out to be SVG are rendered here rather than through Coil. Coil
 * rasterizes SVG via AndroidSVG, which cannot render `<foreignObject>`, CSS/SMIL animation, or
 * nested-SVG `<image>` data URIs — so badge logos (shields.io `logo=…`) drop and live widgets
 * (spotify-github-profile "now playing") collapse to their bare background. A WebView is a full
 * browser engine and renders all of these correctly.
 *
 * Performance: a live [WebView] per SVG is expensive — instantiating several on screen entry
 * stalls the main thread, and any that animate keep repainting forever. So every SVG is rendered
 * once into a WebView (animations stripped), captured to a [Bitmap], and shown as a plain [Image];
 * the WebView is thrown away. Only SVGs classified [SvgAnimation.Light] additionally get a live
 * WebView overlay that plays their animation while the item is settled. [SvgAnimation.Heavy] ones —
 * animation driving a `<filter>`, e.g. lastfmstats' animated feTurbulence "scribble" — force
 * Chromium to re-rasterize the filtered region every frame forever; three of those pinned the whole
 * app, so they stay frozen snapshots (visually the animation's first frame). If a capture ever
 * fails it falls back to the paused WebView, so rendering is never worse than a bare WebView.
 *
 * The WebView is hardened for untrusted, user-authored content: JavaScript disabled (CSS
 * animations still run without it), file/content access off, and a strict CSP that permits only
 * inline styles plus data:/https: images and fonts — no scripts, objects, or frames.
 */

/** How an SVG animates — decides whether it ever gets a live WebView. */
internal enum class SvgAnimation {
    /** No animation. Snapshot only. */
    None,

    /**
     * Cheap animation: CSS `@keyframes`, SMIL attribute tweens (marquees, fades), or an embedded
     * GIF. Plays in a live WebView overlay while the item is settled.
     */
    Light,

    /**
     * SMIL animation inside a `<filter>` (e.g. an animated feTurbulence seed feeding
     * feDisplacementMap). Chromium re-rasterizes the whole filtered region every frame for the
     * animation's entire indefinite duration — one such SVG saturates a core, several lag the
     * whole app. Rendered as a frozen snapshot; never gets a live WebView. (See the note on
     * [RichSvgResolver.stripSmil] for why a partial strip that keeps marquees live doesn't work.)
     */
    Heavy
}

internal sealed interface RichImgKind {
    data object Loading : RichImgKind

    /**
     * [html] is the live document (original animations) for the [SvgAnimation.Light] overlay;
     * [staticHtml] has all animation stripped and is what the snapshot WebView loads, so the
     * capture never races a repaint loop. For non-Light SVGs they are the same instance.
     * [naturalWidthDp] is the SVG's own px width (≈ dp), used to size+center small badges.
     */
    data class Svg(
        val html: String,
        val staticHtml: String,
        val animation: SvgAnimation,
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
                    val animation = classifyAnimation(text)
                    val staticHtml = buildSvgHtml(text, freeze = true)
                    RichImgKind.Svg(
                        html = if (animation == SvgAnimation.Light) {
                            buildSvgHtml(text, freeze = false)
                        } else {
                            staticHtml
                        },
                        staticHtml = staticHtml,
                        animation = animation,
                        aspectRatio = svgAspectRatio(text),
                        naturalWidthDp = svgWidthDp(text)
                    )
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

    // Longer names first so \b can't reject at the prefix; <set\b avoids matching e.g. <settings.
    private val SMIL_ELEMENT =
        Regex("""<(animateMotion|animateTransform|animate|set)\b""", RegexOption.IGNORE_CASE)
    private val SMIL_SELF_CLOSED =
        Regex("""(?is)<(?:animateMotion|animateTransform|animate|set)\b[^>]*/>""")
    private val SMIL_PAIRED =
        Regex("""(?is)<(animateMotion|animateTransform|animate|set)\b[^>]*>.*?</\1\s*>""")
    private val FILTER_BLOCK = Regex("""(?is)<filter\b.*?</filter\s*>""")
    private val GIF_REFERENCE = Regex("""(?i)image/gif|\.gif["'?]""")

    /** See [SvgAnimation]. Heavy wins: one animated filter freezes the whole document. */
    internal fun classifyAnimation(svg: String): SvgAnimation = when {
        FILTER_BLOCK.findAll(svg).any { SMIL_ELEMENT.containsMatchIn(it.value) } -> SvgAnimation.Heavy
        SMIL_ELEMENT.containsMatchIn(svg) ||
            svg.contains("@keyframes", ignoreCase = true) ||
            GIF_REFERENCE.containsMatchIn(svg) -> SvgAnimation.Light
        else -> SvgAnimation.None
    }

    /**
     * Removes SMIL elements, leaving each animated attribute at its base value (the animation's
     * first frame). CSS animations are disabled separately via the frozen document's stylesheet.
     *
     * NOTE: a finer "defang" (strip only in-filter SMIL, keep marquees live) was tried and
     * reverted: animating an element that *has* a `filter=` applied re-rasterizes the filter every
     * frame even when the filter's own parameters are static — lastfm's widgets wiggle ~10
     * scribble-filtered `<text>` layers, which burned ~170% CPU. And the filtered shadow layer is
     * duplicated by an unfiltered top layer, so stripping animation from only the filtered copy
     * desyncs the two. Heavy therefore always means a full freeze.
     */
    internal fun stripSmil(svg: String): String =
        svg.replace(SMIL_SELF_CLOSED, "").replace(SMIL_PAIRED, "")

    /**
     * [freeze] builds the static document used for snapshots: SMIL stripped and CSS animation
     * disabled, so the page paints once instead of looping while the capture is pending — an
     * animated SVG used to keep software-rasterizing on the main thread until its snapshot landed.
     */
    private fun buildSvgHtml(svg: String, freeze: Boolean): String {
        // JS is disabled in the WebView, but strip <script> anyway as defense-in-depth.
        val sanitized = svg.replace(Regex("(?is)<script.*?</script>"), "")
            .let { if (freeze) stripSmil(it) else it }
        val freezeCss = if (freeze) "\n  *{animation:none!important;transition:none!important}" else ""
        return """
            <!DOCTYPE html>
            <html><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta http-equiv="Content-Security-Policy"
                  content="default-src 'none'; img-src data: https:; style-src 'unsafe-inline' https:; font-src data: https:;">
            <style>
              html,body{margin:0;padding:0;background:transparent;overflow:hidden}
              svg{display:block;width:100%;height:auto}$freezeCss
            </style>
            </head><body>$sanitized</body></html>
        """.trimIndent()
    }

    const val DEFAULT_ASPECT_RATIO = 2f
}

/**
 * Primes the WebView/Chromium provider once per process so the first on-screen WebView (e.g. when a
 * profile bio full of badges opens) doesn't pay the cold-start initialization on the UI thread at
 * that moment. Safe to call from [android.app.Application.onCreate]; it is a no-op after the first
 * call and swallows any device-specific WebView init failure.
 */
object WebViewWarmer {
    @Volatile
    private var warmed = false

    fun warmUp(context: Context) {
        if (warmed) return
        warmed = true
        runCatching {
            Handler(Looper.getMainLooper()).post {
                runCatching {
                    val wv = WebView(context.applicationContext)
                    wv.loadDataWithBaseURL(null, "<html></html>", "text/html", "utf-8", null)
                    // Tear it down on the next loop; the provider stays initialized for the process.
                    Handler(Looper.getMainLooper()).post { runCatching { wv.destroy() } }
                }
            }
        }
    }
}

/** Process-wide cache of rasterized static SVGs, keyed by document html. */
private object SvgSnapshotCache {
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(key: String): Bitmap? = synchronized(cache) { cache.get(key) }
    fun put(key: String, bitmap: Bitmap) = synchronized(cache) { cache.put(key, bitmap) }
}

/** How long an image's position must hold still before it's considered settled enough to animate. */
private const val SVG_SETTLE_DELAY_MS = 320L

/**
 * Renders a resolved SVG with smooth scrolling *and* working animation:
 *  - A cached [Bitmap] snapshot is the base layer. It's cheap to composite, so flinging past the
 *    image — or scrolling back to it — never touches a WebView.
 *  - Only an [SvgAnimation.Light] SVG gets a live [WebView], overlaid while the item is *settled*
 *    (its on-screen position has held still for a beat). CSS/SMIL animations therefore play when
 *    you stop on a widget, but the WebView can't invalidate every frame *while you scroll* — which
 *    is what pinned the main thread and dropped frames on Hameru's complex widget.
 *  - Once the live WebView has drawn its first frame the snapshot *leaves the composition*: SVGs
 *    are often background-transparent, so anything the animation moves would otherwise show
 *    doubled over the frozen copy underneath (spotify-github-profile's marquee did). A paused
 *    WebView keeps compositing its last frame, so scrolling stays cheap without the snapshot.
 *  - [SvgAnimation.None] and [SvgAnimation.Heavy] never mount a live WebView at all: the snapshot
 *    is the final render, and none of the settle tracking below runs for them.
 *
 * "Settled" is derived per-image from its own position, so no scroll state has to be threaded down
 * from each screen. [linkUrl] turns the whole image into a link via a transparent overlay.
 */
@Composable
internal fun RichSvgView(
    svg: RichImgKind.Svg,
    linkUrl: String?,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val live = svg.animation == SvgAnimation.Light

    var settled by remember(svg.html) { mutableStateOf(false) }
    // Latches true on the first settle. The live WebView is then kept mounted (just paused/resumed)
    // rather than created and destroyed each time scrolling stops — re-creating + re-rendering a
    // heavy animated SVG on every settle is itself a big main-thread stall.
    var everSettled by remember(svg.html) { mutableStateOf(false) }
    // True once the live WebView has actually drawn — only then is the snapshot dropped, so there
    // is no transparent flash while the live document loads.
    var liveReady by remember(svg.html) { mutableStateOf(false) }
    var lastPosition by remember(svg.html) { mutableStateOf(Offset.Unspecified) }
    var positionTick by remember(svg.html) { mutableIntStateOf(0) }

    // Every scroll frame moves the image and bumps positionTick, which restarts this effect:
    // 'settled' drops to false instantly on movement and only returns to true once the position has
    // been stable for SVG_SETTLE_DELAY_MS.
    if (live) {
        LaunchedEffect(positionTick) {
            settled = false
            delay(SVG_SETTLE_DELAY_MS)
            settled = true
            everSettled = true
        }
    }

    Box(
        modifier = if (live) {
            modifier.onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                if (position != lastPosition) {
                    lastPosition = position
                    positionTick++
                }
            }
        } else {
            modifier
        }
    ) {
        // Base layer: the static snapshot — what shows during first load, while scrolling before
        // the live layer exists, and forever for non-Light SVGs. Removed once the live WebView has
        // drawn, or its frozen content would show through the live document's transparency.
        if (!liveReady) {
            RichSvgSnapshot(svg.staticHtml, Modifier.fillMaxWidth())
        }

        // Animated layer: a live WebView created once the image first settles and then kept mounted.
        // It plays only while settled; the instant a scroll starts it is paused, so it can't redraw
        // mid-scroll (what janked Hameru's complex widget). A paused WebView keeps showing its last
        // frame, so pausing/resuming isn't visible beyond the animation stopping and starting.
        if (live && everSettled) {
            RichSvgLiveWebView(
                html = svg.html,
                playing = settled,
                onFirstFrame = { liveReady = true },
                modifier = Modifier.matchParentSize()
            )
        }

        // Display-only content; a transparent overlay handles the link without letting a WebView
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

/**
 * Renders [html] once in a WebView, captures it to a bitmap, then replaces itself with a static
 * [Image]. Falls back to leaving the live WebView in place if the capture can't be taken (e.g. the
 * view never reports a size), so the result is never worse than a plain WebView.
 */
@Composable
private fun RichSvgSnapshot(html: String, modifier: Modifier = Modifier) {
    var snapshot by remember(html) { mutableStateOf(SvgSnapshotCache.get(html)) }
    // Set once a capture attempt has permanently failed → stop trying. The WebView is paused so it
    // can't keep animating (which would jank), but stays visible as a last-resort fallback.
    var captureFailed by remember(html) { mutableStateOf(false) }

    val current = snapshot
    if (current != null) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = modifier
        )
        return
    }

    AndroidView(
        factory = { context ->
            createHardenedWebView(context).apply {
                // Software layer so View.draw() into a Canvas captures real pixels; a hardware
                // layer would draw blank when snapshotting.
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                tag = html
                webViewClient = object : WebViewClient() {
                    private var attempts = 0
                    override fun onPageFinished(view: WebView, url: String?) {
                        if (captureFailed) return
                        // Let layout + paint settle (data-URI sub-images render synchronously),
                        // then snapshot. Retry a few times if the view has no size yet.
                        fun attempt() {
                            if (snapshot != null || captureFailed) return
                            val w = view.width
                            val h = view.height
                            if (w > 0 && h > 0) {
                                val bmp = runCatching {
                                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { b ->
                                        view.draw(Canvas(b))
                                    }
                                }.getOrNull()
                                if (bmp != null) {
                                    SvgSnapshotCache.put(html, bmp)
                                    snapshot = bmp
                                } else {
                                    view.onPause()
                                    captureFailed = true
                                }
                            } else if (attempts++ < 6) {
                                view.postDelayed({ attempt() }, 100)
                            } else {
                                view.onPause()
                                captureFailed = true
                            }
                        }
                        view.postDelayed({ attempt() }, 150)
                    }
                }
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        onRelease = { it.destroy() },
        modifier = modifier
    )
}

/**
 * A live WebView that plays the SVG's animation. Stays mounted once shown; [playing] resumes it when
 * the item is settled and pauses it the moment a scroll starts, so it can't redraw mid-scroll. A
 * paused WebView still shows its last frame (cheap to composite), so scrolling stays smooth. Uses a
 * hardware layer so the animation composites on the render thread.
 *
 * [onFirstFrame] fires once the loaded document has actually been drawn (visual-state callback
 * registered from onPageFinished, so it re-arms on every load) — the caller's cue to drop the
 * snapshot underneath.
 */
@Composable
private fun RichSvgLiveWebView(
    html: String,
    playing: Boolean,
    onFirstFrame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnFirstFrame by rememberUpdatedState(onFirstFrame)
    AndroidView(
        factory = { context ->
            createHardenedWebView(context).apply {
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                tag = html
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        view.postVisualStateCallback(
                            0,
                            object : WebView.VisualStateCallback() {
                                override fun onComplete(requestId: Long) {
                                    currentOnFirstFrame()
                                }
                            }
                        )
                    }
                }
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
            if (playing) webView.onResume() else webView.onPause()
        },
        onRelease = {
            it.onPause()
            it.destroy()
        },
        modifier = modifier
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun createHardenedWebView(context: Context): WebView = WebView(context).apply {
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
        // Lay out at the view's own width (1 CSS px ≈ 1 dp) so `svg{width:100%}` fills the view
        // exactly — no wide viewport that leaves the badge small and left-aligned.
        loadWithOverviewMode = false
        useWideViewPort = false
    }
}
