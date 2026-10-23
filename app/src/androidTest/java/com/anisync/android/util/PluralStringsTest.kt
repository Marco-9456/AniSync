package com.anisync.android.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.anisync.android.test.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * A translated `<plurals>` missing the item a count needs took the feed down in Spanish. The test
 * APK carries its own incomplete translations, so these hold whatever Weblate ships next.
 */
@RunWith(AndroidJUnit4::class)
class PluralStringsTest {

    private val testContext: Context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun platformLookupThrowsWhenTheTranslationMissesTheItem() {
        val spanish = testContext.inLocale("es")
        assertThrows(Resources.NotFoundException::class.java) {
            spanish.resources.getQuantityString(R.plurals.partial_translation, 3, 3)
        }
    }

    @Test
    fun missingItemFallsBackToTheDefaultStrings() {
        val spanish = testContext.inLocale("es")
        assertEquals("3 updates", spanish.quantityString(3))
    }

    @Test
    fun translationIsKeptWhenItHasTheItem() {
        val spanish = testContext.inLocale("es")
        assertEquals("1 actualización", spanish.quantityString(1))
    }

    @Test
    fun fallbackKeepsTheEnglishSingular() {
        val polish = testContext.inLocale("pl")
        assertEquals("1 update", polish.quantityString(1))
    }

    private fun Context.inLocale(tag: String): Context {
        val config = Configuration(resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
        }
        return createConfigurationContext(config)
    }

    private fun Context.quantityString(count: Int): String =
        resources.getQuantityStringOrDefault(this, R.plurals.partial_translation, count, count)
}
