package com.anisync.android.util

import com.anisync.android.data.TitleLanguage
import com.anisync.android.data.local.entity.LibraryEntryEntity
import com.anisync.android.data.local.entity.MediaDetailsEntity
import com.anisync.android.domain.LibraryEntry
import com.anisync.android.domain.MediaDetails
import com.anisync.android.domain.RelatedMedia

object TitleUtils {

    fun getTitle(
        language: TitleLanguage,
        romaji: String?,
        english: String?,
        native: String?,
        userPreferred: String
    ): String {
        return when (language) {
            TitleLanguage.ROMAJI -> romaji ?: english ?: native ?: userPreferred
            TitleLanguage.ENGLISH -> english ?: romaji ?: native ?: userPreferred
            TitleLanguage.NATIVE -> native ?: romaji ?: english ?: userPreferred
        }
    }
}

fun MediaDetails.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun LibraryEntry.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun RelatedMedia.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun LibraryEntryEntity.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}

fun MediaDetailsEntity.getTitle(language: TitleLanguage): String {
    return TitleUtils.getTitle(language, titleRomaji, titleEnglish, titleNative, titleUserPreferred)
}
