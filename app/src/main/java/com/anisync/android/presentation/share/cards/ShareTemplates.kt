package com.anisync.android.presentation.share.cards

import androidx.compose.runtime.Composable

/** Draws [subject] with a template picked for it from [templatesFor]. */
@Composable
@Suppress("UNCHECKED_CAST")
fun ShareTemplate<*>.Render(subject: ShareSubject, spec: CardSpec) {
    (this as ShareTemplate<ShareSubject>).Card(subject, spec)
}

/** The templates a subject offers, in the order the studio swipes through them. */
fun templatesFor(subject: ShareSubject): List<ShareTemplate<*>> = when (subject) {
    is ShareSubject.Media -> listOf(MediaPosterTemplate, MediaBentoTemplate, MediaCardTemplate)
    is ShareSubject.Stats -> listOf(StatsBigNumberTemplate, StatsBentoTemplate, StatsGenresTemplate)
    is ShareSubject.Favourites -> listOf(FavouritesShelfTemplate, FavouritesCollageTemplate)
    is ShareSubject.Character -> listOf(CharacterTemplate)
    is ShareSubject.Staff -> listOf(StaffTemplate)
    is ShareSubject.Review -> listOf(ReviewQuoteTemplate)
    is ShareSubject.Recap -> recapTemplates(subject)
}

/** The template the studio opens on: a recap's own card, the first one otherwise. */
fun defaultTemplateIndex(subject: ShareSubject): Int =
    templatesFor(subject).indexOfFirst { it == RecapMonthTemplate || it == RecapYearTemplate }.coerceAtLeast(0)
