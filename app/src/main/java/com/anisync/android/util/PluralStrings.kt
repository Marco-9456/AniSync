package com.anisync.android.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.PluralsRes
import java.util.Locale

/**
 * [Resources.getQuantityString] that falls back to the default strings when a translation leaves
 * out the item [quantity] needs.
 *
 * A translated `<plurals>` replaces the default one whole, so Android never reaches `values/` for
 * the missing items and throws instead. [context] only builds the fallback.
 */
fun Resources.getQuantityStringOrDefault(
    context: Context,
    @PluralsRes id: Int,
    quantity: Int,
    vararg formatArgs: Any
): String = try {
    getQuantityString(id, quantity, *formatArgs)
} catch (_: Resources.NotFoundException) {
    context.defaultStrings().getQuantityString(id, quantity, *formatArgs)
}

// English reaches values/ because there is no values-en, and unlike Locale.ROOT it keeps the
// one/other split.
private fun Context.defaultStrings(): Resources {
    val config = Configuration(resources.configuration).apply { setLocale(Locale.ENGLISH) }
    return createConfigurationContext(config).resources
}
