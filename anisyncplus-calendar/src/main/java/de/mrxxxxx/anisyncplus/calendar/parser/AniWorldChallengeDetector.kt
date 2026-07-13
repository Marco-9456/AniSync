package de.mrxxxxx.anisyncplus.calendar.parser

import org.jsoup.nodes.Document

/**
 * Classifies only strong challenge-page structures. Benign words in scripts, CDN URLs, or anime
 * titles are deliberately ignored; the real calendar DOM is authoritative when it is present.
 */
internal object AniWorldChallengeDetector {
    fun reason(document: Document): String? {
        val title = document.title().trim().lowercase()
        val bodyText = document.body()?.text().orEmpty().lowercase()
        return when {
            title.contains("just a moment") -> "challenge_title_just_a_moment"
            title.contains("attention required") -> "challenge_title_attention_required"
            document.selectFirst("[id^=cf-chl-], #challenge-form") != null ->
                "cloudflare_challenge_element"
            document.selectFirst(".cf-turnstile, .g-recaptcha, .h-captcha") != null ->
                "captcha_challenge_element"
            document.selectFirst("script[src*=\"/cdn-cgi/challenge-platform/\"]") != null ->
                "cloudflare_challenge_script"
            document.selectFirst("script[src*=\"check.ddos-guard.net\"]") != null ->
                "ddos_guard_challenge_script"
            "cloudflare ray id" in bodyText -> "cloudflare_ray_id"
            "verify you are human" in bodyText || "verify that you are human" in bodyText ->
                "human_verification_text"
            else -> null
        }
    }
}
