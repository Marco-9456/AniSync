package com.anisync.android.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.anisync.android.R
import java.util.Locale

/**
 * Hands text off to an external translation app installed on the device (TranslateYou, DeepL,
 * Google Translate). There is no in-app translation engine — we simply fire an [Intent] at whichever
 * translator is present.
 *
 * Each candidate is tried in priority order via an explicit [ComponentName]; the first one that
 * launches wins and the rest are skipped. Apps that aren't installed throw on [Context.startActivity]
 * and are silently skipped (explicit-component activity starts are exempt from Android 11+ package
 * visibility, so no `<queries>` entry is required). If nothing handles it, a toast is shown.
 */
object TranslateUtils {

    private const val TAG = "TranslateUtils"

    fun Context.openTranslator(text: String) {
        if (text.isBlank()) return
        if (!openInTranslateYou(text) &&
            !openInDeepLMini(text) &&
            !openInDeepL(text) &&
            !openInGoogleTranslateMini(text) &&
            !openInGoogleTranslate(text)
        ) {
            Toast.makeText(this, R.string.no_translation_app_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun Context.openInTranslateYou(text: String): Boolean = launchProcessText(
        text = text,
        component = ComponentName(
            "com.bnyro.translate",
            "com.bnyro.translate.ui.ShareActivity"
        )
    )

    private fun Context.openInDeepLMini(text: String): Boolean = launchProcessText(
        text = text,
        component = ComponentName(
            "com.deepl.mobiletranslator",
            "com.deepl.mobiletranslator.MiniTranslatorActivity"
        )
    )

    private fun Context.openInGoogleTranslateMini(text: String): Boolean = launchProcessText(
        text = text,
        component = ComponentName(
            "com.google.android.apps.translate",
            "com.google.android.apps.translate.copydrop.gm3.TapToTranslateActivity"
        )
    )

    /** Shared launcher for translators that accept text via [Intent.ACTION_PROCESS_TEXT]. */
    private fun Context.launchProcessText(text: String, component: ComponentName): Boolean {
        return try {
            Intent(Intent.ACTION_PROCESS_TEXT).apply {
                this.component = component
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                startActivity(this)
            }
            true
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
            false
        }
    }

    /**
     * DeepL's main activity takes no text extra, so the text is placed on the clipboard for the user
     * to paste. Mirrors AniHyou's behavior.
     */
    private fun Context.openInDeepL(text: String): Boolean {
        return try {
            copyToClipboard(text)
            Intent(Intent.ACTION_VIEW).apply {
                component = ComponentName(
                    "com.deepl.mobiletranslator",
                    "com.deepl.mobiletranslator.MainActivity"
                )
                startActivity(this)
            }
            true
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
            false
        }
    }

    private fun Context.openInGoogleTranslate(text: String): Boolean {
        return try {
            Intent(Intent.ACTION_SEND).apply {
                component = ComponentName(
                    "com.google.android.apps.translate",
                    "com.google.android.apps.translate.TranslateActivity"
                )
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra("key_text_input", text)
                putExtra("key_text_output", "")
                putExtra("key_language_from", "en")
                putExtra("key_language_to", Locale.getDefault().language)
                putExtra("key_suggest_translation", "")
                putExtra("key_from_floating_window", false)
                startActivity(this)
            }
            true
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
            false
        }
    }

    private fun Context.copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("translate", text))
    }
}
