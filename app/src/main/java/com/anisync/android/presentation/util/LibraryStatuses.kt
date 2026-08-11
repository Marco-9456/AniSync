package com.anisync.android.presentation.util

import androidx.compose.runtime.compositionLocalOf
import com.anisync.android.domain.LibraryStatus

/**
 * List membership for the active account, keyed by media id, published once for the whole app.
 *
 * The indicators appear on every browsing surface (discover, search, related, recommendations,
 * character and staff credits), all of which render the same card. Threading the map through each
 * of those screens' state would repeat the same collect in a dozen ViewModels for one badge.
 *
 * Not a `staticCompositionLocalOf`: the value changes on every library sync, and only the cards
 * that read it should recompose.
 */
val LocalLibraryStatuses = compositionLocalOf { emptyMap<Int, LibraryStatus>() }
